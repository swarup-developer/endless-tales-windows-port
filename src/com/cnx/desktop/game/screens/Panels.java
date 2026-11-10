package com.cnx.desktop.game.screens;

import com.cnx.desktop.game.*;
import com.cnx.desktop.game.Nav;
import com.cnx.desktop.game.Speech;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.entities.ItemReference;
import com.cnx.endlesstalestwo.entities.Quest;
import com.cnx.endlesstalestwo.entities.Recompense;
import com.cnx.endlesstalestwo.entities.Item;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/** Pop-up panels: rewards, quests, level up, death, the game menu and options. */
public final class Panels {
    private Panels() {}

    private static String lang(com.cnx.cnxgameengine.entities.Base b) {
        try { return b.getName(App.CURRENT_LANGUAGE); } catch (Throwable t) { return ""; }
    }

    /** What a Recompense gives: gold, experience, items (RecompenseDisplay). */
    static VBox recompenseBody(Recompense r) {
        VBox body = new VBox(8);
        if (r == null) return body;
        String desc = "";
        try { desc = r.getDescription(App.CURRENT_LANGUAGE); } catch (Throwable ignored) {}
        if (desc != null && !desc.isBlank()) body.getChildren().add(Ui.para(desc.replace("[PLAYERNAME]", App.getPlayerChar().getName())));
        if (r.gold != 0) body.getChildren().add(Ui.statRow("icon_coins", Game.t("Gold").equals("Gold") ? Tr.t("Gold") : Game.t("Gold"), (r.gold > 0 ? "+" : "") + r.gold));
        if (r.experience != 0) body.getChildren().add(Ui.statRow("icon_experience", Game.t("Experience").equals("Experience") ? Tr.t("Experience") : Game.t("Experience"), (r.experience > 0 ? "+" : "") + r.experience));
        if (r.gems != 0) body.getChildren().add(Ui.statRow("icon_gems", Tr.t("Gems"), "+" + r.gems));
        List<ItemReference> items = r.rewardedItems != null && !r.rewardedItems.isEmpty() ? r.rewardedItems : r.items;
        if (items != null) {
            for (ItemReference ref : items) {
                if (ref == null || ref.itemId == null) continue;
                Item item = App.DataManager.getItem(ref.itemId);
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                ImageView iv = new ImageView(item == null ? (javafx.scene.image.Image) null : Images.asset("items", item.getImageFile()));
                iv.setFitWidth(40);
                iv.setFitHeight(40);
                iv.setPreserveRatio(true);
                Label name = new Label((item == null ? ref.itemId.name() : lang(item)) + (ref.amount > 1 ? "  x" + ref.amount : ""));
                name.getStyleClass().add("stat-label");
                row.getChildren().addAll(iv, name);
                body.getChildren().add(row);
            }
        }
        if (body.getChildren().isEmpty()) body.getChildren().add(Ui.para("…"));
        return body;
    }

    public static void recompense(String title, Recompense r, Runnable onClose) {
        Sound.play("gold_trade");
        final Runnable[] close = new Runnable[1];
        Button ok = Ui.button("OK", () -> close[0].run());
        ok.setDefaultButton(true);
        close[0] = Ui.get().showModal(Ui.panel(title, recompenseBody(r), 520, ok), false, () -> {
            Ui.get().refreshHud();
            if (onClose != null) onClose.run();
        });
    }

    public static void questCompleted(Quest q) {
        Sound.play("quest_completed");
        VBox body = new VBox(10);
        body.getChildren().add(Ui.para(q.getDescription(App.CURRENT_LANGUAGE)));
        if (q.recompense != null && !q.secretRecompense) body.getChildren().add(recompenseBody(q.recompense));
        final Runnable[] close = new Runnable[1];
        close[0] = Ui.get().showModal(Ui.panel((Game.t("questCompleted").equals("questCompleted") ? Tr.t("Quest completed") : Game.t("questCompleted")) + ": " + lang(q),
                body, 580, Ui.button("OK", () -> close[0].run())), false, () -> Ui.get().refreshHud());
    }

