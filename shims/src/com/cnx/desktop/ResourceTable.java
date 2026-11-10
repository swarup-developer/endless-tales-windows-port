package com.cnx.desktop;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads the apktool-decoded resource tables:
 *  res/values/public.xml   -> id <-> (type, name)
 *  res/values[-xx]/strings.xml, colors.xml, integers.xml, bools.xml, arrays.xml -> values
 */
public final class ResourceTable {
    private static ResourceTable instance;

    private final Map<Integer, String[]> byId = new HashMap<>();      // id -> {type, name}
    private final Map<String, Integer> byName = new HashMap<>();      // "type/name" -> id
    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, String> colors = new HashMap<>();
    private final Map<String, String> integers = new HashMap<>();
    private final Map<String, String> bools = new HashMap<>();
    private final Map<String, String[]> arrays = new HashMap<>();

    public static synchronized ResourceTable get() {
        if (instance == null) instance = new ResourceTable();
        return instance;
    }

    /** Reload (e.g. after changing {@link DesktopRuntime#language}). */
    public static synchronized void reload() { instance = new ResourceTable(); }

    private ResourceTable() {
        Path values = DesktopRuntime.resDir.resolve("values");
        try {
            Document pub = parse(values.resolve("public.xml"));
            NodeList list = pub.getElementsByTagName("public");
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element) list.item(i);
                int id = (int) Long.parseLong(e.getAttribute("id").substring(2), 16);
                String type = e.getAttribute("type"), name = e.getAttribute("name");
                byId.put(id, new String[]{type, name});
                byName.put(type + "/" + name, id);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot read " + values.resolve("public.xml"), ex);
        }
        loadValues(values);
        String lang = DesktopRuntime.language;
        if (lang != null && !lang.isEmpty()) loadValues(DesktopRuntime.resDir.resolve("values-" + lang));
    }

    private void loadValues(Path dir) {
        loadSimple(dir.resolve("strings.xml"), "string", strings);
        loadSimple(dir.resolve("colors.xml"), "color", colors);
        loadSimple(dir.resolve("integers.xml"), "integer", integers);
        loadSimple(dir.resolve("bools.xml"), "bool", bools);
        Path arraysFile = dir.resolve("arrays.xml");
        if (Files.exists(arraysFile)) {
            try {
                Element root = parse(arraysFile).getDocumentElement();
                NodeList children = root.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    if (!(children.item(i) instanceof Element e)) continue;
                    NodeList items = e.getElementsByTagName("item");
                    String[] vals = new String[items.getLength()];
                    for (int j = 0; j < vals.length; j++) vals[j] = unescape(items.item(j).getTextContent());
                    arrays.put(e.getAttribute("name"), vals);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void loadSimple(Path file, String tag, Map<String, String> into) {
        if (!Files.exists(file)) return;
        try {
            NodeList list = parse(file).getElementsByTagName(tag);
            for (int i = 0; i < list.getLength(); i++) {
                Element e = (Element) list.item(i);
                into.put(e.getAttribute("name"), unescape(e.getTextContent()));
            }
        } catch (Exception ex) {
            DesktopRuntime.host.log("W", "ResourceTable", "Cannot read " + file + ": " + ex);
        }
    }

    private static Document parse(Path file) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        f.setExpandEntityReferences(false);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder().parse(file.toFile());
    }

    /** Android string-resource escaping: surrounding quotes, \n, \t, \', \", \@, \?, \\ and \\uXXXX. */
    static String unescape(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> b.append('\n');
                    case 't' -> b.append('\t');
                    case 'u' -> {
                        if (i + 4 < s.length()) { b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; }
                        else b.append(n);
                    }
                    default -> b.append(n);
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    public String typeOf(int id) { String[] t = byId.get(id); return t == null ? null : t[0]; }
    public String nameOf(int id) { String[] t = byId.get(id); return t == null ? null : t[1]; }
    public int idOf(String type, String name) { Integer i = byName.get(type + "/" + name); return i == null ? 0 : i; }

    public String string(int id) { String n = nameOf(id); return n == null ? null : strings.get(n); }
    public String stringByName(String name) { return strings.get(name); }
    public String[] array(int id) { String n = nameOf(id); return n == null ? null : arrays.get(n); }
    public String integer(int id) { String n = nameOf(id); return n == null ? null : integers.get(n); }
    public String bool(int id) { String n = nameOf(id); return n == null ? null : bools.get(n); }

    /** Resolves a color id (following @color/ references) to ARGB. Unknown colors are opaque black. */
    public int color(int id) {
        String n = nameOf(id);
        return n == null ? 0xFF000000 : colorByName(n, 0);
    }

    private int colorByName(String name, int depth) {
        String v = colors.get(name);
        if (v == null || depth > 8) return 0xFF000000;
        if (v.startsWith("@color/")) return colorByName(v.substring(7), depth + 1);
        if (v.startsWith("@android:color/")) return v.endsWith("white") ? 0xFFFFFFFF : v.endsWith("transparent") ? 0 : 0xFF000000;
        return parseColor(v);
    }

    public static int parseColor(String v) {
        try {
            String h = v.startsWith("#") ? v.substring(1) : v;
            if (h.length() == 3) h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            if (h.length() == 4) h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2) + h.charAt(3) + h.charAt(3);
            long val = Long.parseLong(h, 16);
            if (h.length() == 6) val |= 0xFF000000L;
            return (int) val;
        } catch (Exception e) {
            return 0xFF000000;
        }
    }
}
