package com.cnx.desktop.game;

import com.cnx.endlesstalestwo.App;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-test (-Det2.autotest=<folder>): plays a short session using ONLY the keyboard, the way a
 * blind player would, saves a screenshot at each stage and exits. Run with -Det2.speechlog=<file>
 * to get everything the game spoke.
 */
final class AutoTest {
    private final Path out;
    private final List<Runnable> steps = new ArrayList<>();
    private int shot = 0;

    private AutoTest(Path out) { this.out = out; }

    static void startIfRequested() {
        String dir = System.getProperty("et2.autotest");
        if (dir == null) return;
        AutoTest t = new AutoTest(Path.of(dir));
        try { Files.createDirectories(t.out); } catch (Exception ignored) {}
        t.plan();
        t.next(2500);
    }

    private void keys(KeyCode... codes) { for (KeyCode k : codes) steps.add(() -> press(k, null, false)); }
    private void type(String s) { for (char c : s.toCharArray()) steps.add(() -> press(KeyCode.getKeyCode(String.valueOf(c).toUpperCase()), String.valueOf(c), java.lang.Character.isUpperCase(c))); }
    private void snap(String name) { steps.add(() -> takeShot(name)); }
    private void mark(String text) { steps.add(() -> Log.info("AUTOTEST >>> " + text)); }

    private void plan() {
        String which = System.getProperty("et2.autotest.plan", "tour");
        if (which.equals("battle")) { planBattle(); return; }
        if (which.equals("screens")) { planScreens(); return; }
        if (which.equals("options")) { planOptions(); return; }
        if (which.equals("spanish")) { planSpanish(); return; }
        planTour();
    }

    /** New character, then a real fight: Enter = Attack every turn (and Strike! on criticals). */
    private void planBattle() {
        keys(KeyCode.ENTER, KeyCode.ENTER);
        type("Tester");
        keys(KeyCode.ENTER, KeyCode.END, KeyCode.ENTER);
        steps.add(() -> {});
        keys(KeyCode.ENTER); // intro event: Continue
        steps.add(() -> {});
        mark("start battle ORC_PEASANT_1");
        steps.add(() -> {
            App.Shell.flowManager.currentBattleId = com.cnx.endlesstalestwo.data.battles.BattlesIds.ORC_PEASANT_1;
            App.Shell.flowManager.isPlayerBattling = true;
            Game.activity.StartBattleScreen(com.cnx.endlesstalestwo.data.battles.BattlesIds.ORC_PEASANT_1);
        });
        steps.add(() -> {});
        snap("battle-start");
        keys(KeyCode.S);
        for (int i = 0; i < 30; i++) {
            keys(KeyCode.ENTER);
            if (i == 3) snap("battle-mid");
        }
        snap("battle-end");
        keys(KeyCode.ENTER);
        steps.add(() -> {});
        snap("after-battle");
        steps.add(() -> {
            Log.info("AUTOTEST after battle: screen=" + Screens.current().getClass().getSimpleName() + " hp="
                    + App.getPlayerChar().attributesManager.getCurrent(com.cnx.endlesstalestwo.enums.Enums.AttributeName.HP)
                    + " xp=" + App.getPlayerChar().experience + " gold=" + App.getPlayerChar().gold + " dead=" + App.getPlayerChar().isDead
                    + " corrupted=" + App.Shell.isCorrupted());
            Platform.exit();
        });
    }

