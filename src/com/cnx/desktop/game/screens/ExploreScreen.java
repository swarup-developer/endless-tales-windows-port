package com.cnx.desktop.game.screens;

import com.cnx.desktop.game.*;
import com.cnx.desktop.game.Nav;
import com.cnx.desktop.game.Speech;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.data.battles.BattlesIds;
import com.cnx.endlesstalestwo.data.locations.LocationsIds;
import com.cnx.endlesstalestwo.entities.*;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.interfaces.TravelCallback;
import com.cnx.endlesstalestwo.libs.LibLocationsTravel;
import com.cnx.endlesstalestwo.libs.LibQuest;
import android.content.Context;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The main gameplay screen (port of the Android ViewGameplay): the location art fills the screen,
 * its description is in the text box and the things you can do are in the choice menu.
 */
public final class ExploreScreen implements Screens.Screen {
    private Location shownLocation;
    private boolean processedEvents;
    private boolean skipRandomBattle;
    private boolean actionCanCauseRandomBattle = true;
    private final TravelCallback travelCallback = () -> updateView();

    private static Context ctx() { return Game.activity; }

    @Override
    public void show() {
        Ui ui = Ui.get();
        ui.clearScene();
        ui.dimBackground(false);
        ui.showHud(true);
        actionCanCauseRandomBattle = true;
        // ViewGameplay.onResume()
        if (App.Shell.isCorrupted()) { corrupted(); return; }
        App.Shell.flowManager.updateGameplayFlow(ctx());
        skipRandomBattle = App.Shell.flowManager.currentBattleId != null && !App.Shell.flowManager.isPlayerBattling;
        updateView();
    }

    @Override
    public void refresh() {
        Ui.get().clearScene();
        Ui.get().dimBackground(false);
        Ui.get().showHud(true);
        updateView();
    }

    private void corrupted() {
        Log.warn("Game state flagged as corrupted - returning to title");
        Ui.get().message(Tr.t("Error"), Tr.t("The game data reported an error and the game must return to the title screen.\n")
                + Tr.t("Your last save is kept."), null, Screens::title);
    }

    /** ViewGameplay.updateView() */
    void updateView() {
        Ui.get().busy(false);
        if (App.Shell.isCorrupted()) { corrupted(); return; }
        Character pc = App.getPlayerChar();
        App.Shell.flowManager.updateCharHungry(pc, 0);
        if (App.Shell.flowManager.currentLocation == null && App.Shell.flowManager.currentLocationId != null) {
            App.Shell.flowManager.setCurrentLocation(App.Shell.flowManager.currentLocationId);
        }
        Location loc = App.Shell.flowManager.currentLocation;
        if (loc == null) { corrupted(); return; }
        if (shownLocation == null || shownLocation.getUId() != loc.getUId()) {
            shownLocation = loc;
            processedEvents = false;
        }
        updateGameplay();
    }

    /** ViewGameplay.updateGameplay() */
    private void updateGameplay() {
        Location loc = App.Shell.flowManager.currentLocation;
        Character pc = App.getPlayerChar();
        showLocation(loc);
        Ui.get().refreshHud();

        if (pc.isDead) {
            App.Shell.flowManager.currentBattleId = null;
            App.Shell.flowManager.isPlayerBattling = false;
            loc.checkedEventsAndBattles = true;
            GameEngine.charDeath(true, ctx()); // shows the death panel (Hooks), which restores and refreshes
            return;
        }
        playLocationMusic(loc);

        if (App.Shell.flowManager.currentBattleId != null && !App.Shell.flowManager.isPlayerBattling) {
            App.Shell.flowManager.isPlayerBattling = true;
            BattlesIds id = App.Shell.flowManager.currentBattleId;
            if (App.DataManager.getBattle(id) != null) Game.activity.StartBattleScreen(id);
            return;
        }

        boolean eventsShown = checkEvents();
        checkCompletedQuestsQueue();
        if (!App.Shell.flowManager.shouldCheckRandomBattle) skipRandomBattle = true;

        Battle battle = null;
        if (!processedEvents && !loc.checkedEventsAndBattles && !skipRandomBattle) {
            battle = App.Shell.flowManager.checkRandomEncounter(pc, false, ctx());
        }
        if (battle == null) {
            GameEngine.checkCharLevel(pc, ctx());
            App.Shell.flowManager.checkActiveWorldEvents(ctx());
            if (!eventsShown) showMainMenu();
            GameEngine.saveGame(ctx());
            skipRandomBattle = false;
            App.Shell.flowManager.shouldCheckRandomBattle = true;
        } else {
            GameEngine.saveGame(ctx());
            Game.activity.StartBattleScreen(battle.uId);
        }
        loc.checkedEventsAndBattles = true;
    }

