package com.cnx.desktop.game.screens;

import com.cnx.desktop.game.*;
import com.cnx.desktop.game.Nav;
import com.cnx.desktop.game.Speech;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.entities.Character;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Title screen: continue, choose a saved character, new game, options, quit. */
public final class TitleScreen implements Screens.Screen {
    @Override
    public void show() {
        Ui ui = Ui.get();
        ui.clearScene();
        ui.setBackground(Images.drawable("game_bg"), null);
        ui.dimBackground(false);
        Sound.music("game_theme");
        App.Shell.character = null;

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("title-box");
        ImageView logo = new ImageView(Images.drawable("game_logo"));
        logo.setFitHeight(330);
        logo.setPreserveRatio(true);
        Nav.alt(logo, Tr.t("Endless Tales 2. Main menu"));
        box.getChildren().add(logo);
        box.getProperties().put("nav.noEscape", true); // Escape must not close the main menu

        List<Character> chars = Game.savedCharacters();
        final Runnable[] close = new Runnable[1];
        if (!chars.isEmpty()) {
            Character last = chars.get(chars.size() - 1);
            box.getChildren().add(big(Tr.t("Continue") + "  —  " + last.getName() + " (" + Game.t("Level") + " " + last.level + ")", () -> { close[0].run(); play(last); }));
            if (chars.size() > 1) box.getChildren().add(big(Tr.t("Choose character"), () -> { close[0].run(); chooseCharacter(); }));
        }
        box.getChildren().add(big(Tr.t("New game"), () -> { close[0].run(); Screens.show(new NewGameScreen()); }));
        box.getChildren().add(big(Tr.t("Options"), () -> Panels.options(null)));
        box.getChildren().add(big(Tr.t("Quit"), javafx.application.Platform::exit));
        Label ver = new Label(Tr.t("Windows edition  ·  game data v2.0.0.3  ·  F1: keyboard help"));
        ver.getStyleClass().add("hud-small");
        box.getChildren().add(ver);
        close[0] = ui.showModal(box, false, null);
    }

    private static Button big(String text, Runnable r) {
        Button b = Ui.button(text, r);
        b.getStyleClass().add("btn-big");
        b.setPrefWidth(420);
        return b;
    }

    static void play(Character c) {
        Game.resumeCharacter(c);
        Ui.get().transition(c.getName(), 600, () -> Screens.show(Screens.explore()));
    }

    private void chooseCharacter() {
        VBox list = new VBox(10);
        final Runnable[] close = new Runnable[1];
        for (Character c : Game.savedCharacters()) {
            ImageView face = new ImageView(Images.asset("characters", c.imgFile));
            face.setFitHeight(64);
            face.setPreserveRatio(true);
            Label name = new Label(c.getName() + "\n" + c.getCharClassName() + "  ·  " + Game.t("Level") + " " + c.level);
            name.getStyleClass().add("stat-label");
            Nav.skip(name); // the two buttons below already say whose they are
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Button play = Ui.button(Tr.t("Play"), () -> { close[0].run(); play(c); });
            play.getProperties().put(Nav.ITEM, new Nav.Item(Tr.f("Play %s, %s, level %d", c.getName(), c.getCharClassName(), c.level), play).onActivate(play::fire));
            Button del = Ui.button(Tr.t("Delete"), () -> Ui.get().confirm(Tr.t("Delete character"), Tr.f("Delete %s? This cannot be undone.", c.getName()), yes -> {
                if (yes) { Game.deleteCharacter(c); close[0].run(); Screens.title(); }
            }));
            del.getProperties().put(Nav.ITEM, new Nav.Item(Tr.f("Delete %s", c.getName()), del).onActivate(del::fire));
            HBox row = new HBox(12, face, name, sp, play, del);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6));
            row.getStyleClass().add("item-cell");
            list.getChildren().add(row);
        }
        close[0] = Ui.get().showModal(Ui.panel(Tr.t("Characters"), list, 640, Ui.button(Tr.t("Back"), () -> { close[0].run(); Screens.title(); })), false, null);
    }
}
