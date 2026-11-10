package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Desktop MediaPlayer. The game calls the normal Android API; the real playback is done by an
 * {@link Backend} that the desktop UI installs (JavaFX media). Without a backend it is silent.
 */
public class MediaPlayer {
    public interface OnCompletionListener { void onCompletion(MediaPlayer mp); }
    public interface OnPreparedListener { void onPrepared(MediaPlayer mp); }
    public interface OnErrorListener { boolean onError(MediaPlayer mp, int what, int extra); }

    /** One playing sound. */
    public interface Track {
        void play();
        void pause();
        void stop();
        void dispose();
        void setVolume(float volume);
        void setLooping(boolean looping);
        boolean isPlaying();
        int positionMillis();
        int durationMillis();
        void setOnEnd(Runnable onEnd);
    }

    public interface Backend { Track open(File file); }

    public static volatile Backend backend = null;

    private File file;
    private Track track;
    private boolean looping;
    private float volume = 1f;
    private OnCompletionListener onCompletion;

    public MediaPlayer() {}

    public static MediaPlayer create(Context context, int resId) {
        // Sound resources in res/raw are not used for music by the desktop build.
        return null;
    }

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException {
        File f = AssetFileDescriptor.fileFor(fd);
        if (f == null) throw new IOException("Unknown file descriptor");
        file = f;
    }

    public void setDataSource(FileDescriptor fd) throws IOException { setDataSource(fd, 0, -1); }
    public void setDataSource(AssetFileDescriptor afd) throws IOException { file = afd.getFile(); }
    public void setDataSource(String path) throws IOException { file = new File(path); }

    public void prepare() throws IOException {
        releaseTrack();
        Backend b = backend;
        if (b == null || file == null) return;
        track = b.open(file);
        if (track != null) {
            track.setLooping(looping);
            track.setVolume(volume);
            track.setOnEnd(() -> { OnCompletionListener l = onCompletion; if (l != null) l.onCompletion(this); });
        }
    }

    public void prepareAsync() { try { prepare(); } catch (IOException ignored) {} }

    public void start() { if (track != null) track.play(); }
    public void pause() { if (track != null) track.pause(); }
    public void stop() { if (track != null) track.stop(); }
    public void reset() { releaseTrack(); file = null; }
    public void release() { releaseTrack(); }

    private void releaseTrack() { if (track != null) { track.dispose(); track = null; } }

    public boolean isPlaying() { return track != null && track.isPlaying(); }
    public void setLooping(boolean l) { looping = l; if (track != null) track.setLooping(l); }
    public boolean isLooping() { return looping; }
    public void setVolume(float left, float right) { volume = Math.max(left, right); if (track != null) track.setVolume(volume); }
    public int getCurrentPosition() { return track == null ? 0 : track.positionMillis(); }
    public int getDuration() { return track == null ? 0 : track.durationMillis(); }
    public void seekTo(int msec) {}

    public void setOnCompletionListener(OnCompletionListener l) { onCompletion = l; }
    public void setOnPreparedListener(OnPreparedListener l) {}
    public void setOnErrorListener(OnErrorListener l) {}
}
