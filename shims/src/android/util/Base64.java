package android.util;

/** Desktop android.util.Base64 on top of java.util.Base64 (same flag values as Android). */
public class Base64 {
    public static final int DEFAULT = 0, NO_PADDING = 1, NO_WRAP = 2, CRLF = 4, URL_SAFE = 8, NO_CLOSE = 16;

    private Base64() {}

    public static byte[] decode(String str, int flags) {
        return decode(str.getBytes(java.nio.charset.StandardCharsets.US_ASCII), flags);
    }

    public static byte[] decode(byte[] input, int flags) {
        String s = new String(input, java.nio.charset.StandardCharsets.US_ASCII).replaceAll("\\s", "");
        try {
            return ((flags & URL_SAFE) != 0 ? java.util.Base64.getUrlDecoder() : java.util.Base64.getDecoder()).decode(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("bad base-64", e);
        }
    }

    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        byte[] part = new byte[len];
        System.arraycopy(input, offset, part, 0, len);
        return decode(part, flags);
    }

    public static String encodeToString(byte[] input, int flags) {
        return new String(encode(input, flags), java.nio.charset.StandardCharsets.US_ASCII);
    }

    public static String encodeToString(byte[] input, int offset, int len, int flags) {
        byte[] part = new byte[len];
        System.arraycopy(input, offset, part, 0, len);
        return encodeToString(part, flags);
    }

    public static byte[] encode(byte[] input, int flags) {
        java.util.Base64.Encoder enc;
        if ((flags & URL_SAFE) != 0) enc = java.util.Base64.getUrlEncoder();
        else if ((flags & NO_WRAP) != 0) enc = java.util.Base64.getEncoder();
        else enc = java.util.Base64.getMimeEncoder(76, (flags & CRLF) != 0 ? new byte[]{'\r', '\n'} : new byte[]{'\n'});
        if ((flags & NO_PADDING) != 0) enc = enc.withoutPadding();
        byte[] out = enc.encode(input);
        if ((flags & NO_WRAP) == 0 && (flags & URL_SAFE) == 0) {
            // Android ends wrapped output with a newline.
            byte[] nl = (flags & CRLF) != 0 ? new byte[]{'\r', '\n'} : new byte[]{'\n'};
            byte[] withNl = new byte[out.length + nl.length];
            System.arraycopy(out, 0, withNl, 0, out.length);
            System.arraycopy(nl, 0, withNl, out.length, nl.length);
            return withNl;
        }
        return out;
    }
}
