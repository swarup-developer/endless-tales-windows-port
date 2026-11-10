package android.content.res;

import com.cnx.desktop.DesktopRuntime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/** Desktop AssetManager: serves files from {@link DesktopRuntime#assetsDir}. */
public final class AssetManager implements AutoCloseable {
    public static final int ACCESS_UNKNOWN = 0, ACCESS_RANDOM = 1, ACCESS_STREAMING = 2, ACCESS_BUFFER = 3;

    public AssetManager() {}

    private static File file(String path) throws FileNotFoundException {
        if (path == null) throw new FileNotFoundException("null");
        String p = path.replace('\\', '/');
        while (p.startsWith("/")) p = p.substring(1);
        if (p.contains("..")) throw new FileNotFoundException(path);
        File f = DesktopRuntime.assetsDir.resolve(p).toFile();
        if (!f.isFile()) throw new FileNotFoundException(path);
        return f;
    }

    public InputStream open(String fileName) throws IOException { return new FileInputStream(file(fileName)); }
    public InputStream open(String fileName, int accessMode) throws IOException { return open(fileName); }

    public AssetFileDescriptor openFd(String fileName) throws IOException {
        File f = file(fileName);
        return new AssetFileDescriptor(f, 0, f.length());
    }

    public String[] list(String path) throws IOException {
        File d = DesktopRuntime.assetsDir.resolve(path == null ? "" : path).toFile();
        String[] names = d.list();
        if (names == null) return new String[0];
        Arrays.sort(names);
        return names;
    }

    @Override public void close() {}
}
