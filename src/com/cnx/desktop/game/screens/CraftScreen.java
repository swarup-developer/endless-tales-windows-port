package com.cnx.desktop.game.screens;

import android.content.Context;
import com.cnx.desktop.game.*;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.entities.*;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.entities.Constants;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.libs.LibCraft;
import com.cnx.endlesstalestwo.libs.LibInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Crafting, alchemy and cooking (port of the Android ViewCraft): put up to three ingredients in
 * the slots; when they match a recipe the result, success chance and time are announced; Craft
 * uses the game's own LibCraft (success roll, skill increase).
 */
public final class CraftScreen implements Screens.Screen {
    private final Enums.CraftTypes type;
    private final ItemReference[] slots = new ItemReference[3];
    private final List<ItemReference> virtual = new ArrayList<>();
    private final List<CraftRecipe> recipes = new ArrayList<>();
    private CraftRecipe recipe;
    private Item result;
    private int skillXp, skillToNext;

    public CraftScreen(Enums.CraftTypes type) { this.type = type == null ? Enums.CraftTypes.GENERAL : type; }

    private static Context ctx() { return Game.activity; }
    private static Character pc() { return App.getPlayerChar(); }

    private String title() {
        return switch (type) {
            case ALCHEMY -> Tr.t("Alchemy");
            case COOKING -> Tr.t("Cooking");
            default -> Tr.t("Crafting");
        };
    }

    @Override
    public void show() {
        Ui ui = Ui.get();
        ui.clearScene();
        ui.showHud(true);
        String bg = type == Enums.CraftTypes.ALCHEMY ? "background_alchemy" : type == Enums.CraftTypes.COOKING ? "background_cooking" : "background_craft";
        ui.setBackground(Images.drawable(bg), null);
        ui.dimBackground(true);
        Sound.play(type == Enums.CraftTypes.ALCHEMY ? "craft_alchemy" : type == Enums.CraftTypes.COOKING ? "craft_cooking" : "craft_general");
        recipes.clear();
        for (CraftRecipe r : App.DataManager.getCrafts()) if (r.type == type) recipes.add(r);
        refreshVirtual();
        int craft = pc().attributesManager.getCraft(false);
        skillXp = craft % 100;
        skillToNext = craft % 100 == 0 ? 100 : 100 - craft % 100;
        Ui.get().say(title(), null, Tr.t("Choose up to three ingredients. When they make something, its name and chance of success are read. ")
                + Tr.t("Your skill adds ") + extraRate() + Tr.t(" percent."), null);
        menu(false);
    }

    @Override public void refresh() { menu(true); }

    private int extraRate() { return LibCraft.getCurrentSkillForSuccessRate(type) * Constants.PER_CRAFT_SKILL_SUCCESS_RATE_INCREASE_PERC; }

    private void refreshVirtual() {
        virtual.clear();
        for (ItemReference r : pc().inventory) {
            ItemReference copy = r.getItemData().asReference();
            copy.amount = r.amount;
            virtual.add(copy);
        }
        for (ItemReference s : slots) if (s != null) LibInventory.removeFromInventory(s.itemId, 1, new ArrayList<>(virtual));
    }

