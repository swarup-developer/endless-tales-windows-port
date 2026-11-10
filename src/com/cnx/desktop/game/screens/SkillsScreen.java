package com.cnx.desktop.game.screens;

import android.content.Context;
import android.util.ArrayMap;
import com.cnx.desktop.game.*;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.entities.AttributeModifierValue;
import com.cnx.endlesstalestwo.entities.CharClass;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.entities.Skill;
import com.cnx.endlesstalestwo.enums.Enums;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Skills (port of ViewSkills: list, details, cast outside battle) and spending skill points on
 * attributes (port of ViewAttributesManager: same costs from the character class).
 */
public final class SkillsScreen implements Screens.Screen {
    private static final Enums.AttributeName[] ATTRS = {
            Enums.AttributeName.MAX_HP, Enums.AttributeName.MAX_MP, Enums.AttributeName.STRENGTH, Enums.AttributeName.MAGIC,
            Enums.AttributeName.AGILITY, Enums.AttributeName.CHARISMA, Enums.AttributeName.LUCK};

    private static Context ctx() { return Game.activity; }
    private static Character pc() { return App.getPlayerChar(); }

    @Override
    public void show() {
        Ui ui = Ui.get();
        ui.clearScene();
        ui.showHud(true);
        ui.dimBackground(true);
        showSkills(true);
    }

    @Override public void refresh() { showSkills(false); }

    private static String name(Enums.AttributeName a) {
        if (a == Enums.AttributeName.MAX_HP) return Tr.t("Maximum health");
        if (a == Enums.AttributeName.MAX_MP) return Tr.t("Maximum mana");
        String t = Game.t(a.name());
        return t.equals(a.name()) ? ExploreScreen.pretty(a.name()) : t;
    }

    // ---------------------------------------------------------------- skills

