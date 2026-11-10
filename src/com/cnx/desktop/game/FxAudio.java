package com.cnx.desktop.game;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

/** Plays the game's mp3 files through JavaFX Media (backend for the desktop android.media.MediaPlayer). */
final class FxAudio implements android.media.MediaPlayer.Backend {
    /** Master volumes, 0..1, set from the options screen. */
    static volatile double musicVolume = 0.6, soundVolume = 0.8;

    @Override
    public android.media.MediaPlayer.Track open(File file) {
        try {
            return new FxTrack(new MediaPlayer(new Media(file.toURI().toString())), isMusic(file));
        } catch (Exception e) {
            Log.warn("Cannot play " + file + ": " + e);
            return null;
        }
    }

    /** Looping location themes are music; everything else is a sound effect. */
    private static boolean isMusic(File f) {
        String n = f.getName();
        return n.contains("music") || n.contains("theme") || n.contains("medieval") || n.contains("ambient")
                || n.startsWith("battle_") || n.startsWith("plains") || n.startsWith("tavern") || n.equals("inn_house.mp3")
                || n.equals("challenge.mp3");
    }

    private static void fx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    private static final class FxTrack implements android.media.MediaPlayer.Track {
        private final MediaPlayer p;
        private final boolean music;
        private double volume = 1;
        private volatile boolean playing;
        private Runnable onEnd;

        FxTrack(MediaPlayer p, boolean music) {
            this.p = p;
            this.music = music;
            applyVolume();
            p.setOnEndOfMedia(() -> {
                if (p.getCycleCount() != MediaPlayer.INDEFINITE) {
                    playing = false;
                    Runnable r = onEnd;
                    if (r != null) r.run();
                }
            });
            p.setOnError(() -> Log.warn("Media error: " + p.getError()));
        }

        private void applyVolume() { p.setVolume(volume * (music ? musicVolume : soundVolume)); }

        @Override public void play() { playing = true; fx(() -> { applyVolume(); p.play(); }); }
        @Override public void pause() { playing = false; fx(p::pause); }
        @Override public void stop() { playing = false; fx(p::stop); }
        @Override public void dispose() { playing = false; fx(p::dispose); }
        @Override public void setVolume(float v) { volume = v; fx(this::applyVolume); }
        @Override public void setLooping(boolean l) { fx(() -> p.setCycleCount(l ? MediaPlayer.INDEFINITE : 1)); }
        @Override public boolean isPlaying() { return playing; }
        @Override public int positionMillis() { return (int) p.getCurrentTime().toMillis(); }
        @Override public int durationMillis() { return (int) p.getTotalDuration().toMillis(); }
        @Override public void setOnEnd(Runnable r) { onEnd = r; }
    }
}
