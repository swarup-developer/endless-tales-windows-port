package com.cnx.desktop.game;

import com.cnx.endlesstalestwo.activities.GameplayActivity;
import com.cnx.endlesstalestwo.data.battles.BattlesIds;
import com.cnx.endlesstalestwo.data.houses.HousesIds;
import com.cnx.endlesstalestwo.data.items.ItemsIds;
import com.cnx.endlesstalestwo.data.locations.LocationsIds;
import com.cnx.endlesstalestwo.data.npcs.NpcsIds;
import com.cnx.endlesstalestwo.data.shops.ShopsIds;
import com.cnx.endlesstalestwo.entities.Event;
import com.cnx.endlesstalestwo.enums.Enums;

/**
 * The original GameplayActivity, with every screen change routed to the JavaFX screens.
 * Game scripts find this object through their Context and call these methods.
 */
public class DesktopActivity extends GameplayActivity {

    @Override public void StartBattleScreen(BattlesIds id) { Screens.battle(id); }
    @Override public void goToNpcConversation(NpcsIds id) { Screens.conversation(id); }
    @Override public void executeEventInsideGameplayFragment(Event event) { Screens.event(event, Screens::refreshCurrent); }
    @Override public void travelTo(LocationsIds id) { Screens.explore().travelTo(id); }

    @Override public void goToViewShop(ShopsIds id) { Screens.show(new com.cnx.desktop.game.screens.ShopScreen(id, false)); }
    @Override public void goToViewStable(ShopsIds id) { Screens.show(new com.cnx.desktop.game.screens.ShopScreen(id, true)); }
    @Override public void goToCraft(Enums.CraftTypes type) { Screens.show(new com.cnx.desktop.game.screens.CraftScreen(type)); }
    @Override public void goToViewMap() { Screens.show(new com.cnx.desktop.game.screens.MapScreen()); }
    @Override public void goToViewReader(ItemsIds id) {
        var item = com.cnx.endlesstalestwo.App.DataManager.getItem(id);
        if (item instanceof com.cnx.endlesstalestwo.entities.BookItem b) com.cnx.desktop.game.screens.InventoryScreen.readBook(b, Screens::refreshCurrent);
    }
    @Override public void goToCharacterHouse(HousesIds id) { Screens.show(new com.cnx.desktop.game.screens.HouseScreen(id)); }

    @Override public void openInventory() { Screens.show(new com.cnx.desktop.game.screens.InventoryScreen(false)); }
    @Override public void openEquipments() { Screens.show(new com.cnx.desktop.game.screens.InventoryScreen(true)); }
    @Override public void openQuests() { Screens.characterPanel(); }
    @Override public void openStats() { Screens.characterPanel(); }
    @Override public void openMounts() { Screens.show(new com.cnx.desktop.game.screens.HouseScreen(null)); }
    @Override public void openSkills() { Screens.show(new com.cnx.desktop.game.screens.SkillsScreen()); }
    @Override public void openKnowledges() { Screens.characterPanel(); }

    @Override public void goBack() { Screens.backToGameplay(); }
    @Override public void returnToGameplay() { Screens.backToGameplay(); }
    @Override public void saveExit() { Game.save(); Screens.title(); }
    @Override public void finishActivity() {}
    @Override public void lockScreenTouches(boolean lock) { Ui.get().busy(lock); }
    @Override public void onBackPressed() { Screens.backToGameplay(); }
}
