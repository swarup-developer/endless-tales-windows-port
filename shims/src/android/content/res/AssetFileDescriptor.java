package android.content.res;

import java.io.File;
import java.io.FileDescriptor;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Desktop AssetFileDescriptor. It remembers which asset file it points to, so the desktop
 * MediaPlayer can find the file again from the FileDescriptor the game passes to setDataSource().
 */
public class AssetFileDescriptor implements AutoCloseable {
    public static final long UNKNOWN_LENGTH = -1;

    private static final Map<FileDescriptor, File> FILES = Collections.synchronizedMap(new WeakHashMap<>());

    private final File file;
    private final long start, length;
    private final FileDescriptor fd = new FileDescriptor();

    AssetFileDescriptor(File file, long start, long length) {
        this.file = file;
        this.start = start;
        this.length = length;
        FILES.put(fd, file);
    }

    /** Desktop helper: the asset file behind a descriptor returned by {@link #getFileDescriptor()}. */
    public static File fileFor(FileDescriptor fd) { return fd == null ? null : FILES.get(fd); }

    public File getFile() { return file; }
    public FileDescriptor getFileDescriptor() { return fd; }
    public long getStartOffset() { return start; }
    public long getLength() { return length; }
    public long getDeclaredLength() { return length; }
    public int describeContents() { return 0; }
    @Override public void close() {}
    @Override public String toString() { return "AssetFileDescriptor{" + file + "}"; }
}
