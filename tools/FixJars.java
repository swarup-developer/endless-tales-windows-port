// Post-processes dex2jar output so the classes run on a modern desktop JVM with verification on:
//  - marks calls whose owner is an interface as interface calls (dex2jar loses that bit, which breaks
//    e.g. invokestatic Comparator.comparing -> IncompatibleClassChangeError)
//  - raises the class version to 52 (Java 8: default/static interface methods are legal)
//  - recomputes stack map frames (dex2jar does not write them)
//  - optionally replaces listed method bodies with a call to a desktop hook (see tools/patches.txt)
// Classes that ASM cannot rewrite are copied unchanged and listed.
//
// Run: java -cp <d2j-external.jar> tools/FixJars.java [--patches tools/patches.txt] <out-dir> <classpath-for-hierarchy (;-separated)> <in.jar>...

import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class FixJars {
    record Info(String superName, String[] interfaces, boolean isInterface) {}

    static final Map<String, Info> HIERARCHY = new ConcurrentHashMap<>();

    static void indexJar(Path jar) throws IOException {
        try (ZipFile z = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                try (InputStream in = z.getInputStream(e)) {
                    ClassReader r = new ClassReader(in);
                    HIERARCHY.putIfAbsent(r.getClassName(), new Info(r.getSuperName(), r.getInterfaces(),
                            (r.getAccess() & Opcodes.ACC_INTERFACE) != 0));
                } catch (Exception ignored) {}
            }
        }
    }

    static Info info(String name) {
        Info i = HIERARCHY.get(name);
        if (i != null) return i;
        // JDK classes: ask the running JVM
        try (InputStream in = ClassLoader.getSystemResourceAsStream(name + ".class")) {
            if (in == null) return null;
            ClassReader r = new ClassReader(in);
            i = new Info(r.getSuperName(), r.getInterfaces(), (r.getAccess() & Opcodes.ACC_INTERFACE) != 0);
            HIERARCHY.put(name, i);
            return i;
        } catch (IOException e) {
            return null;
        }
    }

    static boolean isInterface(String name) {
        if (name.startsWith("[")) return false;
        Info i = info(name);
        return i != null && i.isInterface();
    }

    static boolean isAssignableFrom(String parent, String child) {
        Deque<String> todo = new ArrayDeque<>();
        todo.add(child);
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) continue;
            if (c.equals(parent)) return true;
            Info i = info(c);
            if (i == null) continue;
            if (i.superName() != null) todo.add(i.superName());
            if (i.interfaces() != null) todo.addAll(Arrays.asList(i.interfaces()));
        }
        return false;
    }

    static class HierarchyWriter extends ClassWriter {
        HierarchyWriter(int flags) { super(flags); }

        @Override
        protected String getCommonSuperClass(String a, String b) {
            if (a.equals(b)) return a;
            if (isAssignableFrom(a, b)) return a;
            if (isAssignableFrom(b, a)) return b;
            if (isInterface(a) || isInterface(b)) return "java/lang/Object";
            String c = a;
            while (c != null) {
                Info i = info(c);
                if (i == null) return "java/lang/Object";
                c = i.superName();
                if (c != null && isAssignableFrom(c, b)) return c;
            }
            return "java/lang/Object";
        }
    }

    static final String HOOKS = "com/cnx/desktop/game/Hooks";
    /** "class name desc" of methods whose body is replaced by a hook call. */
    static final Set<String> PATCHES = ConcurrentHashMap.newKeySet();
    static final Set<String> APPLIED = ConcurrentHashMap.newKeySet();

    static void loadPatches(Path file) throws IOException {
        for (String line : Files.readAllLines(file)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            if (p.length != 3) throw new IllegalArgumentException("Bad patch line: " + line);
            PATCHES.add(p[0] + " " + p[1] + " " + p[2]);
        }
    }

    /** Writes: load (this +) args; invokestatic Hooks.Simple_name; return. */
    static void writeHookBody(MethodVisitor mv, String owner, int access, String name, String desc) {
        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        Type mt = Type.getMethodType(desc);
        List<Type> params = new ArrayList<>();
        if (!isStatic) params.add(Type.getObjectType(owner));
        params.addAll(Arrays.asList(mt.getArgumentTypes()));
        String hookDesc = Type.getMethodDescriptor(mt.getReturnType(), params.toArray(new Type[0]));
        String simple = owner.substring(owner.lastIndexOf('/') + 1).replace('$', '_');
        mv.visitCode();
        int slot = 0;
        for (Type t : params) { mv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), slot); slot += t.getSize(); }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, simple + "_" + name, hookDesc, false);
        mv.visitInsn(mt.getReturnType().getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    static byte[] fix(byte[] original) {
        ClassReader reader = new ClassReader(original);
        ClassWriter writer = new HierarchyWriter(ClassWriter.COMPUTE_FRAMES);
        final String[] owner = new String[1];
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                owner[0] = name;
                super.visit(Math.max(version & 0xFFFF, Opcodes.V1_8), access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                String key = owner[0] + " " + name + " " + desc;
                if (PATCHES.contains(key) && (access & Opcodes.ACC_ABSTRACT) == 0) {
                    APPLIED.add(key);
                    writeHookBody(mv, owner[0], access, name, desc);
                    return null; // drop the original code
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String mName, String mDesc, boolean itf) {
                        boolean realItf = opcode == Opcodes.INVOKEINTERFACE || (!owner.startsWith("[") && isInterface(owner));
                        super.visitMethodInsn(opcode, owner, mName, mDesc, realItf);
                    }

                    @Override
                    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
                        // recomputed by COMPUTE_FRAMES
                    }
                };
            }
        };
        reader.accept(cv, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("--patches")) {
            loadPatches(Paths.get(args[1]));
            args = Arrays.copyOfRange(args, 2, args.length);
        }
        if (args.length < 3) {
            System.err.println("usage: FixJars [--patches file] <out-dir> <hierarchy-classpath> <in.jar>...");
            System.exit(2);
        }
        Path outDir = Paths.get(args[0]);
        Files.createDirectories(outDir);
        List<Path> inputs = new ArrayList<>();
        for (int i = 2; i < args.length; i++) inputs.add(Paths.get(args[i]));

        for (Path p : inputs) indexJar(p);
        for (String cp : args[1].split(";")) if (!cp.isBlank()) indexJar(Paths.get(cp));
        System.out.println("indexed " + HIERARCHY.size() + " classes");

        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
        List<Future<String>> results = new ArrayList<>();
        for (Path in : inputs) {
            results.add(pool.submit(() -> {
                Path out = outDir.resolve(in.getFileName());
                int ok = 0;
                List<String> failed = new ArrayList<>();
                try (ZipFile z = new ZipFile(in.toFile());
                     ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(out)))) {
                    Enumeration<? extends ZipEntry> en = z.entries();
                    while (en.hasMoreElements()) {
                        ZipEntry e = en.nextElement();
                        if (e.isDirectory()) continue;
                        byte[] data;
                        try (InputStream is = z.getInputStream(e)) { data = is.readAllBytes(); }
                        if (e.getName().endsWith(".class")) {
                            try { data = fix(data); ok++; }
                            catch (Throwable t) { failed.add(e.getName() + " (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")"); }
                        }
                        zout.putNextEntry(new ZipEntry(e.getName()));
                        zout.write(data);
                        zout.closeEntry();
                    }
                }
                StringBuilder sb = new StringBuilder(in.getFileName() + ": fixed " + ok + ", unchanged " + failed.size());
                for (int i = 0; i < Math.min(5, failed.size()); i++) sb.append("\n    ").append(failed.get(i));
                return sb.toString();
            }));
        }
        pool.shutdown();
        for (Future<String> f : results) System.out.println(f.get());
        if (!PATCHES.isEmpty()) {
            System.out.println("patched methods: " + APPLIED.size() + " of " + PATCHES.size());
            for (String p : PATCHES) if (!APPLIED.contains(p)) System.out.println("  NOT FOUND: " + p);
        }
    }
}
