package com.cnx.desktop.game;

import com.cnx.endlesstalestwo.data.battles.BattlesIds;
import com.cnx.endlesstalestwo.data.npcs.NpcsIds;
import com.cnx.endlesstalestwo.entities.Event;
import com.cnx.desktop.game.screens.*;

import java.util.ArrayDeque;
import java.util.Deque;

/** Switches between screens. Everything runs on the JavaFX thread. */
public final class Screens {
    public interface Screen {
        void show();
        /** Called when the game says "update the current screen" (after events, scripts ...). */
        default void refresh() { show(); }
        /** S key on this screen; return false to use the normal status. */
        default boolean sayStatus() { return false; }
    }

    private static Screen current;
    private static final ExploreScreen EXPLORE = new ExploreScreen();
    /** Events waiting to be shown one after another. */
    private static final Deque<Event> EVENT_QUEUE = new ArrayDeque<>();
    private static boolean eventOpen;

    private Screens() {}

    public static Screen current() { return current; }

    public static void show(Screen s) {
        Ui.get().closeAllModals();
        Ui.get().busy(false);
        current = s;
        try { s.show(); }
        catch (Throwable t) {
            Log.error("Screen failed: " + s.getClass().getSimpleName(), t);
            Ui.get().toast(Tr.t("Error: ") + t, true);
        }
    }

    public static void title() { show(new TitleScreen()); }
    public static ExploreScreen explore() { return EXPLORE; }

    /** Go to (or back to) the main gameplay screen. */
    public static void backToGameplay() {
        if (current != EXPLORE) show(EXPLORE);
        else EXPLORE.refresh();
    }

    public static void refreshCurrent() {
        if (current != null) {
            try { current.refresh(); } catch (Throwable t) { Log.error("Refresh failed", t); }
        }
    }

    public static void conversation(NpcsIds id) { show(new ConversationScreen(id)); }

    public static void battle(BattlesIds id) { show(new BattleScreen(id)); }

    /** Shows a game event as a visual-novel scene; queued if another event is open. */
    public static void event(Event e, Runnable after) {
        if (e == null) return;
        if (eventOpen) { EVENT_QUEUE.add(e); return; }
        eventOpen = true;
        EventScene.show(e, () -> {
            eventOpen = false;
            Event next = EVENT_QUEUE.poll();
            if (next != null) event(next, after);
            else if (after != null) after.run();
        });
    }

    public static boolean eventOpen() { return eventOpen; }

    public static void characterPanel() { CharacterPanel.show(); }

    public static void help() { Keys.showHelp(); }

    public static void notYet(String what) {
        Ui.get().toast(what + Tr.t(": this screen is not in the Windows version yet."), false);
        Ui.get().busy(false);
    }
}
