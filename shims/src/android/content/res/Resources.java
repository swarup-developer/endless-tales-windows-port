package android.content.res;

import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import com.cnx.desktop.DesktopRuntime;
import com.cnx.desktop.ResourceTable;

/** Desktop Resources backed by the decoded apktool resource tables. */
public class Resources {
    public static class NotFoundException extends RuntimeException {
        public NotFoundException() {}
        public NotFoundException(String name) { super(name); }
    }

    public final class Theme {}

    private static final Resources SYSTEM = new Resources();
    private final DisplayMetrics metrics = new DisplayMetrics();

    public Resources() {
        metrics.density = 1f;
        metrics.scaledDensity = 1f;
        metrics.densityDpi = 160;
        metrics.widthPixels = 1280;
        metrics.heightPixels = 720;
        metrics.xdpi = 160f;
        metrics.ydpi = 160f;
    }

    public static Resources getSystem() { return SYSTEM; }

    public String getString(int id) {
        String s = ResourceTable.get().string(id);
        if (s == null) throw new NotFoundException("String resource ID #0x" + Integer.toHexString(id));
        return s;
    }

    public String getString(int id, Object... args) {
        return String.format(getString(id), args);
    }

    public CharSequence getText(int id) { return getString(id); }

    public String[] getStringArray(int id) {
        String[] a = ResourceTable.get().array(id);
        if (a == null) throw new NotFoundException("Array resource ID #0x" + Integer.toHexString(id));
        return a.clone();
    }

    public int getInteger(int id) {
        String v = ResourceTable.get().integer(id);
        if (v == null) throw new NotFoundException("Integer resource ID #0x" + Integer.toHexString(id));
        return Integer.parseInt(v.trim());
    }

    public boolean getBoolean(int id) { return Boolean.parseBoolean(ResourceTable.get().bool(id)); }

    public int getColor(int id) { return ResourceTable.get().color(id); }
    public int getColor(int id, Theme theme) { return getColor(id); }

    public int getIdentifier(String name, String type, String pkg) {
        if (name == null) return 0;
        String t = type, n = name;
        int colon = n.indexOf(':');
        if (colon >= 0) n = n.substring(colon + 1);
        int slash = n.indexOf('/');
        if (slash >= 0) { if (t == null) t = n.substring(0, slash); n = n.substring(slash + 1); }
        if (t == null) return 0;
        return ResourceTable.get().idOf(t, n);
    }

    public String getResourceName(int id) { return DesktopRuntime.PACKAGE + ":" + getResourceTypeName(id) + "/" + getResourceEntryName(id); }
    public String getResourceEntryName(int id) { String n = ResourceTable.get().nameOf(id); if (n == null) throw new NotFoundException(); return n; }
    public String getResourceTypeName(int id) { String t = ResourceTable.get().typeOf(id); if (t == null) throw new NotFoundException(); return t; }
    public String getResourcePackageName(int id) { return DesktopRuntime.PACKAGE; }

    /** Drawables are drawn by the desktop UI itself; game logic only passes them along. */
    public Drawable getDrawable(int id) { return null; }
    public Drawable getDrawable(int id, Theme theme) { return null; }

    public DisplayMetrics getDisplayMetrics() { return metrics; }
    public Configuration getConfiguration() { return new Configuration(); }
    public final Theme newTheme() { return new Theme(); }
}