    private void showLocation(Location loc) {
        Image art = Images.asset("locations", loc.getImageFile());
        Ui.get().setBackground(art, App.Shell.flowManager.getDayPeriod());
        Ui.get().setPortrait(null, true);
        String name = loc.getName(App.CURRENT_LANGUAGE);
        String period = App.Shell.flowManager.isDayLight ? "" : Tr.t(" It is night.");
        // full description when arriving; just the place name when coming back (R repeats the full text)
        boolean arrived = loc != announced; // set once the menu is shown (an event may cut the description off)
        String spoken = arrived
                ? name + ". " + regionLine(loc) + "." + period + " " + loc.getDescription(App.CURRENT_LANGUAGE)
                : name + "." + period;
        Ui.get().say(name, regionLine(loc), loc.getDescription(App.CURRENT_LANGUAGE), null, spoken);
        Ui.get().setSceneSpeech(name + ". " + regionLine(loc) + "." + period + " " + loc.getDescription(App.CURRENT_LANGUAGE));
    }

    private Location announced;

    private static String regionLine(Location loc) {
        try {
            Region r = App.DataManager.getRegion(loc.regionId);
            String type = loc.type == null ? "" : Game.t(loc.type.name());
            String region = r == null ? "" : r.getName(App.CURRENT_LANGUAGE);
            if (type.equals(loc.type == null ? "" : loc.type.name())) type = pretty(loc.type == null ? "" : loc.type.name());
            return region.isEmpty() ? type : region + "  ·  " + type;
        } catch (Throwable t) {
            return "";
        }
    }

    /** A game enum (FOOD, CHEST, NORTH ...) as words in the current language: the game's own text if it has one. */
    static String pretty(String enumName) {
        if (enumName == null || enumName.isEmpty()) return "";
        String own = Game.t(enumName);
        if (own != null && !own.isBlank() && !own.equals(enumName)) return own;
        String s = enumName.replace('_', ' ').toLowerCase();
        return Tr.t(java.lang.Character.toUpperCase(s.charAt(0)) + s.substring(1));
    }

    private void playLocationMusic(Location loc) {
        try {
            android.content.res.AssetFileDescriptor music = loc.getIdMusic(ctx());
            if (music != null) App.Shell.playMusic(music, ctx());
            else if (App.Shell.currentPlayingMusicId < 0) Sound.music("game_theme");
            else App.Shell.resumeMusic();
        } catch (Throwable t) {
            Log.warn("Music: " + t);
        }
    }

    /** ViewGameplay.checkEvents(); returns true when an event scene was opened. */
    private boolean checkEvents() {
        if (processedEvents || App.Shell.flowManager.currentLocation.checkedEventsAndBattles) return false;
        App.Shell.flowManager.checkRandomEvents(App.getPlayerChar(), App.Shell.flowManager.currentLocation, ctx());
        if (App.Shell.flowManager.getEventsQueue().isEmpty() || !App.Shell.flowManager.shouldCheckEvents) return false;
        processedEvents = true;
        boolean any = false;
        for (Event event : new ArrayList<>(App.Shell.flowManager.getEventsQueue())) {
            if (event.possibleLocales.isEmpty() || event.possibleLocales.contains(App.Shell.flowManager.currentLocationId)
                    || event.possiblePlaces.contains(App.Shell.flowManager.currentLocationId)) {
                EventScene.execute(event, this::refresh);
                any = true;
            }
            App.Shell.flowManager.removeEventFromQueue(event);
        }
        return any && Screens.eventOpen();
    }

