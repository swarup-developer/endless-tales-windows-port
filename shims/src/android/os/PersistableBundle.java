package android.os;

/** Desktop PersistableBundle. */
public final class PersistableBundle extends BaseBundle {
    public static final PersistableBundle EMPTY = new PersistableBundle();

    public PersistableBundle() {}
    public PersistableBundle(int capacity) {}
    public PersistableBundle(PersistableBundle b) { map.putAll(b.map); }

    public void putPersistableBundle(String k, PersistableBundle v) { map.put(k, v); }
    public PersistableBundle getPersistableBundle(String k) { return typed(k, PersistableBundle.class); }
}
