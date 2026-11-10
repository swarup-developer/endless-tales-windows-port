package android.content;

import android.os.Bundle;

/** Desktop Intent: only carries extras. Activities are not launched through Intents on the desktop. */
public class Intent {
    public static final String ACTION_MAIN = "android.intent.action.MAIN", ACTION_VIEW = "android.intent.action.VIEW",
            ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED", CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000, FLAG_ACTIVITY_CLEAR_TASK = 0x8000, FLAG_ACTIVITY_CLEAR_TOP = 0x4000;

    private String action;
    private Class<?> target;
    private final java.util.HashMap<String, Object> extras = new java.util.HashMap<>();
    private int flags;

    public Intent() {}
    public Intent(String action) { this.action = action; }
    public Intent(Context ctx, Class<?> cls) { target = cls; }
    public Intent(Intent o) { action = o.action; target = o.target; extras.putAll(o.extras); flags = o.flags; }

    public String getAction() { return action; }
    public Intent setAction(String a) { action = a; return this; }
    public Class<?> getTargetClass() { return target; }
    public Intent setClass(Context ctx, Class<?> cls) { target = cls; return this; }
    public Intent addFlags(int f) { flags |= f; return this; }
    public Intent setFlags(int f) { flags = f; return this; }
    public int getFlags() { return flags; }

    public Intent putExtra(String k, String v) { extras.put(k, v); return this; }
    public Intent putExtra(String k, int v) { extras.put(k, v); return this; }
    public Intent putExtra(String k, long v) { extras.put(k, v); return this; }
    public Intent putExtra(String k, boolean v) { extras.put(k, v); return this; }
    public Intent putExtra(String k, float v) { extras.put(k, v); return this; }
    public Intent putExtra(String k, java.io.Serializable v) { extras.put(k, v); return this; }
    public String getStringExtra(String k) { Object v = extras.get(k); return v instanceof String s ? s : null; }
    public int getIntExtra(String k, int d) { Object v = extras.get(k); return v instanceof Integer i ? i : d; }
    public long getLongExtra(String k, long d) { Object v = extras.get(k); return v instanceof Long l ? l : d; }
    public boolean getBooleanExtra(String k, boolean d) { Object v = extras.get(k); return v instanceof Boolean b ? b : d; }
    public java.io.Serializable getSerializableExtra(String k) { Object v = extras.get(k); return v instanceof java.io.Serializable s ? s : null; }
    public boolean hasExtra(String k) { return extras.containsKey(k); }
    public Bundle getExtras() { return null; }
}
