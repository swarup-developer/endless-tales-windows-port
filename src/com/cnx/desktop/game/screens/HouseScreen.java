package com.cnx.desktop.game.screens;

import android.content.Context;
import com.cnx.desktop.game.*;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.data.battles.BattlesIds;
import com.cnx.endlesstalestwo.data.houses.HousesIds;
import com.cnx.endlesstalestwo.entities.*;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.libs.LibInventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Your house (port of ViewHouse: house actions like resting, the storage chest, in-house events)
 * and your mounts (port of ViewMount: choose which mount to ride, saddlebag).
 */
public final class HouseScreen implements Screens.Screen {
    private final HousesIds houseId;   // null = mounts screen

    public HouseScreen(HousesIds houseId) { this.houseId = houseId; }

    private static Context ctx() { return Game.activity; }
    private static Character pc() { return App.getPlayerChar(); }

    @Override
    public void show() {
        Ui ui = Ui.get();
        ui.clearScene();
        ui.showHud(true);
        if (houseId == null) { ui.dimBackground(true); mounts(true); return; }
        House data = App.DataManager.getHouse(houseId);
        if (data == null) { Screens.backToGameplay(); return; }
        ui.setBackground(Images.asset("houses", data.getImageFile()), App.Shell.flowManager.getDayPeriod());
        ui.dimBackground(false);
        Sound.music("inn_house");
        house(true);
        // ViewHouse.checkInHouseEvents()
        App.Shell.flowManager.checkRandomEvents(pc(), App.Shell.flowManager.currentLocation, ctx());
        for (Event e : new ArrayList<>(App.Shell.flowManager.getEventsQueue())) {
            if (e.canHappenInHouse) {
                EventScene.execute(e, () -> house(false));
                App.Shell.flowManager.removeEventFromQueue(e);
            }
        }
    }

    @Override public void refresh() { if (houseId == null) mounts(false); else house(false); }

    // ---------------------------------------------------------------- house

    private House owned() {
        for (House h : pc().ownedHouses) if (h.getUId().equals(houseId)) return h;
        return null;
    }

    private void house(boolean speak) {
        House data = App.DataManager.getHouse(houseId);
        String title = data.getName(App.CURRENT_LANGUAGE);
        String text = data.getDescription(App.CURRENT_LANGUAGE);
        if (speak) Ui.get().say(title, null, text, null); else Ui.get().showTextQuiet(title, null, text);
        List<Ui.Choice> c = Ui.list();
        for (var aid : data.actions) {
            Action a = App.DataManager.getAction(aid);
            if (a != null && a.checkAllRequirements(pc(), ctx()) == Enums.RequirementVerification.OK) {
                c.add(new Ui.Choice(a.getName(App.CURRENT_LANGUAGE), a.getShortDescription(App.CURRENT_LANGUAGE), Images.asset("actions", a.getImageFile()), true, () -> act(a)));
            }
        }
        House mine = owned();
        if (mine != null) {
            c.add(new Ui.Choice(Tr.t("Take from chest"), mine.getChest().size() + Tr.t(" kinds of items"), Icons.get("icon_backpack"), true,
                    () -> transfer(Tr.t("Chest"), mine.getChest(), true, (id, n) -> { mine.removeFromChest(id, n); LibInventory.addToInventory(id, n, (Creature) pc()); }, () -> house(false))));
            c.add(new Ui.Choice(Tr.t("Put in chest"), null, Icons.get("icon_add_item"), true,
                    () -> transfer(Tr.t("Chest"), pc().inventory, false, (id, n) -> { LibInventory.removeFromInventory(id, n, (Creature) pc()); mine.addToChest(id, n); }, () -> house(false))));
        }
        c.add(new Ui.Choice(Tr.t("Leave the house"), Icons.get("icon_back"), Screens::backToGameplay));
        if (!speak) Ui.get().queueMenuAfterSpeech();
        Ui.get().setChoices(title, c, Screens::backToGameplay);
    }

    /** ViewHouse action -> onActionFinish() */
    private void act(Action a) {
        GameEngine.executeAction(a, new Action.UIActionFinishedListener() {
            @Override public void onBattleEncountered(Context c, BattlesIds id) {}
            @Override public void onFinish(Context c, Action action, boolean ok) {
                Ui.get().transition(action.getName(App.CURRENT_LANGUAGE) + "…", 600, () -> {
                    ActionResults r = action.execute(pc(), ctx());
                    if (action.givesRecompense && r != null) {
                        r.giveTo(pc());
                        if (r.finalRecompense.descriptionTranslations.isEmpty()) r.finalRecompense.descriptionTranslations = r.descriptionTranslations;
                        GameEngine.saveGame(ctx());
                        Panels.recompense(action.getName(App.CURRENT_LANGUAGE), r.finalRecompense, () -> house(false));
                    } else {
                        GameEngine.saveGame(ctx());
                        Ui.get().refreshHud();
                        house(false);
                    }
                });
            }
        }, false, ctx());
    }

    // ---------------------------------------------------------------- moving items (chest / saddlebag)

