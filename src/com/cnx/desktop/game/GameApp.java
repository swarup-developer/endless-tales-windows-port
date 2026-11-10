package com.cnx.desktop.game;

import android.os.MainThread;
import com.cnx.desktop.DesktopRuntime;
import com.cnx.desktop.game.screens.ExploreScreen;
import com.cnx.desktop.game.screens.Panels;
import com.cnx.endlesstalestwo.App;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;

/** The JavaFX application: window, fonts, styles, then boots the game data and shows the title. */
public final class GameApp extends Application {
    private static Stage stage;

    public static Stage stage() { return stage; }
    public static double musicVolume() { return FxAudio.musicVolume; }
    public static double soundVolume() { return FxAudio.soundVolume; }

    public static void setVolumes(double music, double sound) {
        FxAudio.musicVolume = music;
        FxAudio.soundVolume = sound;
        if (App.Shell != null && App.Shell.musicPlayer != null) {
            // re-apply the game's own volume; FxAudio multiplies it with the new master volume
            float v = App.Shell.getAccount() != null ? App.Shell.getAccount().musicVolume / 100f : 1f;
            try { App.Shell.musicPlayer.setVolume(v, v); } catch (Throwable ignored) {}
        }
    }

    /** Switches the language of resource strings and game texts. */
    public static void applyLanguage(String code) {
        Game.setLanguage(code);
        Screens.refreshCurrent();
    }

    @Override
    public void start(Stage primary) {
        stage = primary;
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Log.error("Uncaught exception in " + t.getName(), e));
        Settings.load();
        // test switches: -Det2.speechmode=OFF (log only), -Det2.mute=true
        if (System.getProperty("et2.speechmode") != null) Speech.mode = Speech.Mode.valueOf(System.getProperty("et2.speechmode"));
        if (Boolean.getBoolean("et2.mute")) { FxAudio.musicVolume = 0; FxAudio.soundVolume = 0; }
        MainThread.scheduler = new FxScheduler();
        android.media.MediaPlayer.backend = new FxAudio();
        loadFonts();

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");
        Scene scene = new Scene(root, 1280, 720);
        var css = GameApp.class.getResource("game.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        new Ui(scene, root);
        Ui.get().nav.defaultEscape = Keys::defaultEscape;
        Keys.install(scene, primary);
        String speechLog = System.getProperty("et2.speechlog");
        Speech.init(Launcher.nativeDir(), speechLog == null ? null : Path.of(speechLog));
        Speech.say("Endless Tales 2. Loading. Press F1 at any time for keyboard help.", true);

        primary.setTitle("Endless Tales 2");
        Image icon = Images.drawable("game_logo_512");
        if (icon != null) primary.getIcons().add(icon);
        primary.setScene(scene);
        primary.setMinWidth(960);
        primary.setMinHeight(560);
        // only the game's own F11 handler toggles full screen (JavaFX's exit key would undo it at once)
        primary.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primary.fullScreenProperty().addListener((o, was, now) -> { if (Settings.fullscreen != now) { Settings.fullscreen = now; Settings.save(); } });
        primary.setFullScreenExitHint("");
        primary.setFullScreen(Settings.fullscreen);
        primary.setOnCloseRequest(e -> {
            if (App.Shell != null && App.Shell.character != null) Game.save();
        });
        // back in the window (Alt+Tab): after NVDA names the window, say where you are
        primary.focusedProperty().addListener((o, was, now) -> {
            if (!now || Ui.get() == null) return;
            PauseTransition back = new PauseTransition(Duration.millis(400));
            back.setOnFinished(ev -> Ui.get().nav.announceCurrent());
            back.play();
        });
        primary.show();
        root.requestFocus();

        Ui.get().setBackground(Images.drawable("game_bg"), null);
        Ui.get().toast("Loading game data…", false);
        PauseTransition p = new PauseTransition(Duration.millis(150));
        p.setOnFinished(e -> {
            try {
                Game.boot(Settings.language);
                Screens.title();
                AutoTest.startIfRequested();
            } catch (Throwable t) {
                Log.error("Could not start the game", t);
                Ui.get().message("Could not start", "The game data could not be loaded:\n" + t
                        + "\n\nDetails are in " + DesktopRuntime.dataDir.resolve("game.log"), null, Platform::exit);
            }
        });
        p.play();
    }

    private static void loadFonts() {
        Path fonts = DesktopRuntime.resDir.resolve("font");
        for (String f : new String[]{"merienda_regular.ttf", "merienda_bold.ttf", "vecna.ttf", "vecna_bold.ttf", "glass_antiqua.ttf", "texturina.ttf"}) {
            Path p = fonts.resolve(f);
            if (Files.exists(p)) {
                Font font = null;
                try (var in = Files.newInputStream(p)) { font = Font.loadFont(in, 16); }
                catch (Exception e) { Log.warn("Font " + f + ": " + e); }
                Log.info("Font " + f + " -> " + (font == null ? "NOT LOADED" : "family '" + font.getFamily() + "'"));
            }
        }
    }

    @Override
    public void stop() {
        Speech.shutdown();
        if (App.Shell != null && App.Shell.character != null) Game.save();
    }
}
