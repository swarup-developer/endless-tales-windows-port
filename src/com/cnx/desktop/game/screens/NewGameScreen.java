package com.cnx.desktop.game.screens;

import com.cnx.cnxgameengine.utils.LibUtils;
import com.cnx.desktop.game.*;
import com.cnx.desktop.game.Nav;
import com.cnx.desktop.game.Speech;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.entities.CharClass;
import com.cnx.endlesstalestwo.enums.Enums;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** Character creation (port of ViewNewGame): name, class, gender, avatar. */
public final class NewGameScreen implements Screens.Screen {
    private int classIndex = 0, avatar = 0;
    private Enums.Gender gender = Enums.Gender.MALE;
    private final TextField name = new TextField();
    private final ImageView face = new ImageView();
    private final Label className = new Label(), genderLabel = new Label(), avatarLabel = new Label();
    private final Label description = new Label();
    private final GridPane stats = new GridPane();
    private Runnable close;

    @Override
    public void show() {
        Ui ui = Ui.get();
        ui.clearScene();
        ui.setBackground(Images.drawable("game_bg3") != null ? Images.drawable("game_bg3") : Images.drawable("game_bg"), null);
        ui.dimBackground(true);

        name.setPromptText(Tr.t("Character name"));
        name.getStyleClass().add("name-field");
        face.setFitHeight(420);
        face.setPreserveRatio(true);
        description.setWrapText(true);
        description.getStyleClass().add("para");
        stats.setHgap(18);
        stats.setVgap(6);

        HBox avatarRow = carousel(avatarLabel, -1, () -> { avatar = (avatar + 5) % 6; update(); }, () -> { avatar = (avatar + 1) % 6; update(); });
        VBox left = new VBox(10, face, avatarRow);
        left.setAlignment(Pos.TOP_CENTER);
        HBox classRow = carousel(className, -1, () -> { classIndex = (classIndex + classes().size() - 1) % classes().size(); update(); },
                () -> { classIndex = (classIndex + 1) % classes().size(); update(); });
        HBox genderRow = carousel(genderLabel, -1, this::toggleGender, this::toggleGender);
        VBox right = new VBox(12, name, classRow, genderRow, description, stats);
        right.setPrefWidth(520);
        // keyboard order: name, class (with its description and stats), gender, avatar, then Begin / Back
        HBox content = new HBox(30, right, left);

        Nav nav = Ui.get().nav;
        name.setFocusTraversable(false);
        name.getProperties().put(Nav.ITEM, new Nav.Item(() -> Tr.t("Character name") + ": " + (name.getText().isEmpty() ? Tr.t("Empty") : name.getText())
                + ". " + Tr.t("Press Enter to type"), name).onActivate(() -> nav.editText(name, Tr.t("Character name"), null)));
        classRow.getProperties().put(Nav.ITEM, new Nav.Item(() -> {
            CharClass c = classes().get(classIndex);
            return Tr.t("Class") + ": " + c.getName(App.CURRENT_LANGUAGE) + ", " + Tr.f("%d of %d", classIndex + 1, classes().size())
                    + ". " + Tr.t("Left and Right to change.") + " " + c.getDescription(App.CURRENT_LANGUAGE) + " " + statsText(c);
        }, classRow).onAdjust(d -> { classIndex = (classIndex + d + classes().size()) % classes().size(); update(); }));
        genderRow.getProperties().put(Nav.ITEM, new Nav.Item(() -> Tr.t("Gender") + ": " + genderLabel.getText() + ". " + Tr.t("Left and Right to change."), genderRow)
                .onAdjust(d -> toggleGender()));
        avatarRow.getProperties().put(Nav.ITEM, new Nav.Item(() -> Tr.t("Portrait") + ": " + Tr.f("%d of %d", avatar + 1, 6) + ". " + Tr.t("Left and Right to change."), avatarRow)
                .onAdjust(d -> { avatar = (avatar + d + 6) % 6; update(); }));
        Nav.skip(description);
        Nav.skip(stats);
        content.setPadding(new Insets(4));
        Button back = Ui.button(Tr.t("Back"), () -> { close.run(); Screens.title(); });
        Button begin = Ui.button(Tr.t("Begin adventure"), this::begin);
        begin.setDefaultButton(true);
        close = ui.showModal(Ui.panel(Tr.t("New game"), content, 980, back, begin), false, null);
        update();
    }