    /**
     * Lists {@code from} and moves the chosen amount with {@code move}. {@code toBag}: items go to
     * your bag (weight is checked, like the Android game).
     */
    private void transfer(String container, List<ItemReference> from, boolean toBag, BiConsumer<com.cnx.endlesstalestwo.data.items.ItemsIds, Integer> move, Runnable back) {
        List<ItemReference> list = new ArrayList<>(from);
        list.removeIf(r -> r == null || r.itemId == null || r.amount <= 0 || r.getItemData() == null);
        list.sort(Comparator.comparing(r -> r.getItemData().getName(App.CURRENT_LANGUAGE)));
        List<Ui.Choice> c = Ui.list();
        for (ItemReference r : list) {
            Item it = r.getItemData();
            c.add(new Ui.Choice(it.getName(App.CURRENT_LANGUAGE) + (r.amount > 1 ? ", " + r.amount : ""), null, Images.asset("items", it.getImageFile()), true, () -> {
                List<Ui.Choice> amounts = Ui.list();
                for (int n : new int[]{1, r.amount}) {
                    if (n == r.amount && r.amount == 1) continue;
                    int amount = n;
                    amounts.add(new Ui.Choice((n == r.amount ? Tr.t("All ") : "") + n, () -> {
                        if (toBag && !pc().haveWeightCapacityForMoreWeigth(amount * it.weight)) { Speech.say(Game.t("notEnoughCap"), true); return; }
                        move.accept(r.itemId, amount);
                        GameEngine.saveGame(ctx());
                        Sound.play("equip_item");
                        Speech.say((toBag ? Tr.t("Took ") : Tr.t("Stored ")) + amount + " " + it.getName(App.CURRENT_LANGUAGE), true);
                        Ui.get().queueMenuAfterSpeech();
                        transfer(container, from, toBag, move, back);
                    }));
                }
                amounts.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), () -> transfer(container, from, toBag, move, back)));
                Ui.get().setChoices(Tr.t("How many ") + it.getName(App.CURRENT_LANGUAGE) + "?", amounts, () -> transfer(container, from, toBag, move, back));
            }));
        }
        if (list.isEmpty()) c.add(new Ui.Choice(toBag ? container + Tr.t(" is empty") : Tr.t("Your bag is empty"), null, null, false, () -> {}));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), back));
        Ui.get().setChoices((toBag ? Tr.t("Take from ") : Tr.t("Put in ")) + container.toLowerCase(), c, back);
    }

    // ---------------------------------------------------------------- mounts (ViewMount)

    private void mounts(boolean speak) {
        Mount cur = pc().currentMount;
        String text = cur == null ? Tr.t("You are not riding a mount.") : Tr.t("Riding ") + cur.getName(App.CURRENT_LANGUAGE) + Tr.t(". Speed ") + cur.getCurrentSpeed()
                + Tr.t(". Saddlebag carries ") + cur.getCarriedWeight() + Tr.t(" of ") + cur.getMaxWeightDisplay() + (cur.isBurdened ? Tr.t(", too heavy") : "") + ".";
        if (pc().ownedMounts.isEmpty()) text = Tr.t("You have no mounts. Stables sell them.");
        if (speak) Ui.get().say(Tr.t("Mounts"), null, text, null); else Ui.get().showTextQuiet(Tr.t("Mounts"), null, text);
        if (cur != null) Ui.get().setBackground(Images.asset("mounts", cur.getImageFile()), null);
        List<Ui.Choice> c = Ui.list();
        for (Mount m : pc().ownedMounts) {
            boolean riding = cur != null && cur.getUId().equals(m.getUId());
            c.add(new Ui.Choice((riding ? Tr.t("Riding: ") : Tr.t("Ride ")) + m.getName(App.CURRENT_LANGUAGE), Tr.t("speed ") + m.getMaxSpeed(), Images.asset("mounts", m.getImageFile()), !riding, () -> {
                Mount fresh = App.DataManager.getMount(m.getUId());
                fresh.setSaddlebag(m.getSaddlebag());
                pc().setMount(fresh);
                GameEngine.saveGame(ctx());
                Speech.say(Tr.t("Now riding ") + m.getName(App.CURRENT_LANGUAGE), true);
                Ui.get().queueMenuAfterSpeech();
                mounts(false);
            }));
        }
        if (cur != null) {
            c.add(new Ui.Choice(Tr.t("Take from saddlebag"), null, Icons.get("icon_backpack"), true,
                    () -> transfer(Tr.t("Saddlebag"), cur.getSaddlebag(), true, (id, n) -> { LibInventory.addToInventory(id, n, (Creature) pc()); cur.removeFromSaddlebag(id, n); }, () -> mounts(false))));
            c.add(new Ui.Choice(Tr.t("Put in saddlebag"), null, Icons.get("icon_add_item"), true, () -> transfer(Tr.t("Saddlebag"), pc().inventory, false, (id, n) -> {
                Item it = App.DataManager.getItem(id);
                if (!cur.haveWeightCapacityForMoreWeigth(n * it.weight)) { Speech.say(Game.t("notEnoughSpace"), true); return; }
                LibInventory.removeFromInventory(id, n, (Creature) pc());
                cur.addToSaddlebag(id, n);
            }, () -> mounts(false))));
        }
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), Screens::backToGameplay));
        if (!speak) Ui.get().queueMenuAfterSpeech();
        Ui.get().setChoices(Tr.t("Mounts"), c, Screens::backToGameplay);
    }
}