    /** ViewGameplay.checkCompletedQuestsQueue() */
    private void checkCompletedQuestsQueue() {
        boolean add = false;
        for (Quest q : App.getPlayerChar().quests) {
            if (q.status != Enums.QuestStatus.COMPLETED && !q.isRestartable && q.endStatus == Enums.QuestTerminationStatus.FINISHED) {
                App.Shell.flowManager.addCompletedQuestToQueue(q, ctx());
                add = true;
            }
        }
        List<Quest> done = new ArrayList<>(App.Shell.flowManager.getCompletedQuestsQueue());
        for (Quest q : done) {
            if (q.status != Enums.QuestStatus.COMPLETED || q.endStatus != Enums.QuestTerminationStatus.FINISHED) {
                LibQuest.completeQuest(q, App.getPlayerChar(), q.questParts.size(), ctx());
            }
            Panels.questCompleted(q);
            App.Shell.flowManager.removeCompletedQuestFromQueue(q);
        }
        if (add || !done.isEmpty()) Ui.get().refreshHud();
    }

    // ---------------------------------------------------------------- menus

    private boolean isPlace() { return App.Shell.flowManager.currentLocation instanceof Place; }

    /** ViewGameplay.controlButtonsDisplay() as a choice menu. */
    void showMainMenu() {
        Location loc = App.Shell.flowManager.currentLocation;
        announced = loc;
        List<Ui.Choice> c = Ui.list();
        if (!loc.getAllowedNpcs(ctx()).isEmpty()) c.add(new Ui.Choice(Game.t("Persons"), Icons.talk(), this::menuPeople));
        if (!loc.getAllowedActions(ctx()).isEmpty()) c.add(new Ui.Choice(Game.t("Actions"), Icons.get("icon_action"), this::menuActions));
        if (loc.places != null && !loc.places.isEmpty()) c.add(new Ui.Choice(Game.t("Places"), Icons.get("icon_place"), this::menuPlaces));
        if (!isPlace() && loc instanceof Locale l && (!l.shops.isEmpty() || !l.stables.isEmpty()))
            c.add(new Ui.Choice(Game.t("Shops"), Icons.get("icon_shop_general"), this::menuShops));
        for (House h : App.getPlayerChar().ownedHouses) {
            if (h.location != null && h.location.equals(loc.getUId())) {
                c.add(new Ui.Choice(Game.t("Houses"), h.getName(App.CURRENT_LANGUAGE), Icons.get("icon_house"), true, () -> Game.activity.goToCharacterHouse(h.getUId())));
                break;
            }
        }
        if (!isPlace() && loc instanceof Locale l && l.destinationLocales != null && !l.destinationLocales.isEmpty())
            c.add(new Ui.Choice(Game.t("destinations"), Icons.get("icon_map"), this::menuTravel));
        if (isPlace()) c.add(new Ui.Choice(Game.t("Return"), Icons.get("icon_back"), this::leavePlace));
        if (!isPlace()) {
            Region region = App.DataManager.getRegion(loc.regionId);
            if (region != null && region.mapItemUid != null && com.cnx.endlesstalestwo.libs.LibInventory.checkHasItemAmount(region.mapItemUid, 1, App.getPlayerChar()))
                c.add(new Ui.Choice(Tr.t("Map"), Icons.get("icon_map_pin"), () -> Game.activity.goToViewMap()));
        }
        c.add(new Ui.Choice(App.getPlayerChar().getName(), Tr.t("character, inventory, skills"), Icons.get("icon_stats"), true, this::menuCharacter));
        c.add(new Ui.Choice(Game.t("Menu").equals("Menu") ? Tr.t("Menu") : Game.t("Menu"), Icons.get("icon_options"), Panels::gameMenu));
        Ui.get().setChoices(null, c);
    }

    /** The character menu (UICharacterMenu): sheet, inventory, equipment, skills, mounts. */
    private void menuCharacter() {
        List<Ui.Choice> c = Ui.list();
        c.add(new Ui.Choice(Tr.t("Character sheet"), Tr.t("stats and quests"), Icons.get("icon_stats"), true, Screens::characterPanel));
        c.add(new Ui.Choice(Game.t("Inventory").equals("Inventory") ? Tr.t("Inventory") : Game.t("Inventory"), Icons.get("icon_backpack"), () -> Game.activity.openInventory()));
        c.add(new Ui.Choice(Game.t("Equipment").equals("Equipment") ? Tr.t("Equipment") : Game.t("Equipment"), Icons.get("icon_equipments"), () -> Game.activity.openEquipments()));
        c.add(new Ui.Choice(Game.t("Skills").equals("Skills") ? Tr.t("Skills") : Game.t("Skills"), App.getPlayerChar().skillPoints > 0 ? App.getPlayerChar().skillPoints + Tr.t(" skill points to spend") : null,
                Icons.get("icon_skills"), true, () -> Game.activity.openSkills()));
        if (!App.getPlayerChar().ownedMounts.isEmpty()) c.add(new Ui.Choice(Tr.t("Mounts"), Icons.get("icon_mounts"), () -> Game.activity.openMounts()));
        c.add(back());
        Ui.get().setChoices(App.getPlayerChar().getName(), c, this::showMainMenu);
    }