    /** Inventory, skills, shop, craft, map, house, mounts — each driven by keys. */
    private void planScreens() {
        keys(KeyCode.ENTER, KeyCode.ENTER);
        type("Tester");
        keys(KeyCode.ENTER, KeyCode.END, KeyCode.ENTER);
        steps.add(() -> {});
        keys(KeyCode.ENTER);
        steps.add(() -> {
            var pc = App.getPlayerChar();
            pc.addGold(500);
            pc.skillPoints = 6;
            var L = com.cnx.endlesstalestwo.libs.LibInventory.class;
            com.cnx.endlesstalestwo.libs.LibInventory.addToInventory(com.cnx.endlesstalestwo.data.items.ItemsIds.LISPORT_MAP, 1, (com.cnx.endlesstalestwo.entities.Creature) pc);
            for (var id : com.cnx.endlesstalestwo.data.items.ItemsIds.values()) {
                String n = id.name();
                if (n.contains("HEALTH_POTION") || n.equals("WHEAT") || n.equals("WATER") || n.equals("RED_HERB") || n.equals("BRANCH"))
                    com.cnx.endlesstalestwo.libs.LibInventory.addToInventory(id, 3, (com.cnx.endlesstalestwo.entities.Creature) pc);
            }
            Log.info("AUTOTEST inventory: " + pc.inventory.size() + " kinds");
        });
        mark("inventory (I): filter, first item, use/equip");
        keys(KeyCode.I);
        snap("inventory");
        keys(KeyCode.DOWN, KeyCode.ENTER);
        snap("inventory-item");
        keys(KeyCode.ENTER, KeyCode.ESCAPE);
        mark("equipment");
        steps.add(() -> Game.activity.openEquipments());
        snap("equipment");
        keys(KeyCode.ESCAPE);
        mark("skills: improve attributes, raise strength twice, confirm");
        steps.add(() -> Game.activity.openSkills());
        keys(KeyCode.ENTER, KeyCode.DOWN, KeyCode.DOWN, KeyCode.RIGHT, KeyCode.RIGHT);
        snap("attributes");
        keys(KeyCode.END, KeyCode.UP, KeyCode.ENTER, KeyCode.ESCAPE);
        mark("shop of Lisport: buy first item, sell");
        steps.add(() -> {
            var lis = App.DataManager.getLocale(com.cnx.endlesstalestwo.data.locations.LocationsIds.LISPORT);
            Log.info("AUTOTEST Lisport shops=" + lis.shops + " stables=" + lis.stables);
            Game.activity.goToViewShop(lis.shops.get(0));
        });
        snap("shop");
        keys(KeyCode.ENTER, KeyCode.ENTER);
        snap("shop-item");
        keys(KeyCode.ENTER, KeyCode.ESCAPE, KeyCode.ESCAPE);
        keys(KeyCode.END, KeyCode.UP, KeyCode.ENTER);
        snap("shop-sell");
        keys(KeyCode.ESCAPE, KeyCode.ESCAPE);
        mark("craft (general): slot 1 = first ingredient");
        steps.add(() -> Game.activity.goToCraft(com.cnx.endlesstalestwo.enums.Enums.CraftTypes.GENERAL));
        keys(KeyCode.ENTER, KeyCode.ENTER);
        snap("craft");
        keys(KeyCode.ESCAPE);
        mark("map");
        steps.add(() -> Game.activity.goToViewMap());
        snap("map");
        keys(KeyCode.RIGHT, KeyCode.LEFT, KeyCode.ESCAPE);
        mark("house");
        steps.add(() -> {
            var pc = App.getPlayerChar();
            var hid = com.cnx.endlesstalestwo.data.houses.HousesIds.values()[0];
            if (!pc.ownsHouse(hid)) pc.addOwnedHouse(hid);
            Game.activity.goToCharacterHouse(hid);
        });
        snap("house");
        keys(KeyCode.END, KeyCode.UP, KeyCode.ENTER, KeyCode.ENTER, KeyCode.ENTER);
        keys(KeyCode.ESCAPE, KeyCode.ESCAPE);
        mark("mounts");
        steps.add(() -> Game.activity.openMounts());
        snap("mounts");
        keys(KeyCode.ESCAPE);
        steps.add(() -> {
            Log.info("AUTOTEST screens done: gold=" + App.getPlayerChar().gold + " str=" + App.getPlayerChar().attributesManager.getBase(com.cnx.endlesstalestwo.enums.Enums.AttributeName.STRENGTH)
                    + " points=" + App.getPlayerChar().skillPoints + " corrupted=" + App.Shell.isCorrupted());
            Platform.exit();
        });
    }

    private void state(String when) {
        steps.add(() -> Log.info("AUTOTEST state " + when + ": modals=" + Ui.get().modalCount() + " screen=" + (Screens.current() == null ? "-" : Screens.current().getClass().getSimpleName())
                + " speech=" + Speech.mode + " rate=" + Speech.voiceRate + " positions=" + Nav.announcePositions + " sounds=" + Nav.menuSounds
                + " critical=" + Settings.criticalMinigame + " music=" + Math.round(FxAudio.musicVolume * 100) + " sound=" + Math.round(FxAudio.soundVolume * 100)
                + " textSpeed=" + Settings.textSpeed + " full=" + GameApp.stage().isFullScreen() + "/" + Settings.fullscreen + " lang='" + Settings.language + "'"
                + " uiLang=" + App.CURRENT_LANGUAGE));
    }

