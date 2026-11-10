package com.cnx.desktop.game;

import android.app.Activity;
import android.content.Context;
import android.util.ArrayMap;
import android.view.View;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.cnx.cnxgameengine.activities.BaseActivity;
import com.cnx.cnxgameengine.fragments.CnxFragment;
import com.cnx.cnxgameengine.ui.CnxBaseDialog;
import com.cnx.cnxgameengine.utils.CnxToast;
import com.cnx.cnxgameengine.utils.CoreEnums;
import com.cnx.desktop.game.screens.ExploreScreen;
import com.cnx.desktop.game.screens.EventScene;
import com.cnx.desktop.game.screens.Panels;
import com.cnx.endlesstalestwo.entities.Arena;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.GameShell;
import com.cnx.endlesstalestwo.activities.GameplayActivity;
import com.cnx.endlesstalestwo.data.locations.LocationsIds;
import com.cnx.endlesstalestwo.dialogs._CnxConfirmationDialog;
import com.cnx.endlesstalestwo.entities.*;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.interfaces.TravelCallback;
import com.cnx.endlesstalestwo.libs.Utils;
import com.cnx.endlesstalestwo.views.CnxFragmentImplementation;

import java.util.ArrayList;
import java.util.List;

/**
 * Desktop replacements for game methods that open Android screens, dialogs or toasts.
 * The build rewrites each method listed in tools/patches.txt so that its body calls the
 * matching method here (named SimpleClassName_methodName; instance methods get the object first).
 */
public final class Hooks {
    private Hooks() {}

    private static Ui ui() { return Ui.get(); }

    private static String lang(com.cnx.cnxgameengine.entities.Base b) { return b == null ? "" : b.getName(App.CURRENT_LANGUAGE); }

    // ================================================================ GameEngine

    /** Travel: fade to black with "Traveling to X", then the same steps as GameEngine.onTravelDialogClose(). */
    public static void GameEngine_showTravelingDialog(Location from, Location to, TravelCallback callback, Context ctx) {
        int minutes;
        String caption;
        if (from.isPlace || to.isPlace) {
            caption = fmt(Game.t("Walkingto"), lang(to));
            minutes = Constants.TRAVEL_TO_PLACE_DEFAULT_TIME;
        } else {
            caption = fmt(Game.t("Travelingto"), lang(to));
            minutes = GameEngine.calculateTravelTime(from, to);
        }
        final int m = minutes;
        Sound.play(from.isPlace || to.isPlace ? "button_click_2" : "confirm");
        ui().hideChoices();
        ui().transition(caption + "\n\n" + ExploreScreen.formatMinutes(m), 1500, () -> {
            if (App.Shell.isCorrupted()) return;
            App.Shell.flowManager.updateInGameTime(m, true, ctx);
            App.Shell.flowManager.updateGameplayFlow(to.getUId(), ctx);
            App.Shell.flowManager.currentLocation.onArrive(ctx);
            callback.afterTravel();
        });
    }

    private static String fmt(String pattern, String arg) {
        try { return String.format(pattern, arg); } catch (Exception e) { return pattern + " " + arg; }
    }

    public static void GameEngine_showLevelUpDialog(Character c, boolean newSkills, Context ctx) { Panels.levelUp(c, newSkills); }

    public static void GameEngine_showCraftSkillLevelUpDialog(Enums.CraftTypes type, int from, int to, Context ctx) {
        Sound.play("level_up");
        ui().toast((type == Enums.CraftTypes.ALCHEMY ? Game.t("Alchemy") : Game.t("CraftSkill")) + ": " + Game.t("Level") + " " + from + " → " + to, false);
    }

    /** GameEngine.charDeath() already took the lost gold/XP; OK runs restoreAfterDeath() like the Android dialog. */
    public static void GameEngine_showCharDeathDialog(int lostXp, int lostGold, Context ctx) {
        Panels.death(lostXp, lostGold, () -> GameEngine_restoreAfterDeath(ctx));
    }

