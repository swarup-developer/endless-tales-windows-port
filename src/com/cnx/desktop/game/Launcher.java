package com.cnx.desktop.game;

import com.cnx.desktop.DesktopRuntime;
import javafx.application.Application;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point. Finds the game data (decoded res/ and assets/), sets up the Android stand-ins and
 * starts the JavaFX window.
 *
 * Game data folder, first match wins:
 *   1. -Det2.data=<folder>
 *   2. <folder of the running jar>/../gamedata   (installed / packaged layout)
 *   3. <folder of the running jar>/gamedata
 *   4. ./gamedata
 * The folder must contain "res" and "assets".
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        Path data = findGameData();
        if (data == null) {
            System.err.println("Game data not found. Set -Det2.data=<folder containing res and assets>.");
            System.exit(1);
        }
        DesktopRuntime.resDir = data.resolve("res");
        DesktopRuntime.assetsDir = data.resolve("assets");

        String appData = System.getenv("APPDATA");
        Path saves = appData != null ? Path.of(appData, "EndlessTales2") : Path.of(System.getProperty("user.home"), ".endlesstales2");
        Path portable = portableSaves();
        if (portable != null) saves = portable;
        if (System.getProperty("et2.saves") != null) saves = Path.of(System.getProperty("et2.saves"));
        DesktopRuntime.dataDir = saves;
        try { Files.createDirectories(saves); } catch (Exception ignored) {}

        Log.init(saves.resolve("game.log"));
        Log.info("Game data: " + data + "   Saves: " + saves);
        Application.launch(GameApp.class, args);
    }

    /** Folder with nvdaControllerClient.dll: -Det2.native, else <jar folder>/../native or <jar folder>/native. */
    static Path nativeDir() {
        String prop = System.getProperty("et2.native");
        if (prop != null) return Path.of(prop);
        try {
            File f = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path jarDir = (f.isFile() ? f.getParentFile() : f).toPath();
            Path p = jarDir.resolve("../native").normalize();
            return Files.isDirectory(p) ? p : jarDir.resolve("native");
        } catch (Exception e) {
            return Path.of("native");
        }
    }

    /**
     * Portable copy: a file named portable.txt next to EndlessTales2.exe keeps the saves in a "saves"
     * folder beside it (for a USB stick) instead of %APPDATA%\EndlessTales2.
     */
    private static Path portableSaves() {
        try {
            File f = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path jarDir = (f.isFile() ? f.getParentFile() : f).toPath();   // <game>\app
            Path gameDir = jarDir.getParent();
            if (gameDir != null && Files.isRegularFile(gameDir.resolve("portable.txt"))) return gameDir.resolve("saves");
        } catch (Exception ignored) {}
        return null;
    }

    static Path findGameData() {
        String prop = System.getProperty("et2.data");
        if (prop != null) return valid(Path.of(prop));
        Path jarDir = null;
        try {
            File f = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            jarDir = (f.isFile() ? f.getParentFile() : f).toPath();
        } catch (URISyntaxException | SecurityException ignored) {}
        if (jarDir != null) {
            Path p = valid(jarDir.resolve("../gamedata").normalize());
            if (p != null) return p;
            p = valid(jarDir.resolve("gamedata"));
            if (p != null) return p;
        }
        return valid(Path.of("gamedata").toAbsolutePath());
    }

    private static Path valid(Path p) {
        return Files.isDirectory(p.resolve("res")) && Files.isDirectory(p.resolve("assets")) ? p : null;
    }
}