    /** Every option from the title screen and from the game menu, keyboard only. */
    private void planOptions() {
        state("start");
        mark("title -> Options");
        keys(KeyCode.DOWN, KeyCode.ENTER);
        snap("options-title");
        state("options open");
        mark("each item: Right, Left, Down");
        for (int i = 0; i < 16; i++) keys(KeyCode.RIGHT, KeyCode.LEFT, KeyCode.DOWN);
        state("after Right+Left on everything (should equal start)");
        mark("music volume up 3, sound down 1, text speed right 2, voice speed right 1");
        keys(KeyCode.HOME);
        keys(KeyCode.DOWN, KeyCode.DOWN, KeyCode.RIGHT);           // voice speed +1
        keys(KeyCode.PAGE_DOWN, KeyCode.PAGE_DOWN, KeyCode.DOWN, KeyCode.RIGHT, KeyCode.RIGHT, KeyCode.RIGHT); // music
        keys(KeyCode.DOWN, KeyCode.LEFT);                          // sound
        keys(KeyCode.PAGE_DOWN, KeyCode.DOWN, KeyCode.RIGHT, KeyCode.RIGHT); // text speed
        state("after changes");
        mark("language Spanish, check, back to English");
        keys(KeyCode.END, KeyCode.UP, KeyCode.RIGHT);
        steps.add(() -> {});
        snap("options-spanish");
        state("spanish");
        keys(KeyCode.LEFT);
        steps.add(() -> {});
        state("english again");
        mark("speech off and back on");
        keys(KeyCode.HOME, KeyCode.DOWN);
        keys(KeyCode.LEFT);
        state("speech off");
        keys(KeyCode.DOWN, KeyCode.UP, KeyCode.RIGHT);
        state("speech automatic again");
        mark("full screen with F11 twice");
        keys(KeyCode.F11);
        state("F11 once");
        keys(KeyCode.F11);
        state("F11 twice");
        mark("Escape = Done, back on title");
        keys(KeyCode.ESCAPE);
        state("title again");
        mark("restart settings from file");
        steps.add(() -> { Settings.load(); Log.info("AUTOTEST reloaded"); });
        state("reloaded from settings.properties");
        mark("new game, then game menu -> options -> back");
        keys(KeyCode.HOME, KeyCode.ENTER, KeyCode.ENTER);
        type("Opt");
        keys(KeyCode.ENTER, KeyCode.END, KeyCode.ENTER);
        steps.add(() -> {});
        keys(KeyCode.ENTER);
        steps.add(() -> {});
        keys(KeyCode.ESCAPE);
        state("menu");
        keys(KeyCode.DOWN, KeyCode.DOWN, KeyCode.ENTER);
        snap("options-game");
        state("options from menu");
        keys(KeyCode.ESCAPE);
        state("menu again (Options focused)");
        keys(KeyCode.ESCAPE);
        state("game");
        mark("menu -> save game");
        keys(KeyCode.ESCAPE, KeyCode.DOWN, KeyCode.ENTER);
        state("saved");
        mark("character sheet and help close back to the game");
        keys(KeyCode.C, KeyCode.ESCAPE, KeyCode.F1, KeyCode.ESCAPE);
        state("end");
        steps.add(Platform::exit);
    }

    /** Switch to Spanish in the options, then title, help, a new game, status and the game menu. */
    private void planSpanish() {
        keys(KeyCode.DOWN, KeyCode.ENTER);
        keys(KeyCode.END, KeyCode.UP, KeyCode.RIGHT);
        steps.add(() -> {});
        snap("options-es");
        keys(KeyCode.UP, KeyCode.UP, KeyCode.PAGE_UP, KeyCode.DOWN, KeyCode.ESCAPE);
        snap("title-es");
        keys(KeyCode.F1);
        snap("help-es");
        keys(KeyCode.ESCAPE, KeyCode.HOME, KeyCode.ENTER, KeyCode.ENTER);
        type("Anax");
        keys(KeyCode.BACK_SPACE, KeyCode.ENTER, KeyCode.END, KeyCode.ENTER);
        steps.add(() -> {});
        keys(KeyCode.ENTER);
        steps.add(() -> {});
        keys(KeyCode.S, KeyCode.ESCAPE);
        snap("menu-es");
        keys(KeyCode.DOWN, KeyCode.DOWN, KeyCode.ENTER, KeyCode.ESCAPE, KeyCode.ESCAPE);
        steps.add(() -> { Log.info("AUTOTEST spanish done lang=" + Settings.language + " ui=" + App.CURRENT_LANGUAGE); Platform.exit(); });
    }