    private void menuPeople() {
        List<Ui.Choice> c = Ui.list();
        for (Npc npc : App.Shell.flowManager.currentLocation.getAllowedNpcs(ctx())) {
            String job = "";
            try { job = com.cnx.endlesstalestwo.libs.LibNpc.getJobName(npc.job, ctx()); } catch (Throwable ignored) {}
            c.add(new Ui.Choice(npc.getName(App.CURRENT_LANGUAGE), job, Images.asset("npcs", npc.getImageFile()), true,
                    () -> Game.activity.goToNpcConversation(npc.getUId())));
        }
        c.add(back());
        Ui.get().setChoices(Game.t("Persons"), c, this::showMainMenu);
    }

    private void menuActions() {
        List<Ui.Choice> c = Ui.list();
        for (Action a : App.Shell.flowManager.currentLocation.getAllowedActions(ctx())) {
            String d = a.getShortDescription(App.CURRENT_LANGUAGE);
            c.add(new Ui.Choice(a.getName(App.CURRENT_LANGUAGE), d, Images.asset("actions", a.getImageFile()), true, () -> runAction(a)));
        }
        c.add(back());
        Ui.get().setChoices(Game.t("Actions"), c, this::showMainMenu);
    }

    private void menuPlaces() {
        Location loc = App.Shell.flowManager.currentLocation;
        List<Ui.Choice> c = Ui.list();
        List<Place> places = new ArrayList<>();
        for (LocationsIds id : loc.places) {
            Place p = App.DataManager.getPlace(id);
            if (p != null) places.add(p);
        }
        places.sort(Comparator.comparing(p -> p.getName(App.CURRENT_LANGUAGE)));
        for (Place p : places) {
            boolean can = LibLocationsTravel.canGoToLocation(p, ctx());
            if (!can && !p.alwaysShow) continue;
            c.add(new Ui.Choice(p.getName(App.CURRENT_LANGUAGE), can ? pretty(p.type == null ? "" : p.type.name()) : Game.t("notAvailable"),
                    Icons.locationType(p.type), can, () -> travelToLocation(p.getUId(), true)));
        }
        c.add(back());
        Ui.get().setChoices(Game.t("Places"), c, this::showMainMenu);
    }

    private void menuTravel() {
        Locale here = (Locale) App.Shell.flowManager.currentLocation;
        List<LocationTravelReference> refs = new ArrayList<>();
        for (LocationTravelReference r : here.destinationLocales) {
            Locale dest = App.DataManager.getLocale(r.toLocationId);
            if (dest == null) continue;
            if (r.destinationName == null || r.destinationName.isEmpty()) r.destinationName = dest.getName(App.CURRENT_LANGUAGE);
            boolean can = LibLocationsTravel.canGoToLocation(dest, ctx());
            r.disabled = !can;
            if (can || dest.alwaysShow) refs.add(r);
        }
        refs.sort(Comparator.comparing(r -> r.destinationName));
        List<Ui.Choice> c = Ui.list();
        for (LocationTravelReference r : refs) {
            int minutes = GameEngine.calculateTravelTime(r);
            String dir = r.direction == null ? "" : pretty(r.direction.name()) + "  ·  ";
            Locale dest = App.DataManager.getLocale(r.toLocationId);
            c.add(new Ui.Choice(r.destinationName, dir + formatMinutes(minutes), Icons.locationType(dest.type), !r.disabled,
                    () -> travelToLocation(r.toLocationId, false)));
        }
        c.add(back());
        Ui.get().setChoices(Game.t("destinations"), c, this::showMainMenu);
    }