    public static void newQuest(Quest q) {
        Sound.play("new_quest");
        VBox body = new VBox(10, Ui.para(q.getDescription(App.CURRENT_LANGUAGE)));
        try {
            if (q.getCurrentQuestPart() != null) body.getChildren().add(Ui.para("→ " + q.getCurrentQuestPart().getDescription(App.CURRENT_LANGUAGE)));
        } catch (Throwable ignored) {}
        final Runnable[] close = new Runnable[1];
        close[0] = Ui.get().showModal(Ui.panel((Game.t("NewQuest").equals("NewQuest") ? Tr.t("New quest") : Game.t("NewQuest")) + ": " + lang(q),
                body, 580, Ui.button("OK", () -> close[0].run())), false, null);
    }

    public static void levelUp(Character c, boolean newSkills) {
        Sound.play("level_up");
        VBox body = new VBox(8);
        body.getChildren().add(Ui.statRow("icon_star_color", Game.t("Level"), String.valueOf(c.level)));
        body.getChildren().add(Ui.statRow("icon_hp_color", "HP", String.valueOf(c.attributesManager.getMaxHp())));
        body.getChildren().add(Ui.statRow("icon_mp_color", "MP", String.valueOf(c.attributesManager.getMaxMp())));
        body.getChildren().add(Ui.statRow("icon_skills", Game.t("SkillPoints").equals("SkillPoints") ? Tr.t("Skill points") : Game.t("SkillPoints"), String.valueOf(c.skillPoints)));
        if (newSkills) body.getChildren().add(Ui.para(Game.t("newSkillsLearned").equals("newSkillsLearned") ? Tr.t("You learned new skills!") : Game.t("newSkillsLearned")));
        final Runnable[] close = new Runnable[1];
        close[0] = Ui.get().showModal(Ui.panel(Game.t("LevelUp").equals("LevelUp") ? Tr.t("Level up!") : Game.t("LevelUp"), body, 460,
                Ui.button("OK", () -> close[0].run())), false, () -> Ui.get().refreshHud());
    }

    public static void death(int lostXp, int lostGold, Runnable onOk) {
        Sound.play("player_dead");
        VBox body = new VBox(8,
                Ui.para(Game.t("youDied").equals("youDied") ? Tr.t("You have been defeated. You wake up at your residence.") : Game.t("youDied")),
                Ui.statRow("icon_experience", "XP", "-" + lostXp),
                Ui.statRow("icon_coins", Tr.t("Gold"), "-" + lostGold));
        final Runnable[] close = new Runnable[1];
        close[0] = Ui.get().showModal(Ui.panel(Game.t("Defeated").equals("Defeated") ? Tr.t("Defeated") : Game.t("Defeated"), body, 480,
                Ui.button("OK", () -> close[0].run())), false, onOk);
    }

    /** Esc / Menu: save, options, back to title, quit. */
    public static void gameMenu() { gameMenu(0); }

    /** The game menu with the keyboard on item {@code focus} (0 = Resume). */
    static void gameMenu(int focus) {
        final Runnable[] close = new Runnable[1];
        VBox body = new VBox(10);
        body.setAlignment(Pos.CENTER);
        Button resume = Ui.button(Tr.t("Resume"), () -> close[0].run());
        Button save = Ui.button(Tr.t("Save game"), () -> { Game.save(); Ui.get().toast(Tr.t("Game saved"), false); close[0].run(); });
        Button opts = Ui.button(Tr.t("Options"), () -> { close[0].run(); options(() -> gameMenu(2)); });
        Button help = Ui.button(Tr.t("Keyboard help"), () -> { close[0].run(); com.cnx.desktop.game.Screens.help(); });
        Button title = Ui.button(Tr.t("Save and return to title"), () -> { close[0].run(); Game.save(); Screens.title(); });
        Button quit = Ui.button(Tr.t("Save and quit"), () -> { Game.save(); javafx.application.Platform.exit(); });
        for (Button b : new Button[]{resume, save, opts, help, title, quit}) { b.setMaxWidth(Double.MAX_VALUE); body.getChildren().add(b); }
        if (focus > 0) Speech.hold(true); // say only the item you land on
        try { close[0] = Ui.get().showModal(Ui.panel(Tr.t("Menu"), body, 380), true, null); } finally { Speech.hold(false); }
        if (focus > 0) Ui.get().nav.focusModal(focus, true);
    }

