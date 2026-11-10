package android.util;

/** Desktop DisplayMetrics (plain fields, density 1). */
public class DisplayMetrics {
    public static final int DENSITY_DEFAULT = 160, DENSITY_MEDIUM = 160, DENSITY_HIGH = 240, DENSITY_XHIGH = 320;

    public float density = 1f;
    public int densityDpi = 160;
    public int heightPixels = 720;
    public int widthPixels = 1280;
    public float scaledDensity = 1f;
    public float xdpi = 160f;
    public float ydpi = 160f;

    public DisplayMetrics() {}

    public void setTo(DisplayMetrics o) {
        density = o.density; densityDpi = o.densityDpi; heightPixels = o.heightPixels; widthPixels = o.widthPixels;
        scaledDensity = o.scaledDensity; xdpi = o.xdpi; ydpi = o.ydpi;
    }

    public void setToDefaults() { setTo(new DisplayMetrics()); }
}