    private void menu(boolean speak) {
        verifyRecipe();
        List<Ui.Choice> c = Ui.list();
        for (int i = 0; i < 3; i++) {
            int slot = i;
            ItemReference r = slots[i];
            String label = Tr.t("Slot ") + (i + 1) + ": " + (r == null ? Tr.t("empty") : r.getItemData().getName(App.CURRENT_LANGUAGE));
            c.add(new Ui.Choice(label, r == null ? Tr.t("Enter to choose an ingredient") : Tr.t("Enter to take it out"),
                    r == null ? Icons.get("icon_plus") : Images.asset("items", r.getItemData().getImageFile()), true, () -> tapSlot(slot)));
        }
        String what;
        if (result != null) {
            int rate = Math.min(100, recipe.successRate);
            what = Tr.t("Makes ") + result.getName(App.CURRENT_LANGUAGE) + ", " + rate + Tr.t(" percent chance plus ") + extraRate() + Tr.t(" from skill, takes ") + recipe.productionTime + Tr.t(" minutes");
            c.add(new Ui.Choice(type == Enums.CraftTypes.COOKING ? Tr.t("Cook") : type == Enums.CraftTypes.ALCHEMY ? Tr.t("Mix") : Tr.t("Craft"), what, Icons.get("icon_craft"), true, this::craft));
        } else {
            what = slotsEmpty() ? Tr.t("No ingredients yet") : Tr.t("These ingredients make nothing");
        }
        c.add(new Ui.Choice(Tr.t("Clear all slots"), null, Icons.get("icon_discard"), !slotsEmpty(), () -> { clear(); menu(true); }));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), this::leave));
        Ui.get().showTextQuiet(title(), null, what + ".");
        if (speak) Speech.queue(what);
        Ui.get().queueMenuAfterSpeech();
        Ui.get().setChoices(title(), c, this::leave);
    }

    private boolean slotsEmpty() { return slots[0] == null && slots[1] == null && slots[2] == null; }

    private void clear() { slots[0] = slots[1] = slots[2] = null; refreshVirtual(); }

    private void leave() { clear(); Screens.backToGameplay(); }

    /** ViewCraft.onTapItem() */
    private void tapSlot(int i) {
        if (slots[i] != null) {
            slots[i] = null;
            refreshVirtual();
            Speech.say(Tr.t("Ingredient removed"), true);
            menu(true);
            return;
        }
        List<Ui.Choice> c = Ui.list();
        List<ItemReference> list = new ArrayList<>();
        for (ItemReference r : available()) if (r.getItemData().canBeIngredient && r.amount > 0) list.add(r);
        list.sort((a, b) -> a.getItemData().getName(App.CURRENT_LANGUAGE).compareToIgnoreCase(b.getItemData().getName(App.CURRENT_LANGUAGE)));
        for (ItemReference r : list) {
            c.add(new Ui.Choice(r.getItemData().getName(App.CURRENT_LANGUAGE) + (r.amount > 1 ? ", " + r.amount : ""), null,
                    Images.asset("items", r.getItemData().getImageFile()), true, () -> {
                        slots[i] = r.getItemData().asReference();
                        menu(true);
                    }));
        }
        if (list.isEmpty()) c.add(new Ui.Choice(Tr.t("You have no ingredients"), null, null, false, () -> {}));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), () -> menu(true)));
        Ui.get().setChoices(Tr.t("Choose an ingredient for slot ") + (i + 1), c, () -> menu(true));
    }

    /** Inventory minus what is already in the slots. */
    private List<ItemReference> available() {
        List<ItemReference> out = new ArrayList<>();
        for (ItemReference r : pc().inventory) {
            ItemReference copy = r.getItemData().asReference();
            copy.amount = r.amount;
            for (ItemReference s : slots) if (s != null && s.itemId == r.itemId) copy.amount--;
            out.add(copy);
        }
        return out;
    }

    /** ViewCraft.verifyRecipe(): the slots must hold exactly a recipe's ingredients. */
    private void verifyRecipe() {
        recipe = null;
        result = null;
        List<Object> have = new ArrayList<>();
        for (ItemReference s : slots) if (s != null) have.add(s.itemId);
        if (have.isEmpty()) return;
        for (CraftRecipe r : recipes) {
            if (r.ingredients.size() != have.size()) continue;
            List<Object> left = new ArrayList<>(have);
            boolean ok = true;
            for (ItemReference ing : r.ingredients) {
                if (!left.remove(ing.itemId)) { ok = false; break; }
            }
            if (ok) { recipe = r; result = App.DataManager.getItem(r.craftedItemId); return; }
        }
    }

    /** ViewCraft.onTapCraft() */
    private void craft() {
        if (result == null) return;
        String name = result.getName(App.CURRENT_LANGUAGE);
        Item made = LibCraft.doCraft(pc(), recipe, false, ctx());
        if (made != null) {
            skillXp += LibCraft.increaseSkill(type, true);
            if (recipe.soundFile != null && !recipe.soundFile.isEmpty()) Sound.play(recipe.soundFile);
            Ui.get().message(title(), Tr.t("Success! You made ") + name + ".", Images.asset("items", result.getImageFile()), null);
        } else {
            Sound.play("craft_fail");
            skillXp += LibCraft.increaseSkill(type, false);
            Ui.get().message(title(), Tr.t("It failed. ") + name + Tr.t(" was not made, and the ingredients were used."), null, null);
        }
        GameEngine.saveGame(ctx());
        finishProduction();
        Ui.get().refreshHud();
    }

    /** ViewCraft.finishProduction(): keep ingredients you still have, check for a skill level up. */
    private void finishProduction() {
        for (int i = 0; i < 3; i++) {
            if (slots[i] == null) continue;
            int have = LibInventory.howMuchItemHave(slots[i].itemId, pc());
            int used = 0;
            for (int j = 0; j < i; j++) if (slots[j] != null && slots[j].itemId == slots[i].itemId) used++;
            if (have <= used) slots[i] = null;
        }
        if (skillXp >= skillToNext && (type == Enums.CraftTypes.GENERAL || type == Enums.CraftTypes.ALCHEMY)) {
            int craft = pc().attributesManager.getCraft(false);
            GameEngine.showCraftSkillLevelUpDialog(type, craft / 10 - 1, craft / 10, ctx());
            skillXp = craft % 100;
            skillToNext = craft % 100 == 0 ? 100 : 100 - craft % 100;
        }
        menu(false);
    }
}
