package com.cnx.desktop.game;

import com.cnx.desktop.DesktopRuntime;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Desktop options, stored in settings.properties in the save folder. */
public final class Settings {
    /** Milliseconds per typewriter step (0 = show text instantly). */
    public static volatile int textSpeed = 18;
    public static volatile boolean fullscreen = false;
    public static volatile String language = "";
    /** Battle critical hits: press Space quickly (true) or a fixed result like the Android accessibility mode. */
    public static volatile boolean criticalMinigame = true;

    private Settings() {}

    private static Path file() { return DesktopRuntime.dataDir.resolve("settings.properties"); }

    public static void load() {
        Properties p = new Properties();
        if (Files.exists(file())) {
            try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) { p.load(r); } catch (Exception ignored) {}
        }
        textSpeed = parse(p.getProperty("textSpeed"), 18);
        fullscreen = Boolean.parseBoolean(p.getProperty("fullscreen", "false"));
        language = p.getProperty("language", "");
        criticalMinigame = Boolean.parseBoolean(p.getProperty("criticalMinigame", "true"));
        FxAudio.musicVolume = parse(p.getProperty("musicVolume"), 60) / 100.0;
        FxAudio.soundVolume = parse(p.getProperty("soundVolume"), 80) / 100.0;
        try { Speech.mode = Speech.Mode.valueOf(p.getProperty("speech", "AUTO")); } catch (Exception e) { Speech.mode = Speech.Mode.AUTO; }
        Speech.voiceRate = parse(p.getProperty("voiceRate"), 2);
        Nav.announcePositions = Boolean.parseBoolean(p.getProperty("announcePositions", "true"));
        Nav.menuSounds = Boolean.parseBoolean(p.getProperty("menuSounds", "true"));
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("textSpeed", String.valueOf(textSpeed));
        p.setProperty("fullscreen", String.valueOf(fullscreen));
        p.setProperty("language", language);
        p.setProperty("criticalMinigame", String.valueOf(criticalMinigame));
        p.setProperty("musicVolume", String.valueOf(Math.round(FxAudio.musicVolume * 100)));
        p.setProperty("soundVolume", String.valueOf(Math.round(FxAudio.soundVolume * 100)));
        p.setProperty("speech", Speech.mode.name());
        p.setProperty("voiceRate", String.valueOf(Speech.voiceRate));
        p.setProperty("announcePositions", String.valueOf(Nav.announcePositions));
        p.setProperty("menuSounds", String.valueOf(Nav.menuSounds));
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) { p.store(w, "Endless Tales 2 desktop settings"); }
        } catch (Exception e) {
            Log.warn("Cannot save settings: " + e);
        }
    }

    private static int parse(String s, int d) {
        try { return s == null ? d : Integer.parseInt(s.trim()); } catch (Exception e) { return d; }
    }
}
