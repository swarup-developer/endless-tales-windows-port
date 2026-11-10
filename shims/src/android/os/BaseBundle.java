package android.os;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Desktop BaseBundle: a plain key/value map. */
public class BaseBundle {
    final LinkedHashMap<String, Object> map = new LinkedHashMap<>();

    BaseBundle() {}

    public int size() { return map.size(); }
    public boolean isEmpty() { return map.isEmpty(); }
    public void clear() { map.clear(); }
    public boolean containsKey(String key) { return map.containsKey(key); }
    public Object get(String key) { return map.get(key); }
    public void remove(String key) { map.remove(key); }
    public void putAll(PersistableBundle bundle) { map.putAll(bundle.map); }
    public Set<String> keySet() { return map.keySet(); }

    public void putBoolean(String k, boolean v) { map.put(k, v); }
    public void putInt(String k, int v) { map.put(k, v); }
    public void putLong(String k, long v) { map.put(k, v); }
    public void putDouble(String k, double v) { map.put(k, v); }
    public void putString(String k, String v) { map.put(k, v); }
    public void putBooleanArray(String k, boolean[] v) { map.put(k, v); }
    public void putIntArray(String k, int[] v) { map.put(k, v); }
    public void putLongArray(String k, long[] v) { map.put(k, v); }
    public void putDoubleArray(String k, double[] v) { map.put(k, v); }
    public void putStringArray(String k, String[] v) { map.put(k, v); }

    @SuppressWarnings("unchecked")
    <T> T typed(String k, Class<T> c) { Object v = map.get(k); return c.isInstance(v) ? (T) v : null; }

    public boolean getBoolean(String k) { return getBoolean(k, false); }
    public boolean getBoolean(String k, boolean d) { Boolean v = typed(k, Boolean.class); return v == null ? d : v; }
    public int getInt(String k) { return getInt(k, 0); }
    public int getInt(String k, int d) { Integer v = typed(k, Integer.class); return v == null ? d : v; }
    public long getLong(String k) { return getLong(k, 0L); }
    public long getLong(String k, long d) { Long v = typed(k, Long.class); return v == null ? d : v; }
    public double getDouble(String k) { return getDouble(k, 0d); }
    public double getDouble(String k, double d) { Double v = typed(k, Double.class); return v == null ? d : v; }
    public String getString(String k) { return typed(k, String.class); }
    public String getString(String k, String d) { String v = typed(k, String.class); return v == null ? d : v; }
    public boolean[] getBooleanArray(String k) { return typed(k, boolean[].class); }
    public int[] getIntArray(String k) { return typed(k, int[].class); }
    public long[] getLongArray(String k) { return typed(k, long[].class); }
    public double[] getDoubleArray(String k) { return typed(k, double[].class); }
    public String[] getStringArray(String k) { return typed(k, String[].class); }

    @Override public String toString() { return getClass().getSimpleName() + map; }
}
