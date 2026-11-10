package com.cnx.desktop.game.screens;

import com.cnx.desktop.game.*;
import com.cnx.desktop.game.Nav;
import com.cnx.desktop.game.Speech;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.entities.Constants;
import com.cnx.endlesstalestwo.entities.Event;
import com.cnx.endlesstalestwo.entities.EventChoice;
import com.cnx.endlesstalestwo.enums.Enums;

import java.util.List;

/** Game events shown as a visual-novel scene (port of CnxFragmentImplementation.executeEvent + UIDisplayerEventInfo). */
public final class EventScene {
    private EventScene() {}

    /** CnxFragmentImplementation.executeEvent(): decides how an event is presented. */
    public static void execute(Event event, Runnable updateView) {
        if (event == null) return;
        Enums.EventTypes type = event.eventType;
        if (type == Enums.EventTypes.CHOICE || type == Enums.EventTypes.SIMPLE || type == Enums.EventTypes.QUEST) {
            App.Shell.flowManager.statistics.EventsWitnessed++;
            Screens.event(event, updateView);
            return;
        }
        if (type == Enums.EventTypes.INFORMATIVE) {
            Ui.get().toast(event.getName(App.CURRENT_LANGUAGE) + "\n" + event.getDescription(App.CURRENT_LANGUAGE), false);
        }
        runSafely(() -> event.execute(Game.activity), "execute event " + event.getUId());
        Game.save();
        if (updateView != null) updateView.run();
    }

    /** The scene itself; {@code done} runs after the player picked a choice. */
    public static void show(Event event, Runnable done) {
        Ui ui = Ui.get();
        Sound.play(Constants.SFX_NEW_EVENT);
        ui.hideChoices();
        ui.setPortrait(null, true);
        ui.showHud(true);
        ui.dimBackground(true);
        ui.setCg(Images.asset("events", event.imgFile));
        String text = event.getDescription(App.CURRENT_LANGUAGE).replaceAll("\\[PLAYERNAME]", App.getPlayerChar().getName());
        String title = event.getName(App.CURRENT_LANGUAGE);
        ui.say(title, null, text, () -> showChoices(event, done), Tr.t("Event") + ": " + title + ". " + text);
    }

    private static void showChoices(Event event, Runnable done) {
        List<Ui.Choice> c = Ui.list();
        if (event.eventType == Enums.EventTypes.CHOICE && !event.eventChoices.isEmpty()) {
            for (EventChoice choice : event.eventChoices) {
                c.add(new Ui.Choice(choice.getName(App.CURRENT_LANGUAGE), () -> finish(event, choice, done)));
            }
            // a decision is needed: Escape repeats the scene instead of skipping it
            Ui.get().setChoices(null, c, Ui.get()::repeatScene);
        } else {
            c.add(new Ui.Choice(Tr.t("Continue"), Icons.get("icon_complete"), () -> finish(event, null, done)));
            Ui.get().setChoices(null, c, () -> finish(event, null, done));
        }
    }

    private static void finish(Event event, EventChoice choice, Runnable done) {
        Ui ui = Ui.get();
        ui.hideChoices();
        ui.setCg(null);
        ui.dimBackground(false);
        runSafely(() -> event.execute(Game.activity), "execute event " + event.getUId());
        if (choice != null) runSafely(() -> choice.choose(Game.activity), "event choice " + event.getUId());
        Game.save();
        if (done != null) done.run();
    }

    static void runSafely(Runnable r, String what) {
        try { r.run(); }
        catch (Throwable t) { Log.error("Problem: " + what, t); }
    }
}
