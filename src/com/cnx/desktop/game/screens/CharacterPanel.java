package com.cnx.desktop.game.screens;

import com.cnx.desktop.game.*;
import com.cnx.desktop.game.Nav;
import com.cnx.desktop.game.Speech;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.entities.ItemReference;
import com.cnx.endlesstalestwo.entities.Quest;
import com.cnx.endlesstalestwo.entities.Item;
import com.cnx.endlesstalestwo.enums.Enums;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Character sheet: stats, quests and the items being carried (read-only in this version). */
public final class CharacterPanel {
    private CharacterPanel() {}

    public static void show() {
        Character c = App.getPlayerChar();
        // one list with sections (Page Up / Page Down jump between them) instead of tabs
        long active = c.quests.stream().filter(q -> q.status != Enums.QuestStatus.COMPLETED).count();
        VBox all = new VBox(10,
                Ui.section(Tr.t("Stats")), stats(c),
                Ui.section(Tr.t("Quests: ") + active + Tr.t(" active, ") + (c.quests.size() - active) + Tr.t(" completed")), quests(c),
                Ui.section(Tr.t("Inventory: ") + c.inventory.size() + Tr.t(" kinds of items")), inventory(c));
        final Runnable[] close = new Runnable[1];
        close[0] = Ui.get().showModal(Ui.panel(c.getName() + "  —  " + c.getCharClassName() + "  " + Game.t("Level") + " " + c.level,
                all, 760, Ui.button(Tr.t("Close"), () -> close[0].run())), true, () -> Ui.get().busy(false));
    }

    private static javafx.scene.Node stats(Character c) {
        ImageView face = new ImageView(Images.asset("characters", c.imgFile));
        face.setFitHeight(300);
        face.setPreserveRatio(true);
        VBox list = new VBox(6);
        var a = c.attributesManager;
        list.getChildren().addAll(
                Ui.statRow("icon_hp_color", "HP", a.getCurrent(Enums.AttributeName.HP) + Tr.t(" of ") + a.getMaxHp()),
                Ui.statRow("icon_mp_color", "MP", a.getCurrent(Enums.AttributeName.MP) + Tr.t(" of ") + a.getMaxMp()),
                Ui.statRow("icon_experience", "XP", String.valueOf(c.experience)),
                Ui.statRow("icon_coins", Tr.t("Gold"), String.valueOf(c.gold)),
                Ui.statRow("icon_strength", Game.t("STRENGTH"), String.valueOf(a.getCurrent(Enums.AttributeName.STRENGTH))),
                Ui.statRow("icon_magic", Game.t("MAGIC"), String.valueOf(a.getCurrent(Enums.AttributeName.MAGIC))),
                Ui.statRow("icon_agility", Game.t("AGILITY"), String.valueOf(a.getCurrent(Enums.AttributeName.AGILITY))),
                Ui.statRow("icon_charisma", Game.t("CHARISMA"), String.valueOf(a.getCurrent(Enums.AttributeName.CHARISMA))),
                Ui.statRow("icon_luck", Game.t("LUCK"), String.valueOf(a.getCurrent(Enums.AttributeName.LUCK))),
                Ui.statRow("icon_alignment", Game.t("ALIGNMENT"), String.valueOf(a.getCurrent(Enums.AttributeName.ALIGNMENT))),
                Ui.statRow("icon_defense", Game.t("DEFENSE"), String.valueOf(a.getCurrent(Enums.AttributeName.DEFENSE))),
                Ui.statRow("icon_skills", Tr.t("Skill points"), String.valueOf(c.skillPoints)));
        HBox row = new HBox(24, face, list);
        row.setPadding(new Insets(10));
        HBox.setHgrow(list, javafx.scene.layout.Priority.ALWAYS);
        return row;
    }

    private static javafx.scene.Node quests(Character c) {
        VBox list = new VBox(12);
        list.setPadding(new Insets(10));
        for (Quest q : c.quests) {
            VBox one = new VBox(3);
            Label name = new Label(q.getName(App.CURRENT_LANGUAGE) + ", " + ExploreScreen.pretty(q.status == null ? "" : q.status.name()));
            name.getStyleClass().add("stat-label");
            one.getChildren().add(name);
            try {
                if (q.status != Enums.QuestStatus.COMPLETED && q.getCurrentQuestPart() != null) {
                    one.getChildren().add(Ui.para(q.getCurrentQuestPart().getDescription(App.CURRENT_LANGUAGE)));
                }
            } catch (Throwable ignored) {}
            list.getChildren().add(one);
        }
        if (c.quests.isEmpty()) list.getChildren().add(Ui.para(Tr.t("No quests yet.")));
        return list;
    }

    private static javafx.scene.Node inventory(Character c) {
        FlowPane grid = new FlowPane(10, 10);
        grid.setPadding(new Insets(10));
        for (ItemReference ref : c.inventory) {
            if (ref == null || ref.itemId == null) continue;
            Item item = App.DataManager.getItem(ref.itemId);
            ImageView iv = new ImageView(item == null ? (javafx.scene.image.Image) null : Images.asset("items", item.getImageFile()));
            iv.setFitWidth(56);
            iv.setFitHeight(56);
            iv.setPreserveRatio(true);
            Label l = new Label((item == null ? ref.itemId.name() : item.getName(App.CURRENT_LANGUAGE)) + (ref.amount > 1 ? " x" + ref.amount : ""));
            l.setWrapText(true);
            l.setMaxWidth(110);
            l.getStyleClass().add("item-label");
            VBox cell = new VBox(4, iv, l);
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setPrefWidth(116);
            cell.getStyleClass().add("item-cell");
            grid.getChildren().add(cell);
        }
        if (grid.getChildren().isEmpty()) grid.getChildren().add(Ui.para(Tr.t("Empty.")));
        return grid;
    }
}
