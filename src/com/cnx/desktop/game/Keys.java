package com.cnx.desktop.game;

import com.cnx.desktop.game.screens.CharacterPanel;
import com.cnx.desktop.game.screens.Panels;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.enums.ExperienceTable;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Every key in the game goes through here (the screen reader only hears what the game speaks). */
final class Keys {
    static final String HELP = String.join("\n",
            "Up and Down arrows, or Tab: move through the choices.",
            "Enter or Space: choose.",
            "Escape or Backspace: go back, or open the game menu.",
            "Left and Right arrows: change a setting.",
            "Home and End: first and last choice. Page Up and Page Down: jump between sections.",
            "Shift plus a letter: jump to the next choice starting with that letter.",
            "R: repeat the current text.",
            "S: your status: health, mana, gold, level, day, time and place.",
            "C: character sheet with stats, quests and inventory.",
            "I: inventory, to use, equip or discard items.",
            "Control: stop speech.",
            "F1: this help.",
            "F11: full screen.");

    private Keys() {}

    static void install(Scene scene, Stage stage) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            try {
                if (handle(e, stage)) e.consume();
            } catch (Throwable t) {
                Log.error("Key handling failed", t);
            }
        });
    }

    private static boolean handle(KeyEvent e, Stage stage) {
        Ui ui = Ui.get();
        if (ui == null) return false;
        KeyCode k = e.getCode();
        if (System.getProperty("et2.autotest") != null && !AutoTest.firing) return true; // tests ignore the real keyboard
        if (k == KeyCode.F11) {
            Settings.fullscreen = !stage.isFullScreen();
            stage.setFullScreen(Settings.fullscreen);
            Settings.save();
            Speech.say(Tr.t(Settings.fullscreen ? "Full screen" : "Window"), true);
            return true;
        }
        if (ui.nav.editing()) return ui.nav.handle(e);
        if (k == KeyCode.CONTROL) { Speech.stop(); return true; }
        if (k == KeyCode.F1) { showHelp(); return true; }
        boolean plain = !e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && !e.isMetaDown();
        if (plain && k == KeyCode.R) { if (!ui.nav.repeatPopup()) ui.repeatScene(); return true; }
        if (plain && k == KeyCode.S) { sayStatus(); return true; }
        if (plain && k == KeyCode.C && inGame() && !ui.hasModal()) { CharacterPanel.show(); return true; }
        if (plain && k == KeyCode.I && inGame() && !ui.hasModal() && !(Screens.current() instanceof com.cnx.desktop.game.screens.BattleScreen)) { Game.activity.openInventory(); return true; }
        if ((k == KeyCode.ENTER || k == KeyCode.SPACE) && ui.finishTextIfTyping()) return true;
        // while the game is working (travel, an action, a script), only listening keys work
        if (ui.isBusy()) {
            if (k == KeyCode.ENTER || k == KeyCode.SPACE || k == KeyCode.ESCAPE || k == KeyCode.BACK_SPACE) { Speech.say(Tr.t("Please wait"), true); return true; }
            return false;
        }
        return ui.nav.handle(e);
    }

    private static boolean inGame() { return App.Shell != null && App.Shell.character != null; }

    static void showHelp() {
        VBox body = new VBox(6);
        for (String line : HELP.split("\n")) body.getChildren().add(Ui.para(Tr.t(line)));
        final Runnable[] close = new Runnable[1];
        close[0] = Ui.get().showModal(Ui.panel(Tr.t("Keyboard help"), body, 640, Ui.button(Tr.t("Close"), () -> close[0].run())), true, null);
    }

    /** S key: everything the HUD shows, as one sentence. */
    static void sayStatus() {
        if (Screens.current() != null && Screens.current().sayStatus()) return;
        if (!inGame()) { Speech.say(Tr.t("No game in progress"), true); return; }
        Character c = App.getPlayerChar();
        var a = c.attributesManager;
        int next = ExperienceTable.get(c.level + 1);
        StringBuilder sb = new StringBuilder();
        sb.append(c.getName()).append(", ").append(c.getCharClassName()).append(", ").append(Tr.t("level")).append(" ").append(c.level).append(". ");
        sb.append(Tr.f("Health %d of %d.", a.getCurrent(Enums.AttributeName.HP), a.getMaxHp())).append(" ");
        sb.append(Tr.f("Mana %d of %d.", a.getCurrent(Enums.AttributeName.MP), a.getMaxMp())).append(" ");
        sb.append(Tr.f("Experience %d", c.experience));
        if (next > 0) sb.append(Tr.f(" of %d for the next level", next));
        sb.append(". ").append(Tr.f("Gold %d.", c.gold)).append(" ");
        sb.append(Tr.f("Day %d, %s", App.Shell.flowManager.days, App.Shell.flowManager.getHoursForDisplay()))
                .append(Tr.t(App.Shell.flowManager.isDayLight ? ", daytime." : ", night.")).append(" ");
        if (App.Shell.flowManager.currentLocation != null) sb.append(Tr.f("At %s.", App.Shell.flowManager.currentLocation.getName(App.CURRENT_LANGUAGE))).append(" ");
        if (c.isHungry) sb.append(Tr.t("You are hungry.")).append(" ");
        if (c.isPoisoned) sb.append(Tr.t("Poisoned.")).append(" ");
        if (c.isBleeding) sb.append(Tr.t("Bleeding.")).append(" ");
        if (c.isBurdened) sb.append(Tr.t("Carrying too much.")).append(" ");
        Speech.say(sb.toString(), true);
    }

    /** Escape on a screen without its own back action. */
    static void defaultEscape() {
        if (inGame()) Panels.gameMenu();
    }
}
