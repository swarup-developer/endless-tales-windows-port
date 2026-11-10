package com.cnx.desktop.game;

import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.managers.AssetsManager;

/** Short UI sounds, played through the game's own sound player. */
public final class Sound {
    private static long lastClick;

    private Sound() {}

    public static void play(String file) {
        try {
            if (App.Shell != null && Game.activity != null) {
                App.Shell.playSound(AssetsManager.getSoundAsPlayableFile(file, Game.activity), Game.activity);
            }
        } catch (Throwable t) {
            Log.warn("Sound failed " + file + ": " + t);
        }
    }

    /** Soft tick when moving between menu items. */
    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastClick < 60) return;
        lastClick = now;
        play("button_click_3");
    }

    public static void click() {
        long now = System.currentTimeMillis();
        if (now - lastClick < 80) return;
        lastClick = now;
        play("button_click_1");
    }

    /** Starts a looping music track (stops the location music the game was playing). */
    public static void music(String file) {
        try {
            if (App.Shell != null && Game.activity != null) {
                App.Shell.playMusic(AssetsManager.getSoundAsPlayableFile(file, Game.activity), Game.activity);
            }
        } catch (Throwable t) {
            Log.warn("Music failed " + file + ": " + t);
        }
    }
}
