package com.cnx.desktop;

import java.io.File;
import java.nio.file.Path;

/**
 * Desktop-only settings shared by the Android stand-in classes (Context, Resources, AssetManager, ...).
 * The launcher fills these in before the game logic is touched.
 */
public final class DesktopRuntime {
    /** Folder holding the decoded apktool "res" directory (values/strings.xml, values/public.xml ...). */
    public static volatile Path resDir = Path.of("res");
    /** Folder holding the APK's "assets" directory (images, audio ...). */
    public static volatile Path assetsDir = Path.of("assets");
    /** Folder where saves and preferences go. */
    public static volatile Path dataDir = Path.of(System.getProperty("user.home"), "EndlessTales2");
    /** "", "es" or "pt" — which values-XX folder to read strings from ("" = English). */
    public static volatile String language = "";
    /** Package name the game asks for in getIdentifier(). */
    public static final String PACKAGE = "com.cnx.endlesstalestwo";

    /** Callbacks into the desktop UI. All optional. */
    public interface Host {
        default void toast(String message, boolean error) {}
        default void log(String level, String tag, String message) {}
    }

    public static volatile Host host = new Host() {
        @Override public void log(String level, String tag, String message) {
            if (!"D".equals(level) && !"V".equals(level)) System.err.println(level + "/" + tag + ": " + message);
        }
    };

    private DesktopRuntime() {}

    public static File filesDir() {
        File f = dataDir.resolve("files").toFile();
        f.mkdirs();
        return f;
    }
}
