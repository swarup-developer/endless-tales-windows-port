package android.util;

import com.cnx.desktop.DesktopRuntime;

/** Desktop Log: forwards to {@link DesktopRuntime#host}. */
public final class Log {
    public static final int VERBOSE = 2, DEBUG = 3, INFO = 4, WARN = 5, ERROR = 6, ASSERT = 7;

    private Log() {}

    private static int out(String level, String tag, String msg, Throwable tr) {
        DesktopRuntime.host.log(level, tag, tr == null ? msg : msg + "\n" + getStackTraceString(tr));
        return 0;
    }

    public static int v(String tag, String msg) { return out("V", tag, msg, null); }
    public static int v(String tag, String msg, Throwable tr) { return out("V", tag, msg, tr); }
    public static int d(String tag, String msg) { return out("D", tag, msg, null); }
    public static int d(String tag, String msg, Throwable tr) { return out("D", tag, msg, tr); }
    public static int i(String tag, String msg) { return out("I", tag, msg, null); }
    public static int i(String tag, String msg, Throwable tr) { return out("I", tag, msg, tr); }
    public static int w(String tag, String msg) { return out("W", tag, msg, null); }
    public static int w(String tag, String msg, Throwable tr) { return out("W", tag, msg, tr); }
    public static int w(String tag, Throwable tr) { return out("W", tag, "", tr); }
    public static int e(String tag, String msg) { return out("E", tag, msg, null); }
    public static int e(String tag, String msg, Throwable tr) { return out("E", tag, msg, tr); }
    public static int wtf(String tag, String msg) { return out("E", tag, msg, null); }
    public static int wtf(String tag, String msg, Throwable tr) { return out("E", tag, msg, tr); }
    public static boolean isLoggable(String tag, int level) { return level >= INFO; }
    public static int println(int priority, String tag, String msg) { return out(String.valueOf(priority), tag, msg, null); }

    public static String getStackTraceString(Throwable tr) {
        if (tr == null) return "";
        java.io.StringWriter sw = new java.io.StringWriter();
        tr.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
