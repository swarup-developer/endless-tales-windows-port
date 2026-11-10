package com.cnx.desktop.game.screens;

import android.content.Context;
import com.cnx.desktop.game.*;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.R;
import com.cnx.endlesstalestwo.data.knowledges.KnowledgesIds;
import com.cnx.endlesstalestwo.entities.*;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.libs.LibInventory;
import com.cnx.endlesstalestwo.libs.LibNpc;
import javafx.scene.image.Image;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Inventory and equipment (port of the Android ViewInventory / ViewEquipment / ViewReader).
 * Items are a list; choosing one reads its details and offers Use / Read / Equip / Discard.
 */
public final class InventoryScreen implements Screens.Screen {
    private static final String[] FILTERS = {"All items", "Equipment", "Consumables", "Other items"};
    private int filter = 0;
    private final boolean equipmentFirst;
    private Runnable back;

    public InventoryScreen(boolean equipmentFirst) { this.equipmentFirst = equipmentFirst; }

    private static Context ctx() { return Game.activity; }
    private static Character pc() { return App.getPlayerChar(); }
    private static String s(int id, Object... a) { try { return a.length == 0 ? Game.activity.getString(id) : Game.activity.getString(id, a); } catch (Throwable t) { return ""; } }

    @Override
    public void show() {
        Ui ui = Ui.get();
        ui.clearScene();
        ui.showHud(true);
        ui.dimBackground(true);
        if (equipmentFirst) showEquipment(); else showList(true);
    }

    @Override public void refresh() { Ui.get().refreshHud(); showList(false); }

    private String header() {
        Character c = pc();
        String weight = c.getCarriedWeight() + Tr.t(" of ") + c.getWeigthCapacity() + Tr.t(" kilograms") + (c.isBurdened ? Tr.t(", too heavy, you move slowly") : "");
        int food = (int) com.cnx.cnxgameengine.utils.LibUtils.getPercentInRange(0, c.attributesManager.getBase(Enums.AttributeName.HUNGRY), c.minutesToStarve);
        return Tr.t("Carrying ") + weight + Tr.t(". Food ") + food + Tr.t(" percent") + (food <= 20 ? Tr.t(", hungry") : "") + ".";
    }

    // ---------------------------------------------------------------- item list

