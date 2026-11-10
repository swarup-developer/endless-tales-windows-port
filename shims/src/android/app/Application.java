package android.app;

import android.content.ContextWrapper;

/** Desktop Application: a plain Context. */
public class Application extends ContextWrapper {
    public Application() { super(null); }

    public void onCreate() {}
    public void onTerminate() {}
    public void onLowMemory() {}
}