    private void showSkills(boolean speak) {
        List<Skill> skills = new ArrayList<>();
        for (var id : pc().skills.keySet()) {
            Skill s = App.DataManager.getSkill(id);
            if (s != null) skills.add(s);
        }
        skills.sort(Comparator.comparing(s -> s.getName(App.CURRENT_LANGUAGE)));
        String title = Game.t("Skills").equals("Skills") ? Tr.t("Skills") : Game.t("Skills");
        String text = skills.size() + Tr.t(" skills. ") + pc().skillPoints + Tr.t(" skill points to spend.");
        if (speak) Ui.get().say(title, null, text, null); else Ui.get().showTextQuiet(title, null, text);
        List<Ui.Choice> c = Ui.list();
        c.add(new Ui.Choice(Tr.t("Improve attributes"), pc().skillPoints + Tr.t(" skill points"), Icons.get("icon_stats"), true, this::showAttributes));
        for (Skill s : skills) {
            c.add(new Ui.Choice(s.getName(App.CURRENT_LANGUAGE), cost(s) + (s.inMenuUsable ? Tr.t(", usable now") : ""), Images.asset("skills", s.getImageFile()), true, () -> showSkill(s)));
        }
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), Screens::backToGameplay));
        if (!speak) Ui.get().queueMenuAfterSpeech();
        Ui.get().setChoices(title, c, Screens::backToGameplay);
    }

    static String plural(int n, String word) { return n + " " + Tr.t(n == 1 ? word : word + "s"); }

    private static String cost(Skill s) {
        if (s.mpCost > 0) return s.mpCost + Tr.t(" mana");
        if (s.hpCost > 0) return s.hpCost + Tr.t(" health");
        return "free";
    }

    private void showSkill(Skill s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s.getName(App.CURRENT_LANGUAGE)).append(". ");
        if (s.type != null) sb.append(ExploreScreen.pretty(s.type.name())).append(Tr.t(" skill"));
        if (s.element != null) sb.append(", ").append(ExploreScreen.pretty(s.element.name()));
        sb.append(Tr.t(". Costs ")).append(cost(s)).append(". ");
        if (s.targets != null) sb.append(Tr.t("Target: ")).append(s.type != Enums.SkillTypes.ATTACK ? "yourself" : ExploreScreen.pretty(s.targets.name())).append(". ");
        try { String v = s.getBaseInflictValueDisplay(); if (v != null && !v.isBlank()) sb.append(Tr.t("Power ")).append(v).append(". "); } catch (Throwable ignored) {}
        for (AttributeModifierValue m : s.inflictValues) {
            if (m.getAttribute() == Enums.AttributeName.HP || m.getAttribute() == Enums.AttributeName.MP) continue;
            try { sb.append(m.getAttributeName(ctx())).append(" ").append(m.getDisplayValue()).append(". "); } catch (Throwable ignored) {}
        }
        String d = s.getDescription(App.CURRENT_LANGUAGE);
        if (d != null && !d.isBlank()) sb.append(d);
        Ui.get().say(s.getName(App.CURRENT_LANGUAGE), null, sb.toString(), null);
        List<Ui.Choice> c = Ui.list();
        if (s.inMenuUsable) c.add(new Ui.Choice(Tr.t("Use now"), null, Icons.get("icon_self_usage"), true, () -> cast(s)));
        c.add(new Ui.Choice(Tr.t("Back"), Icons.get("icon_back"), () -> showSkills(false)));
        Ui.get().setChoices(s.getName(App.CURRENT_LANGUAGE), c, () -> showSkills(false));
    }

    /** ViewSkills.castSpellCommand() */
    private void cast(Skill s) {
        try { s.castOnMenu(pc(), null, ctx()); } catch (Throwable t) { Log.error("Skill cast failed", t); }
        if (s.soundEffectFile != null && !s.soundEffectFile.isEmpty()) Sound.play(s.soundEffectFile);
        GameEngine.saveGame(ctx());
        Ui.get().refreshHud();
        Speech.say(Tr.t("Health ") + pc().attributesManager.getCurrent(Enums.AttributeName.HP) + Tr.t(" of ") + pc().attributesManager.getMaxHp()
                + Tr.t(", mana ") + pc().attributesManager.getCurrent(Enums.AttributeName.MP) + Tr.t(" of ") + pc().attributesManager.getMaxMp(), false);
        Ui.get().queueMenuAfterSpeech();
        showSkills(false);
    }

    // ---------------------------------------------------------------- attributes (ViewAttributesManager)

    private ArrayMap<Enums.AttributeName, Integer> start, current;
    private int points;

    private void showAttributes() {
        CharClass cls = App.DataManager.getClass(pc().classId);
        start = new ArrayMap<>();
        current = new ArrayMap<>();
        for (Enums.AttributeName a : ATTRS) {
            int v = a == Enums.AttributeName.MAX_HP ? pc().attributesManager.getMaxHp()
                    : a == Enums.AttributeName.MAX_MP ? pc().attributesManager.getMaxMp() : pc().attributesManager.getBase(a);
            start.put(a, v);
            current.put(a, v);
        }
        points = pc().skillPoints;
        Ui.get().say(Tr.t("Improve attributes"), null, points + Tr.t(" skill points. Use Left and Right on an attribute to raise or lower it, then choose Confirm."), null);
        attributeMenu(cls);
    }

    private void attributeMenu(CharClass cls) {
        List<Ui.Choice> c = Ui.list();
        Ui.get().showTextQuiet(Tr.t("Improve attributes"), null, points + Tr.t(" skill points left."));
        List<Nav.Item> extra = new ArrayList<>();
        for (Enums.AttributeName a : ATTRS) {
            c.add(new Ui.Choice(name(a) + ": " + current.get(a), Tr.t("next costs ") + plural(cls.getAttributeIncrementCost(a, current.get(a)), "point"), null, true, () -> change(cls, a, 1)));
        }
        c.add(new Ui.Choice(Tr.t("Confirm"), points + Tr.t(" points left"), Icons.get("icon_complete"), !current.equals(start), this::save));
        c.add(new Ui.Choice(Tr.t("Cancel"), Icons.get("icon_back"), () -> showSkills(false)));
        Ui.get().setChoices(Tr.t("Improve attributes, ") + points + Tr.t(" points"), c, () -> showSkills(false));
        // Left / Right on an attribute: lower / raise it
        var items = Ui.get().nav.baseItems();
        for (int i = 0; i < ATTRS.length && i < items.size(); i++) {
            Enums.AttributeName a = ATTRS[i];
            items.get(i).onAdjust(d -> change(cls, a, d));
        }
    }

    /** ViewAttributesManager.onChangeAttribute() */
    private void change(CharClass cls, Enums.AttributeName a, int dir) {
        int v = current.get(a);
        if (dir > 0) {
            int costUp = cls.getAttributeIncrementCost(a, v);
            if (costUp > points) { Speech.say(Game.t("NotEnoughSkillPts"), true); return; }
            points -= costUp;
            current.put(a, v + 1);
        } else {
            if (start.get(a) >= v) { Speech.say(Tr.t("Cannot go below ") + start.get(a), true); return; }
            points += cls.getAttributeIncrementCost(a, v, true);
            current.put(a, v - 1);
        }
        int index = java.util.Arrays.asList(ATTRS).indexOf(a);
        attributeMenu(cls);
        Ui.get().nav.focus(index);
        Speech.say(name(a) + ": " + current.get(a) + ". " + points + Tr.t(" points left"), true);
    }

    /** ViewAttributesManager.saveStats() */
    private void save() {
        var am = pc().attributesManager;
        int maxHp = current.get(Enums.AttributeName.MAX_HP), maxMp = current.get(Enums.AttributeName.MAX_MP);
        int dHp = maxHp - am.getMaxHp(), dMp = maxMp - am.getMaxMp();
        am.set(Enums.AttributeName.MAX_HP, maxHp);
        am.set(Enums.AttributeName.MAX_MP, maxMp);
        am.increaseCurrentHp(dHp);
        am.increaseCurrentMp(dMp);
        for (Enums.AttributeName a : ATTRS) {
            if (a == Enums.AttributeName.MAX_HP || a == Enums.AttributeName.MAX_MP) continue;
            int v = current.get(a);
            int diff = v - am.getBase(a);
            am.set(a, v);
            am.increase(a, diff);
        }
        pc().skillPoints = points;
        GameEngine.saveGame(ctx());
        Sound.play("level_up");
        Ui.get().refreshHud();
        Speech.say(Tr.t("Attributes saved."), true);
        Ui.get().queueMenuAfterSpeech();
        showSkills(false);
    }
}
