package com.cnx.desktop.game;

import android.os.MainThread;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

/** Runs the game's Handler/Looper work on the JavaFX application thread. */
final class FxScheduler implements MainThread.Scheduler {
    @Override
    public void post(Runnable r, long delayMillis) {
        Runnable safe = () -> {
            try { r.run(); }
            catch (Throwable t) { Log.error("Error in scheduled game task", t); }
        };
        if (delayMillis <= 0) {
            Platform.runLater(safe);
        } else if (Platform.isFxApplicationThread()) {
            schedule(safe, delayMillis);
        } else {
            Platform.runLater(() -> schedule(safe, delayMillis));
        }
    }

    private static void schedule(Runnable r, long delayMillis) {
        PauseTransition p = new PauseTransition(Duration.millis(delayMillis));
        p.setOnFinished(e -> r.run());
        p.play();
    }

    @Override
    public boolean isMainThread() { return Platform.isFxApplicationThread(); }
}