    private void menuShops() {
        Locale l = (Locale) App.Shell.flowManager.currentLocation;
        List<Ui.Choice> c = Ui.list();
        for (var id : l.shops) {
            Shop s = App.DataManager.getShop(id);
            if (s != null) c.add(new Ui.Choice(s.getName(App.CURRENT_LANGUAGE), Icons.get("icon_shop_general"), () -> Game.activity.goToViewShop(s.getUId())));
        }
        for (var id : l.stables) {
            Stable s = App.DataManager.getStable(id);
            if (s != null) c.add(new Ui.Choice(s.getName(App.CURRENT_LANGUAGE), Icons.get("icon_shop_stable"), () -> Game.activity.goToViewStable(s.getUId())));
        }
        c.add(back());
        Ui.get().setChoices(Game.t("Shops"), c, this::showMainMenu);
    }

    private Ui.Choice back() { return new Ui.Choice(Game.t("Back").equals("Back") ? Tr.t("Back") : Game.t("Back"), Icons.get("icon_back"), this::showMainMenu); }

    public static String formatMinutes(int m) {
        if (m < 60) return m + Tr.t(" min");
        return (m / 60) + " h" + (m % 60 == 0 ? "" : " " + (m % 60) + Tr.t(" min"));
    }

    // ---------------------------------------------------------------- actions / travel

    /** ViewGameplay action selection + onActionFinished(). */
    private void runAction(Action action) {
        Ui.get().hideChoices();
        GameEngine.executeAction(action, new Action.UIActionFinishedListener() {
            @Override public void onFinish(Context context, Action a, boolean success) { actionFinished(a, success); }
            @Override public void onBattleEncountered(Context context, BattlesIds id) {
                App.Shell.flowManager.currentBattleId = id;
                actionCanCauseRandomBattle = false;
                updateView();
            }
        }, actionCanCauseRandomBattle, ctx());
        if (!Ui.get().hasModal() && Screens.current() == this) showMainMenu();
    }

    private void actionFinished(Action action, boolean success) {
        if (action == null) { showMainMenu(); return; }
        if (action.executionSound != null && !action.executionSound.isEmpty()) Sound.play(action.executionSound);
        Ui.get().transition(action.getName(App.CURRENT_LANGUAGE) + "…", 500, () -> {
            ActionResults results = action.execute(App.getPlayerChar(), success, ctx());
            if (action.givesRecompense && results != null) {
                results.giveTo(App.getPlayerChar());
                if (results.finalRecompense != null && results.finalRecompense.descriptionTranslations.isEmpty()) {
                    results.finalRecompense.descriptionTranslations = results.descriptionTranslations;
                }
                GameEngine.saveGame(ctx());
                Panels.recompense(action.getName(App.CURRENT_LANGUAGE), results.finalRecompense, this::refresh);
            } else {
                refresh();
            }
        });
    }

    /** ViewGameplay.travelToLocation() */
    public void travelToLocation(LocationsIds id, boolean isPlace) {
        Ui.get().hideChoices();
        App.Shell.flowManager.currentLocation.checkedEventsAndBattles = false;
        App.Shell.flowManager.shouldCheckRandomBattle = true;
        skipRandomBattle = false;
        GameEngine.executeTravelEvent(App.Shell.flowManager.currentLocation, id, isPlace, travelCallback, ctx());
    }

    /** Used by scripts (GameplayActivity.travelTo). */
    public void travelTo(LocationsIds id) {
        if (Screens.current() != this) Screens.show(this);
        boolean place = App.DataManager.getPlace(id) != null && App.DataManager.getLocale(id) == null;
        travelToLocation(id, place);
    }

    /** ViewGameplay.leavePlaceCommand() */
    private void leavePlace() {
        Location target = null;
        boolean toPlace = false;
        Location here = App.Shell.flowManager.currentLocation;
        try {
            if (here instanceof Place p) {
                if (p.returnToPlace != null) { target = App.DataManager.getPlace(p.returnToPlace); toPlace = true; }
                else target = App.DataManager.getLocale(p.returnToLocale);
            } else {
                target = App.DataManager.getLocale(App.Shell.flowManager.lastLocationId);
            }
        } catch (Exception ignored) {}
        if (target != null) travelToLocation(target.getUId(), toPlace);
        else App.Shell.setCorrupted(true, "Cannot determine place/locale to return.");
    }
}
