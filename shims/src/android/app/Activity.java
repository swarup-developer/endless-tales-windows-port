package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.MainThread;
import android.view.ContextThemeWrapper;
import android.view.View;

/**
 * Desktop Activity. The game's own activities are subclassed by the desktop UI so that calls made
 * from game scripts (start a battle, open a shop, ...) reach the JavaFX screens. Lifecycle methods
 * are plain no-ops.
 */
public class Activity extends ContextThemeWrapper {
    public static final int RESULT_CANCELED = 0, RESULT_OK = -1;

    private Intent intent = new Intent();
    private boolean finishing;

    public Activity() {}

    protected void onCreate(Bundle savedInstanceState) {}
    protected void onStart() {}
    protected void onResume() {}
    protected void onPause() {}
    protected void onStop() {}
    protected void onDestroy() {}
    protected void onPostCreate(Bundle savedInstanceState) {}
    protected void onPostResume() {}
    protected void onSaveInstanceState(Bundle outState) {}
    public void onBackPressed() {}

    public Intent getIntent() { return intent; }
    public void setIntent(Intent i) { intent = i; }

    public void runOnUiThread(Runnable action) { MainThread.post(action, 0); }

    public void finish() { finishing = true; }
    public void finishAffinity() { finish(); }
    public boolean isFinishing() { return finishing; }
    public boolean isDestroyed() { return false; }

    public void setContentView(int layoutResID) {}
    public void setContentView(View view) {}
    public <T extends View> T findViewById(int id) { return null; }
    public final Activity getParent() { return null; }
    public Application getApplication() { return null; }

    @Override public void startActivity(Intent intent) {}
    public void startActivityForResult(Intent intent, int requestCode) {}
    public void setRequestedOrientation(int o) {}
    public int getRequestedOrientation() { return 0; }
    public void overridePendingTransition(int enterAnim, int exitAnim) {}
}
