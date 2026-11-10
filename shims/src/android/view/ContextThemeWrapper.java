package android.view;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

/** Desktop ContextThemeWrapper: themes do not exist on the desktop. */
public class ContextThemeWrapper extends ContextWrapper {
    private int themeRes;

    public ContextThemeWrapper() { super(null); }
    public ContextThemeWrapper(Context base, int themeResId) { super(base); themeRes = themeResId; }
    public ContextThemeWrapper(Context base, Resources.Theme theme) { super(base); }

    public void setTheme(int resid) { themeRes = resid; }
    public Resources.Theme getTheme() { return getResources().newTheme(); }
    public void applyOverrideConfiguration(android.content.res.Configuration c) {}
}
