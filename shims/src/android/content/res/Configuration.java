package android.content.res;

/** Desktop Configuration: landscape, normal screen. */
public final class Configuration {
    public static final int ORIENTATION_PORTRAIT = 1, ORIENTATION_LANDSCAPE = 2;
    public static final int UI_MODE_NIGHT_MASK = 0x30, UI_MODE_NIGHT_NO = 0x10, UI_MODE_NIGHT_YES = 0x20;

    public int orientation = ORIENTATION_LANDSCAPE;
    public int screenWidthDp = 1280;
    public int screenHeightDp = 720;
    public int smallestScreenWidthDp = 720;
    public int densityDpi = 160;
    public float fontScale = 1f;
    public int uiMode = UI_MODE_NIGHT_NO;

    public Configuration() {}
}
