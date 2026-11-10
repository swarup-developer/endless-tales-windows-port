package com.cnx.desktop;

import android.content.SharedPreferences;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** SharedPreferences stored as a .properties file (values are kept as "type:value"). */
public final class DesktopPreferences implements SharedPreferences {
    private final Path file;
    private final Properties props = new Properties();
    private final List<OnSharedPreferenceChangeListener> listeners = new ArrayList<>();

    public DesktopPreferences(Path file) {
        this.file = file;
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { props.load(r); } catch (IOException ignored) {}
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { props.store(w, null); }
        } catch (IOException e) {
            DesktopRuntime.host.log("E", "Prefs", "Cannot save " + file + ": " + e);
        }
    }

    private synchronized String raw(String key) { return props.getProperty(key); }
    private static String val(String raw) { return raw == null ? null : raw.substring(raw.indexOf(':') + 1); }

    @Override public synchronized Map<String, ?> getAll() {
        Map<String, Object> m = new HashMap<>();
        for (String k : props.stringPropertyNames()) m.put(k, val(props.getProperty(k)));
        return m;
    }
    @Override public String getString(String k, String d) { String r = raw(k); return r == null ? d : val(r); }
    @Override public Set<String> getStringSet(String k, Set<String> d) {
        String r = raw(k);
        if (r == null) return d;
        String v = val(r);
        return v.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(v.split("\u001f")));
    }
    @Override public int getInt(String k, int d) { String r = raw(k); try { return r == null ? d : Integer.parseInt(val(r)); } catch (Exception e) { return d; } }
    @Override public long getLong(String k, long d) { String r = raw(k); try { return r == null ? d : Long.parseLong(val(r)); } catch (Exception e) { return d; } }
    @Override public float getFloat(String k, float d) { String r = raw(k); try { return r == null ? d : Float.parseFloat(val(r)); } catch (Exception e) { return d; } }
    @Override public boolean getBoolean(String k, boolean d) { String r = raw(k); return r == null ? d : Boolean.parseBoolean(val(r)); }
    @Override public boolean contains(String k) { return raw(k) != null; }
    @Override public Editor edit() { return new Ed(); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { listeners.add(l); }
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { listeners.remove(l); }

    private final class Ed implements Editor {
        private final Map<String, String> puts = new LinkedHashMap<>();
        private final Set<String> removes = new HashSet<>();
        private boolean clear;

        @Override public Editor putString(String k, String v) { if (v == null) return remove(k); puts.put(k, "s:" + v); return this; }
        @Override public Editor putStringSet(String k, Set<String> v) { if (v == null) return remove(k); puts.put(k, "S:" + String.join("\u001f", v)); return this; }
        @Override public Editor putInt(String k, int v) { puts.put(k, "i:" + v); return this; }
        @Override public Editor putLong(String k, long v) { puts.put(k, "l:" + v); return this; }
        @Override public Editor putFloat(String k, float v) { puts.put(k, "f:" + v); return this; }
        @Override public Editor putBoolean(String k, boolean v) { puts.put(k, "b:" + v); return this; }
        @Override public Editor remove(String k) { removes.add(k); puts.remove(k); return this; }
        @Override public Editor clear() { clear = true; return this; }
        @Override public boolean commit() { apply(); return true; }
        @Override public void apply() {
            synchronized (DesktopPreferences.this) {
                if (clear) props.clear();
                for (String k : removes) props.remove(k);
                props.putAll(puts);
                save();
            }
        }
    }
}