    public static void GameEngine_restoreAfterDeath(Context ctx) {
        Character pc = App.getPlayerChar();
        App.Shell.flowManager.updateInGameTime(Constants.MINUTES_IN_DAY, false, ctx);
        if (pc.residenceLocationId == null) pc.residenceLocationId = LocationsIds.LISPORT;
        App.Shell.flowManager.setCurrentLocation(pc.residenceLocationId);
        Game.save();
        Screens.backToGameplay();
    }

    public static void GameEngine_showDialog(CnxBaseDialog dialog) {
        if (dialog != null) Log.warn("Android dialog not shown on desktop: " + dialog.getClass().getName());
    }

    public static void GameEngine_closeDialog() {
        GameShell.dialogOnScreen = false;
        GameEngine.Dialog = null;
    }

    public static void GameEngine_updateTopToolbar(View view, boolean showBack) { ui().refreshHud(); }

    /** GameEngine.executeAction() without the touch mini-game (motion actions just run). */
    public static void GameEngine_executeAction(Action action, Action.UIActionFinishedListener listener, boolean canBattle, Context ctx) {
        int max = action.maxCumulativeTriggers;
        int done = max > 0 ? action.currentTriggeredTimes : -1;
        Enums.RequirementVerification req = action.checkAllRequirements(App.getPlayerChar(), ctx);
        String error = switch (req) {
            case NEED_ITEMS -> Game.t("actionNoItems");
            case NEED_ATTRIBUTES -> Game.t("actionNoStats");
            case NEED_KNOWLEDGES -> Game.t("actionNoKnowledges");
            case NEED_VARIABLES -> Game.t("actionNoVariables");
            case NEED_GOLD -> Game.t("actionNoGold");
            default -> null;
        };
        if (error != null) { ui().toast(error, true); return; }
        if (done >= max) { ui().toast(Game.t("actionmaxtriggered"), true); return; }
        if (req != Enums.RequirementVerification.OK) return;
        Battle battle = canBattle ? GameEngine.checkRandomEncounter(App.Shell.flowManager.currentLocation, App.getPlayerChar(), true, ctx) : null;
        if (battle == null) listener.onFinish(ctx, action, true);
        else listener.onBattleEncountered(ctx, battle.uId);
    }

    public static void GameEngine_giveGoogleAchievement(String id, Activity activity) { Log.info("Achievement unlocked: " + id); }
    public static void GameEngine_giveGoogleAchievement(String id, Context ctx) { Log.info("Achievement unlocked: " + id); }
    public static void GameEngine_applyDayNightFilter(ImageView view, Enums.DayPeriod period) {}
    public static void GameEngine_showToast(String text, Context ctx) { ui().toast(text, false); }

    // ================================================================ libs

    public static void LibQuest_showNewQuestDialog(Quest quest, Context ctx) { if (quest != null) Panels.newQuest(quest); }

    public static void LibActions_executeAction(Action action, Action.UIActionFinishedListener listener, boolean canBattle, Context ctx) {
        GameEngine_executeAction(action, listener, canBattle, ctx);
    }