    private void planTour() {
        snap("title");
        mark("title: Enter = New game");
        keys(KeyCode.ENTER);
        snap("new-game");
        mark("new game: type name");
        keys(KeyCode.ENTER);
        type("Tester");
        keys(KeyCode.ENTER);
        mark("class: next class, then back");
        keys(KeyCode.DOWN, KeyCode.RIGHT, KeyCode.LEFT);
        mark("begin");
        keys(KeyCode.END, KeyCode.ENTER);
        steps.add(() -> {});
        snap("intro-event");
        mark("repeat scene, then continue");
        keys(KeyCode.R, KeyCode.ENTER);
        steps.add(() -> {});
        snap("explore");
        mark("status");
        keys(KeyCode.S);
        mark("Persons -> first NPC");
        keys(KeyCode.ENTER, KeyCode.ENTER);
        snap("conversation");
        mark("first reply, then goodbye with Escape");
        keys(KeyCode.ENTER, KeyCode.ESCAPE);
        steps.add(() -> {});
        mark("Actions -> first action");
        keys(KeyCode.DOWN, KeyCode.ENTER, KeyCode.ENTER);
        steps.add(() -> {});
        snap("action-reward");
        keys(KeyCode.ENTER);
        mark("character sheet, next section, close");
        keys(KeyCode.C, KeyCode.PAGE_DOWN, KeyCode.DOWN);
        snap("character");
        keys(KeyCode.ESCAPE);
        mark("help");
        keys(KeyCode.F1);
        snap("help");
        keys(KeyCode.ESCAPE);
        mark("game menu -> Options");
        keys(KeyCode.ESCAPE, KeyCode.DOWN, KeyCode.DOWN, KeyCode.ENTER);
        snap("options");
        mark("options: speech output (Right), then position setting");
        keys(KeyCode.DOWN, KeyCode.DOWN, KeyCode.ESCAPE);
        steps.add(() -> {
            Log.info("AUTOTEST location=" + (App.Shell.flowManager.currentLocation == null ? "-" : App.Shell.flowManager.currentLocation.getName(App.CURRENT_LANGUAGE))
                    + " gold=" + App.getPlayerChar().gold + " corrupted=" + App.Shell.isCorrupted());
            Game.save();
            Platform.exit();
        });
    }

    private void next(long delay) {
        if (steps.isEmpty()) return;
        Runnable r = steps.remove(0);
        PauseTransition p = new PauseTransition(Duration.millis(delay));
        p.setOnFinished(e -> {
            try { r.run(); } catch (Throwable t) { Log.error("AUTOTEST step failed", t); }
            next(1300);
        });
        p.play();
    }

    /** True while the test is sending a key (real keyboard input is ignored during tests). */
    static volatile boolean firing;

    private static void press(KeyCode code, String text, boolean shift) {
        Scene scene = GameApp.stage().getScene();
        String t = text != null ? text : (code == KeyCode.SPACE ? " " : "");
        Log.info("AUTOTEST key " + code + (text != null ? " '" + text + "'" : ""));
        firing = true;
        try { Event.fireEvent(scene.getRoot(), new KeyEvent(KeyEvent.KEY_PRESSED, t, t, code, shift, false, false, false)); }
        finally { firing = false; }
    }

    private void takeShot(String name) {
        WritableImage img = Ui.get().surface.snapshot(null, null);
        int w = (int) img.getWidth(), h = (int) img.getHeight();
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] px = new int[w * h];
        img.getPixelReader().getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        bi.setRGB(0, 0, w, h, px, 0, w);
        Path f = out.resolve(String.format("%02d-%s.png", ++shot, name));
        try { ImageIO.write(bi, "png", f.toFile()); } catch (Exception e) { Log.error("snapshot failed", e); }
    }
}
