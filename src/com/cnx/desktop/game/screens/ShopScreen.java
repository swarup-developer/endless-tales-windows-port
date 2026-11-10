package com.cnx.desktop.game.screens;

import android.content.Context;
import com.cnx.desktop.game.*;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.R;
import com.cnx.endlesstalestwo.data.shops.ShopsIds;
import com.cnx.endlesstalestwo.entities.*;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.libs.LibShop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shops and stables (port of the Android ViewShop / ViewStable): buy by item type, sell from your
 * inventory, prices from the game's own LibShop rules (shop modifier and your Charisma).
 */
public final class ShopScreen implements Screens.Screen {
    private final ShopsIds id;
    private final boolean stable;
    private Shop shop;

    public ShopScreen(ShopsIds id, boolean stable) { this.id = id; this.stable = stable; }

    private static Context ctx() { return Game.activity; }
    private static Character pc() { return App.getPlayerChar(); }
    private static String s(int id, Object... a) { try { return a.length == 0 ? Game.activity.getString(id) : Game.activity.getString(id, a); } catch (Throwable t) { return ""; } }

    @Override
    public void show() {
        shop = stable ? App.DataManager.getStable(id) : App.DataManager.getShop(id);
        if (shop == null) { Screens.backToGameplay(); return; }
        Ui ui = Ui.get();
        ui.clearScene();
        ui.showHud(true);
        ui.setBackground(Images.drawable("background_shop") != null ? Images.drawable("background_shop") : Images.asset("locations", App.Shell.flowManager.currentLocation.getImageFile()), null);
        ui.dimBackground(false);
        if (shop.musicFile != null && !shop.musicFile.isEmpty()) Sound.music(shop.musicFile);
        if (stable) showMounts(true); else showMain(true);
    }

    @Override public void refresh() { if (stable) showMounts(false); else showMain(false); }

    private String keeper() {
        String k = shop.shopKeeperName;
        return k == null || k.isBlank() ? null : Tr.t("Keeper: ") + k;
    }

    private void leave() {
        App.Shell.flowManager.shouldUpdateGameflow = true;
        Screens.backToGameplay();
    }

    // ---------------------------------------------------------------- shop

    private void showMain(boolean speak) {
        String title = shop.getName(App.CURRENT_LANGUAGE);
        String text = Tr.t("You have ") + pc().gold + Tr.t(" gold.");
        if (speak) Ui.get().say(title, keeper(), text, null); else Ui.get().showTextQuiet(title, keeper(), text);
        List<Ui.Choice> c = Ui.list();
        List<Enums.ItemType> types = new ArrayList<>();
        for (ItemReference r : shop.items) {
            Item it = App.DataManager.getItem(r.itemId);
            if (it != null && !types.contains(it.type)) types.add(it.type);
        }
        types.sort(Comparator.comparing(ShopScreen::typeName));
        for (Enums.ItemType t : types) c.add(new Ui.Choice(Tr.t("Buy ") + typeName(t).toLowerCase(), null, Icons.get("icon_purchase"), true, () -> showBuy(t)));
        c.add(new Ui.Choice(Tr.t("Sell"), Tr.t("your items"), Icons.get("icon_coins"), true, this::showSell));
        c.add(new Ui.Choice(Tr.t("Leave"), Icons.get("icon_back"), this::leave));
        if (!speak) Ui.get().queueMenuAfterSpeech();
        Ui.get().setChoices(title, c, this::leave);
    }

    private static String typeName(Enums.ItemType t) {
        if (t == null) return "";
        String tr = Game.t(t.name());
        return tr.equals(t.name()) ? ExploreScreen.pretty(t.name()) : tr;
    }

