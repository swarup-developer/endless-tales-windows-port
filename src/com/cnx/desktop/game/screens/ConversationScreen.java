package com.cnx.desktop.game.screens;

import com.cnx.desktop.game.*;
import com.cnx.desktop.game.Nav;
import com.cnx.desktop.game.Speech;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.data.npcs.NpcsIds;
import com.cnx.endlesstalestwo.entities.ConversationOption;
import com.cnx.endlesstalestwo.entities.Npc;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.libs.LibNpc;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Talking to an NPC (port of the Android ViewNpcConversation): the NPC portrait stands on the
 * right, the NPC's line types out in the text box and the player's answers are the choices.
 */
public final class ConversationScreen implements Screens.Screen {
    private final NpcsIds npcId;
    private Npc npc;
    private ConversationOption current;
    private int group = -1;
    private String npcPhrase = "";
    private boolean ended;
    private boolean byeDisabled;
    /** Job and age are spoken once, with the greeting. */
    private boolean greeted;

    public ConversationScreen(NpcsIds id) { this.npcId = id; }

    @Override
    public void show() {
        ended = false;
        npc = App.DataManager.getNpc(npcId);
        if (npc == null) { leave(); return; }
        if (npc.hasRandomizedConversations) npc.randomize();
        Ui ui = Ui.get();
        ui.clearScene();
        ui.showHud(true);
        ui.dimBackground(true);
        ui.setPortrait(Images.asset("npcs", npc.getImageFile()), true);
        updateView();
    }

    @Override
    public void refresh() {
        if (ended) return;
        Ui.get().refreshHud();
        updateChoices();
    }

    /** ViewNpcConversation.updateView() */
    private void updateView() {
        if (group == -1) {
            npcPhrase = LibNpc.getGreetingsMsg(npc);
            group = 0;
            if (npc.listeners != null) npc.listeners.onGreetings();
        }
        byeDisabled = npc.cantSayBye;
        updateTalkDisplay(this::updateChoices);
    }

    private String subtitle() {
        String age = "";
        try { age = Tr.t("Age") + ": " + LibNpc.getAge(npc.age); } catch (Throwable ignored) {}
        String job = "";
        try { job = LibNpc.getJobName(npc.job, Game.activity); } catch (Throwable ignored) {}
        String you = current == null ? "" : "\n" + App.getPlayerChar().getName() + ": “"
                + LibNpc.talk(current.getName(App.CURRENT_LANGUAGE), Game.activity, npc) + "”";
        String info = job.isEmpty() ? age : job + "  ·  " + age;
        return info + you;
    }

    private void updateTalkDisplay(Runnable then) {
        String text = npc.talk(npcPhrase, Game.activity);
        Ui.get().hideChoices();
        // speech: name first, then the line; job and age only when they first greet you.
        // Your own chosen line is not read back (you just picked it).
        String name = npc.getName(App.CURRENT_LANGUAGE);
        String spoken = name + ": " + text;
        if (!greeted) {
            greeted = true;
            String info = subtitle().trim();
            if (!info.isEmpty()) spoken += ". " + info;
        }
        Ui.get().say(name, subtitle(), text, then, spoken);
        Ui.get().setSceneSpeech(name + ": " + text);
    }

    /** ViewNpcConversation.onChooseConversation() */
    private void choose(ConversationOption option) {
        if (ended) return;
        Screens.Screen before = Screens.current();
        try {
            option.select(Game.activity, Game.conversationFragment);
        } catch (Throwable t) {
            Log.error("Conversation option failed (" + npcId + ")", t);
        }
        if (Screens.current() != before) return; // the option opened another screen (battle, shop, event ...)
        current = option;
        group = option.toGroup;
        npcPhrase = option.getNpcAnswer(App.CURRENT_LANGUAGE);
        if (option.disableBye) byeDisabled = true;
        else if (!npc.cantSayBye) byeDisabled = false;
        Ui.get().refreshHud();
        updateTalkDisplay(this::updateChoices);
    }

    /** ViewNpcConversation.updateChoices() */
    private void updateChoices() {
        if (ended || Screens.current() != this) return;
        if (group >= 999 || App.Shell.isCorrupted()) {
            Ui.get().hideChoices();
            after(2500, this::sayBye);
            return;
        }
        List<ConversationOption> options = new ArrayList<>();
        for (ConversationOption o : npc.conversationOptions) {
            if (o != null && o.group == group && (o.checkRequirements(App.getPlayerChar(), Game.activity) == Enums.RequirementVerification.OK || o.showEvenWhenNotValid)) {
                options.add(o);
            }
        }
        if (options.isEmpty()) options.add(LibNpc.getConversationOptionWhenNoOptionsAvailable());
        List<Ui.Choice> c = Ui.list();
        for (ConversationOption o : options) {
            boolean ok = o.checkRequirements(App.getPlayerChar(), Game.activity) == Enums.RequirementVerification.OK;
            String label = LibNpc.talk(o.getName(App.CURRENT_LANGUAGE), Game.activity, npc);
            c.add(new Ui.Choice(label, null, null, ok || !o.showEvenWhenNotValid, () -> choose(o)));
        }
        if (!byeDisabled) c.add(new Ui.Choice(Game.t("Bye").equals("Bye") ? Tr.t("Goodbye") : Game.t("Bye"), Icons.get("icon_exit"), this::sayBye));
        Ui.get().setChoices(Tr.t("Your reply"), c, byeDisabled ? () -> Speech.say(Tr.t("You cannot leave this conversation yet"), true) : this::sayBye);
    }

    /** ViewNpcConversation.onSayBye() */
    private void sayBye() {
        if (ended) return;
        if (npc != null && group != 999) {
            npcPhrase = LibNpc.getByeMsg(npc);
            current = null;
            updateTalkDisplay(null);
            if (npc.listeners != null) npc.listeners.onBye();
        }
        group = -1;
        ended = true;
        Ui.get().hideChoices();
        after(1000, this::leave);
    }

    private void leave() {
        App.Shell.flowManager.shouldUpdateGameflow = true;
        App.Shell.flowManager.shouldCheckEvents = true;
        if (Screens.current() == this || npc == null) Screens.backToGameplay();
    }

    private static void after(long ms, Runnable r) {
        PauseTransition p = new PauseTransition(Duration.millis(ms));
        p.setOnFinished(e -> r.run());
        p.play();
    }
}
