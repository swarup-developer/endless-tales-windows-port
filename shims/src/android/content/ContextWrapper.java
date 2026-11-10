package android.content;

/** Desktop ContextWrapper: wraps another Context (or itself). */
public class ContextWrapper extends Context {
    private Context base;

    public ContextWrapper(Context base) { this.base = base; }

    protected void attachBaseContext(Context base) { this.base = base; }

    public Context getBaseContext() { return base != null ? base : this; }
}
