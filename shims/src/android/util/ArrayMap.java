package android.util;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/** Desktop replacement for android.util.ArrayMap: an insertion-ordered map with index access. */
public final class ArrayMap<K, V> implements Map<K, V> {
    private final ArrayList<K> keys;
    private final ArrayList<V> vals;
    private final HashMap<Object, Integer> index;

    public ArrayMap() { this(0); }
    public ArrayMap(int capacity) {
        keys = new ArrayList<>(capacity); vals = new ArrayList<>(capacity); index = new HashMap<>(Math.max(4, capacity * 2));
    }
    public ArrayMap(ArrayMap<K, V> map) { this(map == null ? 0 : map.size()); if (map != null) putAll((Map<K, V>) map); }

    private void reindex() { index.clear(); for (int i = 0; i < keys.size(); i++) index.put(keys.get(i), i); }

    public void clear() { keys.clear(); vals.clear(); index.clear(); }
    public void ensureCapacity(int n) { keys.ensureCapacity(n); vals.ensureCapacity(n); }
    public boolean containsKey(Object k) { return index.containsKey(k); }
    public int indexOfKey(Object k) { Integer i = index.get(k); return i == null ? -1 : i; }
    public int indexOfValue(Object v) { return vals.indexOf(v); }
    public boolean containsValue(Object v) { return vals.contains(v); }
    public V get(Object k) { Integer i = index.get(k); return i == null ? null : vals.get(i); }
    public V getOrDefault(Object k, V d) { Integer i = index.get(k); return i == null ? d : vals.get(i); }
    public K keyAt(int i) { return keys.get(i); }
    public V valueAt(int i) { return vals.get(i); }
    public V setValueAt(int i, V v) { return vals.set(i, v); }
    public boolean isEmpty() { return keys.isEmpty(); }
    public V put(K k, V v) {
        Integer i = index.get(k);
        if (i != null) return vals.set(i, v);
        index.put(k, keys.size()); keys.add(k); vals.add(v); return null;
    }
    public void putAll(ArrayMap<? extends K, ? extends V> m) { for (int i = 0; i < m.size(); i++) put(m.keyAt(i), m.valueAt(i)); }
    public void putAll(Map<? extends K, ? extends V> m) { for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue()); }
    public V remove(Object k) { Integer i = index.get(k); return i == null ? null : removeAt(i); }
    public V removeAt(int i) { keys.remove(i); V v = vals.remove(i); reindex(); return v; }
    public int size() { return keys.size(); }
    public boolean containsAll(Collection<?> c) { for (Object o : c) if (!containsKey(o)) return false; return true; }
    public boolean removeAll(Collection<?> c) { boolean r = false; for (Object o : c) if (index.containsKey(o)) { remove(o); r = true; } return r; }
    public boolean retainAll(Collection<?> c) {
        boolean r = false;
        for (int i = keys.size() - 1; i >= 0; i--) if (!c.contains(keys.get(i))) { keys.remove(i); vals.remove(i); r = true; }
        if (r) reindex();
        return r;
    }
    public void forEach(BiConsumer<? super K, ? super V> a) { for (int i = 0; i < keys.size(); i++) a.accept(keys.get(i), vals.get(i)); }
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> f) { for (int i = 0; i < keys.size(); i++) vals.set(i, f.apply(keys.get(i), vals.get(i))); }

    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            public int size() { return keys.size(); }
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<>() {
                    int pos = 0; boolean canRemove = false;
                    public boolean hasNext() { return pos < keys.size(); }
                    public Map.Entry<K, V> next() {
                        if (pos >= keys.size()) throw new NoSuchElementException();
                        final K k = keys.get(pos++); canRemove = true;
                        return new Map.Entry<>() {
                            public K getKey() { return k; }
                            public V getValue() { return get(k); }
                            public V setValue(V v) { return put(k, v); }
                            public boolean equals(Object o) { return o instanceof Map.Entry<?, ?> e && Objects.equals(k, e.getKey()) && Objects.equals(getValue(), e.getValue()); }
                            public int hashCode() { return Objects.hashCode(k) ^ Objects.hashCode(getValue()); }
                            public String toString() { return k + "=" + getValue(); }
                        };
                    }
                    public void remove() { if (!canRemove) throw new IllegalStateException(); removeAt(--pos); canRemove = false; }
                };
            }
        };
    }
    public Set<K> keySet() {
        return new AbstractSet<>() {
            public int size() { return keys.size(); }
            public boolean contains(Object o) { return containsKey(o); }
            public boolean remove(Object o) { if (!containsKey(o)) return false; ArrayMap.this.remove(o); return true; }
            public Iterator<K> iterator() {
                Iterator<Map.Entry<K, V>> it = entrySet().iterator();
                return new Iterator<>() {
                    public boolean hasNext() { return it.hasNext(); }
                    public K next() { return it.next().getKey(); }
                    public void remove() { it.remove(); }
                };
            }
        };
    }
    public Collection<V> values() {
        return new AbstractCollection<>() {
            public int size() { return keys.size(); }
            public Iterator<V> iterator() {
                Iterator<Map.Entry<K, V>> it = entrySet().iterator();
                return new Iterator<>() {
                    public boolean hasNext() { return it.hasNext(); }
                    public V next() { return it.next().getValue(); }
                    public void remove() { it.remove(); }
                };
            }
        };
    }
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map<?, ?> m) || m.size() != size()) return false;
        for (int i = 0; i < keys.size(); i++) if (!Objects.equals(vals.get(i), m.get(keys.get(i)))) return false;
        return true;
    }
    public int hashCode() { int h = 0; for (int i = 0; i < keys.size(); i++) h += Objects.hashCode(keys.get(i)) ^ Objects.hashCode(vals.get(i)); return h; }
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) { if (i > 0) b.append(", "); b.append(keys.get(i)).append('=').append(vals.get(i)); }
        return b.append('}').toString();
    }
}