    private static final String[] SPEECH_NAMES = {
            "Automatic: NVDA, otherwise the Windows voice", "Screen reader only", "Windows voice", "Off"};

    /** Options. Every change applies (and is saved) at once, so you hear it while adjusting. */
    public static void options(Runnable after) { options(after, -1); }

    private static void options(Runnable after, int focus) {
        final Runnable[] close = new Runnable[1];
        final Runnable[] reopenOnLanguage = new Runnable[1];
        VBox body = new VBox(12);
        ComboBox<String> speech = new ComboBox<>();
        for (String n : SPEECH_NAMES) speech.getItems().add(Tr.t(n));
        speech.getSelectionModel().select(Speech.mode.ordinal());
        speech.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> {
            if (b.intValue() < 0) return;
            Speech.Mode m = Speech.Mode.values()[b.intValue()];
            // say it while speech still works, so turning speech off is never silent
            if (m == Speech.Mode.OFF) Speech.say(Tr.t("Speech output, Off. Press Left arrow to turn speech back on."), true);
            Speech.mode = m;
            Settings.save();
        });
        Slider rate = new Slider(-10, 10, Speech.voiceRate);
        rate.valueProperty().addListener((o, a, b) -> { Speech.voiceRate = (int) Math.round(b.doubleValue()); Settings.save(); });
        Nav.format(rate, v -> {
            int r = (int) Math.round(v);
            return r == 0 ? Tr.t("normal") : (r > 0 ? Tr.f("faster, plus %d", r) : Tr.f("slower, minus %d", -r));
        }, 1);
        CheckBox positions = new CheckBox(Tr.t("Say position in lists, like 2 of 5"));
        positions.setSelected(Nav.announcePositions);
        positions.selectedProperty().addListener((o, a, b) -> { Nav.announcePositions = b; Settings.save(); });
        CheckBox sounds = new CheckBox(Tr.t("Sound when moving through lists"));
        sounds.setSelected(Nav.menuSounds);
        sounds.selectedProperty().addListener((o, a, b) -> { Nav.menuSounds = b; Settings.save(); });