    /** A list of destinations to pick from (Utils.showTravelSelectionDialog). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void Utils_showTravelSelectionDialog(Context ctx, ArrayList refs) {
        List<Ui.Choice> c = Ui.list();
        final Runnable[] close = new Runnable[1];
        javafx.scene.layout.VBox list = new javafx.scene.layout.VBox(8);
        for (Object o : refs) {
            if (!(o instanceof LocationTravelReference r)) continue;
            LocationsIds target = r.toLocationId != null ? r.toLocationId : r.toPlaceId;
            Location dest = App.DataManager.getLocale(target);
            if (dest == null) dest = App.DataManager.getPlace(target);
            String name = r.destinationName != null && !r.destinationName.isEmpty() ? r.destinationName : lang(dest);
            String cost = r.travelCost > 0 ? "  ·  " + r.travelCost + " gold" : "";
            list.getChildren().add(Ui.choiceButton(new Ui.Choice(name, ExploreScreen.formatMinutes(r.travelMinutes) + cost, null, true, () -> {
                close[0].run();
                Utils.getCharToLocation(target, r.travelCost, Game.activity, App.getPlayerChar(), 300);
            })));
        }
        close[0] = ui().showModal(Ui.panel(Game.t("destinations"), list, 520, Ui.button("Cancel", () -> close[0].run())), true, null);
    }

    // ================================================================ GameShell

    public static void GameShell_showConfirmationDialog(GameShell s, CnxBaseDialog d) { GameEngine_showDialog(d); }
    public static void GameShell_showConfirmationDialog(GameShell s, _CnxConfirmationDialog d) { GameEngine_showDialog(d); }
    public static void GameShell_closeCurrentDialog(GameShell s) {}

    public static void GameShell_returnScreen(GameShell s) {
        s.flowManager.shouldUpdateGameflow = true;
        Screens.backToGameplay();
    }

    // ================================================================ screen callbacks used by scripts

    public static void CnxFragment_setScreenTouchable(CnxFragment f, boolean touchable) { ui().busy(!touchable); }
    public static View CnxFragment_getView(CnxFragment f, int id) { return null; }
    public static BaseActivity CnxFragment_getBaseActivity(CnxFragment f) { return Game.activity; }

    public static void CnxFragmentImplementation_updateTopDisplay(CnxFragmentImplementation f) { ui().refreshHud(); }
    public static void CnxFragmentImplementation_updateTopDisplay(CnxFragmentImplementation f, boolean b) { ui().refreshHud(); }
    public static void CnxFragmentImplementation_executeEvent(CnxFragmentImplementation f, Event e) { EventScene.execute(e, Screens::refreshCurrent); }
    public static GameplayActivity CnxFragmentImplementation_getGameplayActivity(CnxFragmentImplementation f) { return Game.activity; }
    public static Context CnxFragmentImplementation_safeContext(CnxFragmentImplementation f) { return Game.activity; }
    public static void CnxFragmentImplementation_closeCurrentDialog(CnxFragmentImplementation f) {}

    public static FragmentActivity Fragment_getActivity(Fragment f) { return Game.activity; }
    public static FragmentActivity Fragment_requireActivity(Fragment f) { return Game.activity; }
    public static Context Fragment_getContext(Fragment f) { return Game.activity; }
    public static Context Fragment_requireContext(Fragment f) { return Game.activity; }

    // ================================================================ LibUtils toasts / alerts

    private static boolean isError(CoreEnums.MessageType t) { return t != null && t.name().contains("ERROR"); }

    public static void LibUtils_showToast(String s, int duration, Context c) { ui().toast(s, false); }
    public static void LibUtils_showToast(String s, int duration, CoreEnums.MessageType t, Context c) { ui().toast(s, isError(t)); }
    public static void LibUtils_showToast(String s, Context c) { ui().toast(s, false); }
    public static void LibUtils_showToast(String s, CnxToast.ToastDuration d, Context c) { ui().toast(s, false); }
    public static void LibUtils_showToast(String s, CnxToast.ToastDuration d, CoreEnums.MessageType t, Context c) { ui().toast(s, isError(t)); }
    public static void LibUtils_showToast(String s, String title, CnxToast.ToastDuration d, CoreEnums.MessageType t, Context c) {
        ui().toast(title == null || title.isEmpty() ? s : title + "\n" + s, isError(t));
    }

    public static void LibUtils_showInfo(String s, Context c) { ui().message("", s, null, null); }
    public static void LibUtils_showInfo(String s, String type, Context c) { ui().message("", s, null, null); }
    public static void LibUtils_showError(String s) { Log.warn("Game error: " + s); ui().toast(s, true); }
    public static void LibUtils_showError(String s, Context c) { LibUtils_showError(s); }

    public static void LibUtils_showAlertMessage(Context c, String msg, ArrayMap<String, String> opts) {
        String title = opts == null ? "" : opts.getOrDefault("title", "");
        ui().message(title, msg, null, null);
    }
    public static void LibUtils_showAlertMessage(Context c, String msg, String title) { ui().message(title, msg, null, null); }
    public static void LibUtils_showAlertMessage(Context c, String msg, String title, boolean b) { ui().message(title, msg, null, null); }
    public static void LibUtils_showAlertMessage(Context c, String msg, boolean b) { ui().message("", msg, null, null); }

    // ================================================================ content scripts with their own dialogs

    /** Arena master after your last arena win: Arena.onVictory() gives the prize; show it. */
    private static void arenaVictory(Context ctx, com.cnx.endlesstalestwo.data.arenas.ArenasIds arena) {
        Sound.play(Constants.SFX_QUEST_COMPLETED);
        Arena a = App.Shell.flowManager.getActiveArena(arena);
        Recompense r = a == null ? null : a.onVictory();
        if (r != null) Panels.recompense(Game.t("VictoryExclamation"), r, Screens::refreshCurrent);
        App.getPlayerChar().storyInfo.arenaProgress.remove(arena);
        Game.save();
    }

