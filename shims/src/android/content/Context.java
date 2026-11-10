package android.content;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Looper;
import com.cnx.desktop.DesktopPreferences;
import com.cnx.desktop.DesktopRuntime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Desktop Context. Unlike Android's abstract Context, every method used by the game logic has a
 * working default here: resources from the decoded res/, assets from assets/, files under the
 * user's save folder.
 */
public abstract class Context {
    public static final int MODE_PRIVATE = 0, MODE_APPEND = 0x8000;
    public static final String ALARM_SERVICE = "alarm", NOTIFICATION_SERVICE = "notification",
            AUDIO_SERVICE = "audio", VIBRATOR_SERVICE = "vibrator", CONNECTIVITY_SERVICE = "connectivity",
            WINDOW_SERVICE = "window", LAYOUT_INFLATER_SERVICE = "layout_inflater", INPUT_METHOD_SERVICE = "input_method";

    private static final Resources RESOURCES = new Resources();
    private static final AssetManager ASSETS = new AssetManager();
    private static final Map<String, DesktopPreferences> PREFS = new HashMap<>();

    public Context() {}

    public Resources getResources() { return RESOURCES; }
    public AssetManager getAssets() { return ASSETS; }
    public Context getApplicationContext() { return this; }
    public String getPackageName() { return DesktopRuntime.PACKAGE; }
    public Looper getMainLooper() { return Looper.getMainLooper(); }

    public final String getString(int resId) { return getResources().getString(resId); }
    public final String getString(int resId, Object... formatArgs) { return getResources().getString(resId, formatArgs); }
    public final CharSequence getText(int resId) { return getResources().getText(resId); }
    public final int getColor(int id) { return getResources().getColor(id); }

    public File getFilesDir() { return DesktopRuntime.filesDir(); }
    public File getCacheDir() { File f = DesktopRuntime.dataDir.resolve("cache").toFile(); f.mkdirs(); return f; }
    public File getFileStreamPath(String name) { return new File(getFilesDir(), name); }
    public FileInputStream openFileInput(String name) throws FileNotFoundException { return new FileInputStream(getFileStreamPath(name)); }
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return new FileOutputStream(getFileStreamPath(name), (mode & MODE_APPEND) != 0);
    }
    public boolean deleteFile(String name) { return getFileStreamPath(name).delete(); }
    public String[] fileList() { String[] l = getFilesDir().list(); return l == null ? new String[0] : l; }

    public SharedPreferences getSharedPreferences(String name, int mode) {
        synchronized (PREFS) {
            return PREFS.computeIfAbsent(name, n -> new DesktopPreferences(DesktopRuntime.dataDir.resolve("prefs").resolve(n + ".properties")));
        }
    }

    /** No Android system services on the desktop. */
    public Object getSystemService(String name) { return null; }

    public void startActivity(Intent intent) {}
    public void sendBroadcast(Intent intent) {}
}
