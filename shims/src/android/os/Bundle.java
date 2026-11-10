package android.os;

import java.io.Serializable;
import java.util.ArrayList;

/** Desktop Bundle: a plain key/value map (nothing is ever parcelled). */
public final class Bundle extends BaseBundle implements Cloneable, Parcelable {
    public static final Bundle EMPTY = new Bundle();

    public Bundle() {}
    public Bundle(int capacity) {}
    public Bundle(ClassLoader loader) {}
    public Bundle(Bundle b) { if (b != null) map.putAll(b.map); }
    public Bundle(PersistableBundle b) { if (b != null) map.putAll(b.map); }

    @Override public Object clone() { return new Bundle(this); }
    public Bundle deepCopy() { return new Bundle(this); }
    public void setClassLoader(ClassLoader loader) {}
    public ClassLoader getClassLoader() { return Bundle.class.getClassLoader(); }
    public boolean hasFileDescriptors() { return false; }
    public void putAll(Bundle b) { if (b != null) map.putAll(b.map); }

    public void putByte(String k, byte v) { map.put(k, v); }
    public void putChar(String k, char v) { map.put(k, v); }
    public void putShort(String k, short v) { map.put(k, v); }
    public void putFloat(String k, float v) { map.put(k, v); }
    public void putCharSequence(String k, CharSequence v) { map.put(k, v); }
    public void putParcelable(String k, Parcelable v) { map.put(k, v); }
    public void putParcelableArray(String k, Parcelable[] v) { map.put(k, v); }
    public void putParcelableArrayList(String k, ArrayList<? extends Parcelable> v) { map.put(k, v); }
    public void putIntegerArrayList(String k, ArrayList<Integer> v) { map.put(k, v); }
    public void putStringArrayList(String k, ArrayList<String> v) { map.put(k, v); }
    public void putCharSequenceArrayList(String k, ArrayList<CharSequence> v) { map.put(k, v); }
    public void putSerializable(String k, Serializable v) { map.put(k, v); }
    public void putByteArray(String k, byte[] v) { map.put(k, v); }
    public void putShortArray(String k, short[] v) { map.put(k, v); }
    public void putCharArray(String k, char[] v) { map.put(k, v); }
    public void putFloatArray(String k, float[] v) { map.put(k, v); }
    public void putCharSequenceArray(String k, CharSequence[] v) { map.put(k, v); }
    public void putBundle(String k, Bundle v) { map.put(k, v); }
    public void putBinder(String k, IBinder v) { map.put(k, v); }

    public byte getByte(String k) { return getByte(k, (byte) 0); }
    public Byte getByte(String k, byte d) { Byte v = typed(k, Byte.class); return v == null ? d : v; }
    public char getChar(String k) { return getChar(k, (char) 0); }
    public char getChar(String k, char d) { Character v = typed(k, Character.class); return v == null ? d : v; }
    public short getShort(String k) { return getShort(k, (short) 0); }
    public short getShort(String k, short d) { Short v = typed(k, Short.class); return v == null ? d : v; }
    public float getFloat(String k) { return getFloat(k, 0f); }
    public float getFloat(String k, float d) { Float v = typed(k, Float.class); return v == null ? d : v; }
    public CharSequence getCharSequence(String k) { return typed(k, CharSequence.class); }
    public CharSequence getCharSequence(String k, CharSequence d) { CharSequence v = typed(k, CharSequence.class); return v == null ? d : v; }
    public Bundle getBundle(String k) { return typed(k, Bundle.class); }
    @SuppressWarnings("unchecked") public <T extends Parcelable> T getParcelable(String k) { return (T) typed(k, Parcelable.class); }
    public <T> T getParcelable(String k, Class<T> c) { return typed(k, c); }
    public Parcelable[] getParcelableArray(String k) { return typed(k, Parcelable[].class); }
    @SuppressWarnings("unchecked") public <T extends Parcelable> ArrayList<T> getParcelableArrayList(String k) { return (ArrayList<T>) typed(k, ArrayList.class); }
    @SuppressWarnings("unchecked") public <T> ArrayList<T> getParcelableArrayList(String k, Class<? extends T> c) { return (ArrayList<T>) typed(k, ArrayList.class); }
    public Serializable getSerializable(String k) { return typed(k, Serializable.class); }
    public <T extends Serializable> T getSerializable(String k, Class<T> c) { return typed(k, c); }
    @SuppressWarnings("unchecked") public ArrayList<Integer> getIntegerArrayList(String k) { return (ArrayList<Integer>) typed(k, ArrayList.class); }
    @SuppressWarnings("unchecked") public ArrayList<String> getStringArrayList(String k) { return (ArrayList<String>) typed(k, ArrayList.class); }
    @SuppressWarnings("unchecked") public ArrayList<CharSequence> getCharSequenceArrayList(String k) { return (ArrayList<CharSequence>) typed(k, ArrayList.class); }
    public byte[] getByteArray(String k) { return typed(k, byte[].class); }
    public short[] getShortArray(String k) { return typed(k, short[].class); }
    public char[] getCharArray(String k) { return typed(k, char[].class); }
    public float[] getFloatArray(String k) { return typed(k, float[].class); }
    public CharSequence[] getCharSequenceArray(String k) { return typed(k, CharSequence[].class); }
    public IBinder getBinder(String k) { return typed(k, IBinder.class); }

    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) {}
}