    public static void Cradul_lambda$npc$15(Context ctx, CnxFragmentImplementation f) { arenaVictory(ctx, com.cnx.endlesstalestwo.data.arenas.ArenasIds.FROZEN_ARENA); }
    public static void Fargus_lambda$npc$9(Context ctx, CnxFragmentImplementation f) { arenaVictory(ctx, com.cnx.endlesstalestwo.data.arenas.ArenasIds.SWAMP_FORTRESS); }
    public static void Danjor_lambda$npc$17(Context ctx, CnxFragmentImplementation f) { arenaVictory(ctx, com.cnx.endlesstalestwo.data.arenas.ArenasIds.WAZEL_TOWER); }

    /** Random event "A Hostage", talk choice: same charisma roll, texts and gift as the original. */
    public static void RandomEvents_lambda$aHostage$12(Context ctx) {
        int charisma = App.getPlayerChar().attributesManager.getCurrent(Enums.AttributeName.CHARISMA);
        int chance = com.cnx.cnxgameengine.utils.LibUtils.getRandom(5, 45);
        int roll = charisma * Math.round(com.cnx.cnxgameengine.utils.LibUtils.getRandom(10, 30) / 10.0f);
        var lang = App.CURRENT_LANGUAGE;
        boolean pt = lang == com.cnx.cnxgameengine.utils.CoreEnums.AvailableLanguages.PORTUGUESE;
        boolean es = lang == com.cnx.cnxgameengine.utils.CoreEnums.AvailableLanguages.SPANISH;
        String head = "Chance: " + chance + "%\n" + (pt ? "Resultado Carisma: " : es ? "Resultado de Carisma: " : "Charisma Result: ") + (100 - roll) + "\n\n";
        String title, text;
        if (roll >= chance) {
            title = pt ? "Sucesso!" : es ? "¡Éxito!" : "Success!";
            text = head + (pt ? "Após seu discurso, o bandido libertou a refém e se entregou aos soldados.\nA refém lhe deu um presente por ser salva."
                    : es ? "Tras su discurso, el bandido liberó al rehén y se entregó a los soldados.\nEl rehén le dio un regalo en agradecimiento por haber sido salvado."
                    : "After your speech, the bandit released the hostage and was captured by the army.\nThe hostage gave you a gift for being saved.");
            ItemReference gift = new ItemReference(com.cnx.endlesstalestwo.data.items.ItemsIds.EMERALD, 1);
            com.cnx.endlesstalestwo.libs.LibInventory.addToInventory(gift, App.getPlayerChar());
            ui().toast(Game.t("receivedItem") + gift.getItemData().getName(lang) + " & +5 " + Game.t("ALIGNMENT"), false);
        } else {
            title = pt ? "Falha!" : es ? "¡Fracaso!" : "Fail!";
            text = head + (pt ? "Após seu discurso, o bandido riu sarcasticamente e cortou a garganta da refém.\nEle conseguiu fugir logo em seguida.."
                    : es ? "Tras su discurso, el bandido rió sarcásticamente y degolló al rehén. Logró escapar poco después."
                    : "After your speech, the bandit laughed sarcastically and slit the hostage's throat. He managed to escape immediately afterwards.");
        }
        Game.save();
        ui().message(title, text, null, Screens::refreshCurrent);
    }
}