    private void showList(boolean speakHeader) {
        List<ItemReference> items = new ArrayList<>();
        for (ItemReference r : pc().inventory) {
            Item it = r.getItemData();
            if (it == null) continue;
            boolean ok = switch (filter) {
                case 1 -> it instanceof EquippableItem;
                case 2 -> it instanceof ConsumableItem;
                case 3 -> !(it instanceof EquippableItem) && !(it instanceof ConsumableItem);
                default -> true;
            };
            if (ok) items.add(r);
        }
        items.sort(Comparator.comparing(r -> Normalizer.normalize(r.getItemData().getName(App.CURRENT_LANGUAGE), Normalizer.Form.NFD)));
        Ui ui = Ui.get();
        String title = Game.t("Inventory").equals("Inventory") ? Tr.t("Inventory") : Game.t("Inventory");
        if (speakHeader) ui.say(title, null, header(), null);
        else ui.showTextQuiet(title, null, header());
        ui.setSceneSpeech(title + ". " + header());

        List<Ui.Choice> c = Ui.list();
        c.add(new Ui.Choice(Tr.t("Show: ") + Tr.t(FILTERS[filter]), Tr.t("Enter to change"), Icons.get("icon_sort_type"), true, () -> {
            filter = (filter + 1) % FILTERS.length;
            showList(false);
        }));
        for (ItemReference r : items) {
            Item it = r.getItemData();
            String type = ExploreScreen.pretty(it.type == null ? "" : it.type.name());
            c.add(new Ui.Choice(it.getName(App.CURRENT_LANGUAGE) + (r.amount > 1 ? ", " + r.amount : ""), type,
                    Images.asset("items", it.getImageFile()), true, () -> showItem(r)));
        }
        if (items.isEmpty()) c.add(new Ui.Choice(s(R.string.listNoItems).isEmpty() ? Tr.t("No items") : s(R.string.listNoItems), null, null, false, () -> {}));
        c.add(new Ui.Choice(Game.t("Equipment").equals("Equipment") ? Tr.t("Equipment") : Game.t("Equipment"), Icons.get("icon_equipments"), this::showEquipment));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), this::close));
        if (!speakHeader) ui.queueMenuAfterSpeech();
        ui.setChoices(title + ", " + Tr.t(FILTERS[filter]).toLowerCase(), c, this::close);
    }

    private void close() { Screens.backToGameplay(); }

    /** Everything a player needs to know about an item, as text. */
    static String describe(Item it, int amount) { return describe(it, amount, true); }

    static String describe(Item it, int amount, boolean withName) {
        StringBuilder sb = new StringBuilder();
        if (withName) sb.append(it.getName(App.CURRENT_LANGUAGE)).append(amount > 1 ? ", " + amount : "").append(". ");
        else if (amount > 1) sb.append(Tr.t("You have ")).append(amount).append(". ");
        if (it.rarity != null && it.rarity != Enums.Rarity.COMMON) sb.append(ExploreScreen.pretty(it.rarity.name())).append(". ");
        if (it.type != null) sb.append(ExploreScreen.pretty(it.type.name())).append(". ");
        String d = it.getDescription(App.CURRENT_LANGUAGE);
        if (d != null && !d.isBlank()) sb.append(d.strip()).append(" ");
        if (it instanceof EquippableItem e) {
            if (e.slotPart != null) sb.append(Tr.t("Worn on: ")).append(ExploreScreen.pretty(e.slotPart.name())).append(". ");
            String dmg = LibInventory.getDamageInfoDisplay(e);
            if (e.maxDamage > 0) sb.append(Tr.t("Damage ")).append(dmg).append(". ");
            if (e.defense != 0) sb.append(Tr.t("Defense ")).append(e.defense).append(". ");
            if (e.criticalChanceIncrease != 0) sb.append(Tr.t("Critical chance ")).append(e.criticalChanceIncrease).append(Tr.t(" percent. "));
            modifiers(sb, e.modifiers);
        }
        if (it instanceof ConsumableItem ci) {
            modifiers(sb, ci.modifiers);
            if (ci.hungryTimeRecover > 0) sb.append(Tr.t("Satisfies hunger. "));
            if (ci.inBattleUsage && !ci.outBattleUsage) sb.append(Tr.t("Only in battle. "));
        }
        sb.append(Tr.t("Weight ")).append(it.getWeightDisplay()).append(".");
        return sb.toString();
    }

    private static void modifiers(StringBuilder sb, List<AttributeModifierValue> mods) {
        if (mods == null) return;
        for (AttributeModifierValue m : mods) {
            try { sb.append(m.getAttributeName(Game.activity)).append(" ").append(m.getDisplayValue()).append(". "); }
            catch (Throwable ignored) {}
        }
    }

    // ---------------------------------------------------------------- one item

    private void showItem(ItemReference r) {
        Item it = r.getItemData();
        Ui ui = Ui.get();
        ui.say(it.getName(App.CURRENT_LANGUAGE), null, describe(it, r.amount, false), null);
        boolean canUse = false, reusable = false;
        if (it instanceof ConsumableItem ci) {
            canUse = ci.outBattleUsage && it.checkRequirements(pc(), ctx()) == Enums.RequirementVerification.OK;
            reusable = ci.reusable;
        } else if (it instanceof BookItem b) {
            canUse = pc().hasKnowledge(KnowledgesIds.READ_AND_WRITE) && it.checkRequirements(pc(), ctx()) == Enums.RequirementVerification.OK;
            reusable = !b.vanishAfterRead;
        }
        List<Ui.Choice> c = Ui.list();
        if (it instanceof BookItem) c.add(new Ui.Choice(canUse ? Tr.t("Read") : Tr.t("Read (you need to know how to read)"), null, Icons.get("icon_read_write"), canUse, () -> read((BookItem) it)));
        else if (it instanceof ConsumableItem) c.add(new Ui.Choice(Tr.t("Use"), null, Icons.get("icon_self_usage"), canUse, () -> use(r)));
        if (it instanceof EquippableItem e) c.add(new Ui.Choice(Tr.t("Equip"), null, Icons.get("icon_equip"), true, () -> equip(e)));
        if (it.discardable && !reusable) c.add(new Ui.Choice(Tr.t("Discard"), null, Icons.get("icon_discard"), true, () -> askDiscard(r)));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), () -> showList(false)));
        ui.setChoices(it.getName(App.CURRENT_LANGUAGE), c, () -> showList(false));
    }

    /** ViewInventory.useItemCommand() */
    private void use(ItemReference r) {
        Item it = r.getItemData();
        LibInventory.removeFromInventory(r.itemId, 1, (Creature) pc());
        ((ConsumableItem) it).defaultUseCommand(pc(), ctx());
        if (it.usageSound != null && !it.usageSound.isEmpty()) Sound.play(it.usageSound);
        else Sound.play("consumable_usage");
        pc().updateHungry(0);
        GameEngine.saveGame(ctx());
        Ui.get().refreshHud();
        Speech.say(s(R.string.menuuseditem, it.getName(App.CURRENT_LANGUAGE)) + Tr.t(". Health ") + pc().attributesManager.getCurrent(Enums.AttributeName.HP)
                + Tr.t(" of ") + pc().attributesManager.getMaxHp() + Tr.t(", mana ") + pc().attributesManager.getCurrent(Enums.AttributeName.MP) + Tr.t(" of ") + pc().attributesManager.getMaxMp() + ".", true);
        Ui.get().queueMenuAfterSpeech();
        showList(false);
    }

    /** ViewInventory.equipItemCommand() */
    private void equip(EquippableItem e) {
        Enums.RequirementVerification r = pc().equipItem(e.getUId(), ctx());
        if (r != null && r != Enums.RequirementVerification.OK) {
            Speech.say(Tr.t("Cannot equip: ") + ExploreScreen.pretty(r.name()), true);
            return;
        }
        Sound.play(e.slotPart == Enums.SlotPart.HAND_1 || e.slotPart == Enums.SlotPart.HAND_2 ? "equip_weapon" : "equip_item");
        GameEngine.saveGame(ctx());
        Ui.get().refreshHud();
        Speech.say(Tr.t("Equipped ") + e.getName(App.CURRENT_LANGUAGE), true);
        Ui.get().queueMenuAfterSpeech();
        showList(false);
    }

    /** ViewInventory.askDropItemCommand(): how many to drop. */
    private void askDiscard(ItemReference r) {
        String name = r.getItemData().getName(App.CURRENT_LANGUAGE);
        List<Ui.Choice> c = Ui.list();
        c.add(new Ui.Choice(Tr.t("Discard 1"), () -> discard(r, 1, name)));
        if (r.amount > 2) c.add(new Ui.Choice(Tr.t("Discard ") + (r.amount / 2), () -> discard(r, r.amount / 2, name)));
        if (r.amount > 1) c.add(new Ui.Choice(Tr.t("Discard all ") + r.amount, () -> discard(r, r.amount, name)));
        c.add(new Ui.Choice(Tr.t("Cancel"), Icons.get("icon_back"), () -> showItem(r)));
        Ui.get().setChoices(Tr.t("Discard ") + name + "?", c, () -> showItem(r));
    }

    private void discard(ItemReference r, int n, String name) {
        LibInventory.removeFromInventory(r.itemId, n, (Creature) pc());
        GameEngine.saveGame(ctx());
        Speech.say(s(R.string.Dropped, (n > 1 ? n + "x " : "") + name), true);
        Ui.get().queueMenuAfterSpeech();
        showList(false);
    }

    private void read(BookItem book) { readBook(book, () -> showList(false)); }

    /** ViewReader: the book text as a pop-up, one paragraph per line. */
    public static void readBook(BookItem book, Runnable after) {
        try { book.read(ctx()); } catch (Throwable t) { Log.error("Book read failed", t); }
        String text = LibNpc.talk(book.getText(App.CURRENT_LANGUAGE), ctx(), null).replace("[B]", "\n");
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(8);
        for (String para : text.split("\\n+")) if (!para.isBlank()) body.getChildren().add(Ui.para(para.strip()));
        final Runnable[] close = new Runnable[1];
        close[0] = Ui.get().showModal(Ui.panel(book.getName(App.CURRENT_LANGUAGE), body, 760, Ui.button(Tr.t("Close"), () -> close[0].run())), true,
                () -> { GameEngine.saveGame(ctx()); if (after != null) after.run(); });
    }

    // ---------------------------------------------------------------- equipment (ViewEquipment)

    private void showEquipment() {
        Ui ui = Ui.get();
        List<Ui.Choice> c = Ui.list();
        StringBuilder sum = new StringBuilder();
        for (ItemReference r : new ArrayList<>(pc().equipment)) {
            if (r == null || r.itemId == null) continue;
            Item it = r.getItemData();
            if (it == null) continue;
            String slot = r.slotPart != null ? ExploreScreen.pretty(r.slotPart.name()) : it instanceof EquippableItem e && e.slotPart != null ? ExploreScreen.pretty(e.slotPart.name()) : "";
            Enums.SlotPart part = r.slotPart != null ? r.slotPart : it instanceof EquippableItem e ? e.slotPart : null;
            sum.append(slot).append(": ").append(it.getName(App.CURRENT_LANGUAGE)).append(". ");
            c.add(new Ui.Choice(slot + ": " + it.getName(App.CURRENT_LANGUAGE), Tr.t("Enter to take off"), Images.asset("items", it.getImageFile()), part != null, () -> {
                pc().unequipItem(part, ctx());
                Sound.play("equip_item");
                GameEngine.saveGame(ctx());
                Ui.get().refreshHud();
                Speech.say(Tr.t("Took off ") + it.getName(App.CURRENT_LANGUAGE), true);
                ui.queueMenuAfterSpeech();
                showEquipment();
            }));
        }
        if (c.isEmpty()) sum.append(Tr.t("Nothing equipped. "));
        String title = Game.t("Equipment").equals("Equipment") ? Tr.t("Equipment") : Game.t("Equipment");
        var dmg = pc().getDamageInfo(ctx());
        String attack = "";
        try {
            for (AttributeModifierValue v : dmg.inflictValues) if (v.getAttribute() == Enums.AttributeName.HP) attack = Tr.t("Attack ") + v.getMinValue() + Tr.t(" to ") + v.getMaxValue() + ". ";
        } catch (Throwable ignored) {}
        String text = attack + Tr.t("Defense ") + pc().attributesManager.getCurrent(Enums.AttributeName.DEFENSE) + ". " + sum;
        ui.say(title, null, text, null);
        c.add(new Ui.Choice(Game.t("Inventory").equals("Inventory") ? Tr.t("Inventory") : Game.t("Inventory"), Icons.get("icon_backpack"), () -> showList(true)));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), this::close));
        ui.setChoices(title, c, this::close);
    }

    @SuppressWarnings("unused")
    private static Image noImage() { return null; }
}