    /** ViewShop.prepareItemsToBuyByType() */
    private void showBuy(Enums.ItemType type) {
        List<Item> list = new ArrayList<>();
        for (ItemReference r : shop.items) {
            Item it = App.DataManager.getItem(r.itemId);
            if (it == null || it.type != type || !LibShop.isTradable(it, Enums.ShopMode.BUY)) continue;
            LibShop.calculateItemPrice(it, shop.priceModifier, pc().attributesManager.getCurrent(Enums.AttributeName.CHARISMA));
            boolean classOk = it.classes == null || it.classes.isEmpty() || it.classes.contains(pc().classId);
            if ((classOk && it.requirementValidations == null) || it.checkRequirements(pc(), ctx()) == Enums.RequirementVerification.OK) list.add(it);
        }
        list.sort(Comparator.comparing(it -> it.getName(App.CURRENT_LANGUAGE)));
        List<Ui.Choice> c = Ui.list();
        for (Item it : list) {
            int price = (int) it.buyingPrice;
            c.add(new Ui.Choice(it.getName(App.CURRENT_LANGUAGE), price + Tr.t(" gold") + (price > pc().gold ? Tr.t(", too expensive") : ""),
                    Images.asset("items", it.getImageFile()), true, () -> showItem(it, Enums.ShopMode.BUY, () -> showBuy(type))));
        }
        if (list.isEmpty()) c.add(new Ui.Choice(Tr.t("Nothing for you here"), null, null, false, () -> {}));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), () -> showMain(false)));
        Ui.get().setChoices(Tr.t("Buy ") + typeName(type).toLowerCase() + Tr.t(", you have ") + pc().gold + Tr.t(" gold"), c, () -> showMain(false));
    }

    /** ViewShop.prepareCharacterSellableItems() */
    private void showSell() {
        List<Item> list = new ArrayList<>();
        boolean onlyShopTypes = shop.itemsTypes != null && !shop.itemsTypes.isEmpty();
        for (ItemReference r : pc().inventory) {
            Item it = r.getItemData();
            if (it == null || (onlyShopTypes && !shop.itemsTypes.contains(it.type)) || !LibShop.isTradable(it, Enums.ShopMode.SELL)) continue;
            LibShop.calculateItemPrice(it, shop.priceModifier, pc().attributesManager.getCurrent(Enums.AttributeName.CHARISMA));
            if (it.sellingPrice > 0) list.add(it);
        }
        list.sort(Comparator.comparing(it -> it.getName(App.CURRENT_LANGUAGE)));
        List<Ui.Choice> c = Ui.list();
        for (Item it : list) {
            int have = com.cnx.endlesstalestwo.libs.LibInventory.howMuchItemHave(it.getUId(), pc());
            c.add(new Ui.Choice(it.getName(App.CURRENT_LANGUAGE) + (have > 1 ? Tr.t(", you have ") + have : ""), (int) it.sellingPrice + Tr.t(" gold each"),
                    Images.asset("items", it.getImageFile()), true, () -> showItem(it, Enums.ShopMode.SELL, this::showSell)));
        }
        if (list.isEmpty()) c.add(new Ui.Choice(Tr.t("This shop buys nothing you carry"), null, null, false, () -> {}));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), () -> showMain(false)));
        Ui.get().setChoices(Tr.t("Sell, you have ") + pc().gold + Tr.t(" gold"), c, () -> showMain(false));
    }

    private void showItem(Item it, Enums.ShopMode mode, Runnable back) {
        int price = (int) (mode == Enums.ShopMode.BUY ? it.buyingPrice : it.sellingPrice);
        int have = com.cnx.endlesstalestwo.libs.LibInventory.howMuchItemHave(it.getUId(), pc());
        String text = InventoryScreen.describe(it, 1, false) + " " + (mode == Enums.ShopMode.BUY ? Tr.t("Price ") : Tr.t("Sells for ")) + price + Tr.t(" gold. You have ") + have + ".";
        Ui.get().say(it.getName(App.CURRENT_LANGUAGE), null, text, null);
        List<Ui.Choice> c = Ui.list();
        int max = mode == Enums.ShopMode.BUY ? (price <= 0 ? 10 : Math.max(0, Math.min(99, pc().gold / price))) : have;
        String verb = mode == Enums.ShopMode.BUY ? Tr.t("Buy") : Tr.t("Sell");
        for (int n : new int[]{1, 5, 10}) {
            if (n > 1 && n > max) continue;
            c.add(new Ui.Choice(verb + " " + n + Tr.t(" for ") + (price * n) + Tr.t(" gold"), null, null, n <= Math.max(1, max), () -> trade(it, n, mode, back)));
        }
        if (max > 10) c.add(new Ui.Choice(verb + " " + max + Tr.t(" for ") + (price * max) + Tr.t(" gold"), () -> trade(it, max, mode, back)));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), back));
        Ui.get().setChoices(it.getName(App.CURRENT_LANGUAGE), c, back);
    }

    /** ViewShop.executeTrade() */
    private void trade(Item it, int amount, Enums.ShopMode mode, Runnable back) {
        int price = (int) (mode == Enums.ShopMode.SELL ? it.sellingPrice : it.buyingPrice);
        Enums.TradeResults r = LibShop.doTrade(it, amount, mode, ctx());
        switch (r) {
            case NO_GOLD -> Speech.say(s(R.string.notEnoughGold), true);
            case NO_GEMS -> Speech.say(s(R.string.notEnoughGems), true);
            case NO_WEIGHT_CAP -> Speech.say(s(R.string.notEnoughCap), true);
            case DONT_OWN_ITEM -> Speech.say(s(R.string.dontHaveItemAnymore), true);
            case CORRECT -> {
                Sound.play("gold_trade");
                String msg = mode == Enums.ShopMode.BUY
                        ? s(R.string.BoughtXItem, String.valueOf(amount), it.getName(App.CURRENT_LANGUAGE), String.valueOf(price * amount))
                        : s(R.string.SoldXItem, String.valueOf(amount), it.getName(App.CURRENT_LANGUAGE), String.valueOf(price * amount));
                GameEngine.saveGame(ctx());
                Ui.get().refreshHud();
                Speech.say(msg + Tr.t(". You have ") + pc().gold + Tr.t(" gold."), true);
                Ui.get().queueMenuAfterSpeech();
                back.run();
            }
            default -> Speech.say(Tr.t("Trade not possible"), true);
        }
    }

    // ---------------------------------------------------------------- stable (ViewStable)

    private void showMounts(boolean speak) {
        Stable st = (Stable) shop;
        String title = st.getName(App.CURRENT_LANGUAGE);
        String text = Tr.t("You have ") + pc().gold + Tr.t(" gold.");
        if (speak) Ui.get().say(title, keeper(), text, null); else Ui.get().showTextQuiet(title, keeper(), text);
        List<Ui.Choice> c = Ui.list();
        for (var mid : st.mountsIds) {
            Mount m = App.DataManager.getMount(mid);
            if (m == null) continue;
            boolean owned = pc().ownedMounts.stream().anyMatch(o -> o.getUId().equals(m.getUId()));
            String detail = owned ? Tr.t("you own this mount") : m.price + Tr.t(" gold") + (m.gemPrice > 0 ? Tr.t(" and ") + m.gemPrice + Tr.t(" gems") : "")
                    + Tr.t(", speed ") + m.getMaxSpeed() + Tr.t(", carries ") + m.getMaxWeightDisplay();
            c.add(new Ui.Choice(m.getName(App.CURRENT_LANGUAGE), detail, Images.asset("mounts", m.getImageFile()), !owned, () -> buyMount(m)));
        }
        c.add(new Ui.Choice(Tr.t("Leave"), Icons.get("icon_back"), this::leave));
        if (!speak) Ui.get().queueMenuAfterSpeech();
        Ui.get().setChoices(title, c, this::leave);
    }

    /** ViewStable.executeTrade() */
    private void buyMount(Mount m) {
        Enums.TradeResults r = LibShop.buyMount(m, ctx());
        switch (r) {
            case NO_GOLD -> Speech.say(s(R.string.notEnoughGold), true);
            case NO_GEMS -> Speech.say(s(R.string.notEnoughGems), true);
            case NO_WEIGHT_CAP -> Speech.say(s(R.string.notEnoughCap), true);
            case DONT_OWN_ITEM -> Speech.say(s(R.string.dontHaveItemAnymore), true);
            case CORRECT -> {
                pc().removeGold(m.price);
                if (m.gemPrice > 0 && App.Shell.getAccount() != null) App.Shell.getAccount().removeGems(m.gemPrice);
                pc().addMount(m);
                GameEngine.saveGame(ctx());
                Sound.play("gold_trade");
                Ui.get().refreshHud();
                Speech.say(s(R.string.BoughtXItem, "1", m.getName(App.CURRENT_LANGUAGE), String.valueOf(m.price)), true);
                Ui.get().queueMenuAfterSpeech();
                showMounts(false);
            }
            default -> Speech.say(Tr.t("Not possible"), true);
        }
    }
}