    private static String statsText(CharClass c) {
        return Tr.f("Starting stats: health %d, mana %d", c.startHp, c.startMp) + ", " + Game.t("STRENGTH") + " " + c.startStrength
                + ", " + Game.t("MAGIC") + " " + c.startMagic + ", " + Game.t("CHARISMA") + " " + c.startCharisma
                + ", " + Game.t("AGILITY") + " " + c.startAgility + ", " + Game.t("LUCK") + " " + c.startLuck
                + ", " + Game.t("ALIGNMENT") + " " + c.initialAlignment + ".";
    }

    private static List<CharClass> classes() { return Game.classes(); }

    private void toggleGender() { gender = gender == Enums.Gender.MALE ? Enums.Gender.FEMALE : Enums.Gender.MALE; update(); }

    private static HBox carousel(Label value, int unused, Runnable left, Runnable right) {
        value.getStyleClass().add("carousel-value");
        value.setMinWidth(220);
        value.setAlignment(Pos.CENTER);
        HBox h = new HBox(10, Ui.button("◀", left), value, Ui.button("▶", right));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void update() {
        CharClass c = classes().get(classIndex);
        className.setText(c.getName(App.CURRENT_LANGUAGE));
        genderLabel.setText(LibUtils.getEnum(gender, Game.activity));
        avatarLabel.setText(Tr.t("Portrait") + " " + (avatar + 1));
        face.setImage(Images.asset("characters", (gender == Enums.Gender.MALE ? "male_char_" : "female_char_") + (avatar + 1)));
        description.setText(c.getDescription(App.CURRENT_LANGUAGE) + "\n" + Game.t("Initialstats") + ":");
        stats.getChildren().clear();
        String[][] rows = {
                {"icon_hp_color", "HP", String.valueOf(c.startHp)}, {"icon_mp_color", "MP", String.valueOf(c.startMp)},
                {"icon_strength", Game.t("STRENGTH"), String.valueOf(c.startStrength)}, {"icon_magic", Game.t("MAGIC"), String.valueOf(c.startMagic)},
                {"icon_charisma", Game.t("CHARISMA"), String.valueOf(c.startCharisma)}, {"icon_agility", Game.t("AGILITY"), String.valueOf(c.startAgility)},
                {"icon_luck", Game.t("LUCK"), String.valueOf(c.startLuck)}, {"icon_alignment", Game.t("ALIGNMENT"), String.valueOf(c.initialAlignment)}};
        for (int i = 0; i < rows.length; i++) {
            HBox r = Ui.statRow(rows[i][0], rows[i][1], rows[i][2]);
            r.setPrefWidth(240);
            stats.add(r, i % 2, i / 2);
        }
    }

    /** ViewNewGame.onTapBegin(): same name rules as the Android game. */
    private void begin() {
        String str = name.getText() == null ? "" : name.getText().trim();
        boolean bad;
        try { bad = LibUtils.hasBadWords(str); } catch (Throwable t) { bad = false; }
        for (char c : new char[]{'_', '*', '\\', '/', '@', '!', '#', '=', '^', '~', ';', '%', '?', '|', 187, '$', '[', ']', '{', '}', '(', ')'}) {
            if (str.indexOf(c) >= 0) { bad = true; break; }
        }
        if (str.length() < 3 || bad) {
            Ui.get().toast(Game.t("ChooseValidName"), true);
            name.requestFocus();
            return;
        }
        String clean;
        try { clean = LibUtils.filterBadWords(LibUtils.capitalizeString(str).replace("\n", "").replace("\r", "")); }
        catch (Throwable t) { clean = str; }
        if (clean.length() > 20) clean = clean.substring(0, 20);
        Sound.play("confirm");
        close.run();
        Game.startNewCharacter(clean, classes().get(classIndex), gender, avatar);
        String hero = clean;
        Ui.get().transition(hero, 900, () -> Screens.show(Screens.explore()));
    }
}