        CheckBox critical = new CheckBox(Tr.t("Critical hits in battle: press Space quickly (off: automatic)"));
        critical.setSelected(Settings.criticalMinigame);
        critical.selectedProperty().addListener((o, a, b) -> { Settings.criticalMinigame = b; Settings.save(); });
        Slider music = new Slider(0, 100, Math.round(GameApp.musicVolume() * 100));
        Slider sound = new Slider(0, 100, Math.round(GameApp.soundVolume() * 100));
        music.valueProperty().addListener((o, a, b) -> { GameApp.setVolumes(b.doubleValue() / 100, GameApp.soundVolume()); Settings.save(); });
        sound.valueProperty().addListener((o, a, b) -> { GameApp.setVolumes(GameApp.musicVolume(), b.doubleValue() / 100); Settings.save(); Sound.click(); });
        Nav.format(music, v -> Math.round(v) == 0 ? Tr.t("off") : Tr.f("%d percent", Math.round(v)), 5);
        Nav.format(sound, v -> Math.round(v) == 0 ? Tr.t("off") : Tr.f("%d percent", Math.round(v)), 5);
        Slider text = new Slider(0, 60, 60 - Settings.textSpeed);
        text.valueProperty().addListener((o, a, b) -> { Settings.textSpeed = (int) Math.round(60 - b.doubleValue()); Settings.save(); });
        Nav.format(text, v -> Math.round(v) >= 60 ? Tr.t("instant") : Tr.f("%d percent", Math.round(v * 100 / 60.0)), 6);
        CheckBox full = new CheckBox(Tr.t("Full screen"));
        full.setSelected(GameApp.stage() != null ? GameApp.stage().isFullScreen() : Settings.fullscreen);
        full.selectedProperty().addListener((o, a, b) -> {
            Stage stage = GameApp.stage();
            if (stage != null && stage.isFullScreen() != b) stage.setFullScreen(b);
            Settings.fullscreen = b;
            Settings.save();
        });
        // F11 while the options are open keeps the check box right
        javafx.beans.value.ChangeListener<Boolean> fsSync = (o, a, b) -> full.setSelected(b);
        if (GameApp.stage() != null) GameApp.stage().fullScreenProperty().addListener(fsSync);
        ComboBox<String> language = new ComboBox<>();
        language.getItems().addAll("English", "Español", "Português");
        language.getSelectionModel().select(switch (Settings.language) { case "es" -> 1; case "pt" -> 2; default -> 0; });
        language.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> {
            if (b.intValue() < 0) return;
            String lang = switch (b.intValue()) { case 1 -> "es"; case 2 -> "pt"; default -> ""; };
            if (lang.equals(Settings.language)) return;
            Settings.language = lang;
            Settings.save();
            reopenOnLanguage[0].run();
        });
        body.getChildren().addAll(
                Ui.section(Tr.t("Speech")),
                labeled(Tr.t("Speech output"), speech), labeled(Tr.t("Windows voice speed"), rate), positions, sounds,
                Ui.section(Tr.t("Battle")), critical,
                Ui.section(Tr.t("Sound")),
                labeled(Tr.t("Music volume"), music), labeled(Tr.t("Sound effects volume"), sound),
                Ui.section(Tr.t("Display and language")),
                labeled(Tr.t("Text speed on screen"), text), full, labeled(Tr.t("Language"), language));
        for (javafx.scene.Node n : new javafx.scene.Node[]{speech, rate, positions, sounds, critical, music, sound, text, full, language}) n.setFocusTraversable(false);
        Button done = Ui.button(Tr.t("Done"), () -> close[0].run());
        Runnable afterClose = () -> {
            if (GameApp.stage() != null) GameApp.stage().fullScreenProperty().removeListener(fsSync);
            if (after != null) after.run();
        };
        if (focus >= 0) Speech.hold(true); // reopened after a language change: say only where you land
        try { close[0] = Ui.get().showModal(Ui.panel(Tr.t("Options"), body, 560, done), false, afterClose); } finally { Speech.hold(false); }
        reopenOnLanguage[0] = () -> javafx.application.Platform.runLater(() -> {
            // the new language rebuilds the screen behind; open the options again on top, still on Language
            int at = Ui.get().nav.modalIndex();
            if (GameApp.stage() != null) GameApp.stage().fullScreenProperty().removeListener(fsSync);
            Speech.hold(true); // rebuilding the screens behind: say only where you land
            try {
                Ui.get().closeAllModals();
                Game.setLanguage(Settings.language);
                if (Screens.current() instanceof TitleScreen) { Screens.title(); Ui.get().nav.focusModalText(Tr.t("Options")); }
                else Screens.refreshCurrent();
            } finally { Speech.hold(false); }
            options(after, at);
        });
        if (focus >= 0) Ui.get().nav.focusModal(focus, true);
    }

    /** A setting with its name above it; the name is spoken together with the value. */
    private static VBox labeled(String label, javafx.scene.control.Control n) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Nav.labelFor(l);
        Nav.label(n, label);
        return new VBox(4, l, n);
    }

}
