package com.cnx.desktop.game.screens;

import android.content.Context;
import com.cnx.desktop.game.*;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.R;
import com.cnx.endlesstalestwo.data.battles.BattlesIds;
import com.cnx.endlesstalestwo.entities.*;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.libs.LibBattle;
import com.cnx.endlesstalestwo.libs.LibInventory;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turn-based battle — port of the Android ViewBattle, using the game's own LibBattle rules
 * (turn order by agility, attacks, criticals, skills, items, escape, enemy AI, status effects,
 * victory / defeat / escape results and rewards).
 *
 * Accessibility: every battle message is spoken in order; targets are announced with their health;
 * the critical hit is a "press Space quickly" moment with a sound per hit (or automatic, in Options);
 * S reads both sides.
 */
public final class BattleScreen implements Screens.Screen {
    private final BattlesIds id;
    private Battle battle;
    private Battler player;
    /** Enemy slots 1 and 2 (ENEMY1 / ENEMY2); null when that enemy is gone. */
    private final Battler[] enemies = new Battler[2];
    private final boolean[] dead = new boolean[2];
    private int remaining;
    private Enums.BattleParticipants[] turnOrder = new Enums.BattleParticipants[0];
    private int turnIndex = 0;
    private Enums.BattleParticipants turnOwner;
    private int currentTurn = 0;
    private Enums.BattleResult result = Enums.BattleResult.UNDEFINED;
    private Enums.BattleEscapeResults escapeResults = Enums.BattleEscapeResults.FAILED;
    private boolean finished, awaitingNextTurn;
    private final List<String> log = new ArrayList<>();
    private VBox stageNode;

    public BattleScreen(BattlesIds id) { this.id = id; }

    private static Context ctx() { return Game.activity; }
    private static String s(int resId, Object... args) {
        try { return args.length == 0 ? Game.activity.getString(resId) : Game.activity.getString(resId, args); }
        catch (Throwable t) { return ""; }
    }

    // ================================================================ setup (onResume + startBattlePreparation)

    @Override
    public void show() {
        App.Shell.flowManager.isPlayerBattling = true;
        battle = App.DataManager.getBattle(id, true);
        if (battle == null) {
            Log.warn("Battle " + id + " not found");
            App.Shell.flowManager.currentBattleId = null;
            App.Shell.flowManager.isPlayerBattling = false;
            Screens.backToGameplay();
            return;
        }
        Ui ui = Ui.get();
        ui.clearScene();
        ui.showHud(false);
        String bg = battle.backgroundImg;
        ui.setBackground(bg != null && !bg.isEmpty() ? Images.asset("locations", bg) : Images.asset("locations", App.Shell.flowManager.currentLocation.getImageFile()), null);
        ui.dimBackground(true);
        setBattleEnvironment();
        setPlayerBattler();
        battle.generateBattlers();
        if (battle.enemyBattlers == null || battle.enemyBattlers.isEmpty()) { result = Enums.BattleResult.VICTORY; updateView(); return; }
        enemies[0] = battle.enemyBattlers.get(0);
        enemies[0].battlerIndex = 1;
        if (battle.enemyBattlers.size() > 1) { enemies[1] = battle.enemyBattlers.get(1); enemies[1].battlerIndex = 2; }
        remaining = enemies[1] == null ? 1 : 2;
        for (Battler b : enemies) if (b != null) b.displayName = b.getName(App.CURRENT_LANGUAGE);
        if (enemies[1] != null && enemies[0].displayName.equals(enemies[1].displayName)) {
            enemies[0].displayName += " 1";
            enemies[1].displayName += " 2";
        }
        result = Enums.BattleResult.UNDEFINED;
        turnOrder = LibBattle.checkFirstTurnByAgility(battle.enemyBattlers, player, ctx());
        if (turnOrder.length > 0) turnOwner = turnOrder[0];
        battle.isAllDataPrepared = true;
        Sound.play("battle_start");
        buildStage();

        StringBuilder intro = new StringBuilder(Tr.t("Battle! "));
        for (Battler b : enemies) if (b != null) intro.append(b.displayName).append(Tr.t(", health ")).append(hp(b)).append(Tr.t(" of ")).append(b.attributesManager.getMaxHp()).append(". ");
        Speech.say(intro.toString(), true);
        String first = turnOwner == Enums.BattleParticipants.PLAYER ? player.displayName : enemies[0].displayName;
        message(s(R.string.hasinitiative, first));
        updateView();
    }

    /** ViewBattle.setBattleEnvironment(): battle music. */
    private void setBattleEnvironment() {
        String music = battle.musicFile != null && !battle.musicFile.isEmpty() ? battle.musicFile
                : battle.escapable ? Constants.MUSIC_BATTLE : Constants.MUSIC_BOSS_BATTLE;
        Sound.music(music);
    }

    /** ViewBattle.setPlayerBattler() */
    private void setPlayerBattler() {
        Character pc = App.getPlayerChar();
        if (pc.currentStatus == null) pc.currentStatus = new ArrayList<>();
        player = new Battler(pc);
        player.updateCriticalByEquips();
        player.isCPU = false;
        player.level = pc.level;
        player.turnName = Enums.BattleParticipants.PLAYER;
        for (AttributeModifierValue v : pc.getDamageInfo(ctx()).inflictValues) {
            if (v.getAttribute().equals(Enums.AttributeName.HP)) {
                player.minValue = v.getMinValue();
                player.maxValue = v.getMaxValue();
                player.damageModifier = v.getExtraValue();
            }
        }
        player.currentStatus.addAll(pc.currentStatus);
        player.displayName = pc.getName();
    }

    // ================================================================ turn loop

    private static int hp(Battler b) { return b.attributesManager.getCurrent(Enums.AttributeName.HP); }
    private static int mp(Battler b) { return b.attributesManager.getCurrent(Enums.AttributeName.MP); }

    private boolean ongoing() {
        if (result == Enums.BattleResult.UNDEFINED) {
            if (remaining <= 0) result = Enums.BattleResult.VICTORY;
            else if (hp(player) <= 0) result = Enums.BattleResult.DEFEAT;
        }
        return result == Enums.BattleResult.UNDEFINED;
    }

    /** ViewBattle.updateView() */
    private void updateView() {
        if (finished) return;
        refreshStage();
        if (ongoing()) prepareNextTurn();
        else finishBattle(LibBattle.executeBattleEndResults(battle, result, ctx()));
    }

    private void prepareNextTurn() {
        if (awaitingNextTurn) return;
        awaitingNextTurn = true;
        Ui.get().hideChoices();
        after(1200, () -> {
            awaitingNextTurn = false;
            if (!ongoing() || finished) { updateView(); return; }
            currentTurn++;
            if (turnOwner == Enums.BattleParticipants.PLAYER) startPlayerTurn();
            else if (turnOwner != null) startEnemyTurn(turnOwner);
        });
    }

    private void startPlayerTurn() {
        message("--- " + s(R.string.turn) + ": " + currentTurn + " ---");
        if (player.isParalyzed) {
            message(s(R.string.IsParalyzed, player.displayName));
            finalizeTurn();
            return;
        }
        showActions();
    }

    private void showActions() {
        List<Ui.Choice> c = Ui.list();
        c.add(new Ui.Choice(Game.t("Attack").equals("Attack") ? Tr.t("Attack") : Game.t("Attack"), Icons.get("icon_damage"), this::beginAttack));
        c.add(new Ui.Choice(Game.t("Skills").equals("Skills") ? Tr.t("Skills") : Game.t("Skills"), Icons.get("icon_skills"), this::openSkills));
        c.add(new Ui.Choice(Game.t("Items").equals("Items") ? Tr.t("Items") : Game.t("Items"), Icons.get("icon_backpack"), this::openItems));
        if (battle.escapable) c.add(new Ui.Choice(Game.t("Escape").equals("Escape") ? Tr.t("Escape") : Game.t("Escape"), Icons.get("icon_battle_escape"), this::tryEscape));
        c.add(new Ui.Choice(Tr.t("Battle status"), Icons.get("icon_stats"), this::sayStatusNow));
        Ui.get().queueMenuAfterSpeech();
        Ui.get().setChoices(Tr.t("Your turn"), c, this::sayStatusNow);
    }

    // ---------------------------------------------------------------- player actions

    private void beginAttack() {
        InflictInfo info = player.getAttackInflictInfo();
        info.displayMessage = s(R.string.creatureattacked, player.displayName);
        chooseTarget(t -> playerAttackEnemy(info, t), this::showActions);
    }

    /** One living enemy: hit it; two: ask which (spoken with health). */
    private void chooseTarget(java.util.function.Consumer<Integer> onTarget, Runnable back) {
        List<Integer> alive = new ArrayList<>();
        for (int i = 0; i < 2; i++) if (enemies[i] != null && !dead[i]) alive.add(i);
        if (alive.size() == 1) { onTarget.accept(alive.get(0)); return; }
        List<Ui.Choice> c = Ui.list();
        for (int i : alive) {
            Battler b = enemies[i];
            c.add(new Ui.Choice(b.displayName, Tr.t("health ") + hp(b) + Tr.t(" of ") + b.attributesManager.getMaxHp() + statusText(b), null, true, () -> onTarget.accept(i)));
        }
        Ui.get().setChoices(Tr.t("Choose a target"), c, back);
    }

    /** ViewBattle.playerAttackEnemy() */
    private void playerAttackEnemy(InflictInfo info, int target) {
        Ui.get().hideChoices();
        boolean crit = LibBattle.checkCritical(info.criticalChance,
                !App.getPlayerChar().isBurdened ? player.attributesManager.getCurrent(Enums.AttributeName.AGILITY, true) : 0);
        info.isCriticalCalculated = true;
        info.inflictValues.get(0).calculationMethod = Enums.StatusModifier.DECREASE;
        if (crit) { criticalHit(info, target); return; }
        LibBattle.calculateFinalValues(info, player, enemies[target]);
        applyToEnemy(target, info);
        finalizeTurn();
    }

    /** ViewBattle.startScreenCriticalHitter(): tap quickly for extra hits (or a fixed 16, like accessibility mode). */
    private void criticalHit(InflictInfo info, int target) {
        Runnable resolve;
        int[] hits = {0};
        resolve = () -> {
            LibBattle.calculateFinalCriticalByHits(Settings.criticalMinigame ? hits[0] : 16, info);
            info.isCritical = true;
            LibBattle.calculateFinalValues(info, player, enemies[target]);
            if (Settings.criticalMinigame) message(hits[0] + " " + s(R.string.Hits));
            applyToEnemy(target, info);
            finalizeTurn();
        };
        if (!Settings.criticalMinigame) { Speech.queue(Tr.t("Critical hit!")); resolve.run(); return; }
        Speech.say(Tr.t("Critical chance! Press Space as fast as you can!"), true);
        Ui.get().setChoices(Tr.t("Critical! Press Space!"), List.of(new Ui.Choice(Tr.t("Strike!"), () -> {
            hits[0]++;
            Sound.play(Constants.SFX_BATTLE_CRITICAL_CHANCE);
        })), null);
        after(2800, () -> { Ui.get().hideChoices(); resolve.run(); });
    }

    private void openSkills() {
        List<Skill> skills = new ArrayList<>();
        for (var key : new ArrayList<>(player.skills.keySet())) {
            Skill sk = App.DataManager.getSkill(key);
            if (sk != null && sk.inBattleUsable) {
                sk.disabled = LibBattle.getSkillCastAttributesCostResults(player, sk) != Enums.SkillCastingResult.CASTABLE;
                skills.add(sk);
            }
        }
        skills.sort(Comparator.comparing(sk -> sk.getName(App.CURRENT_LANGUAGE)));
        if (skills.isEmpty()) { Speech.say(s(R.string.listNoSkills), true); return; }
        List<Ui.Choice> c = Ui.list();
        for (Skill sk : skills) {
            String cost = (sk.mpCost > 0 ? sk.mpCost + Tr.t(" MP") : "") + (sk.hpCost > 0 ? (sk.mpCost > 0 ? ", " : "") + sk.hpCost + Tr.t(" HP") : "");
            String detail = (cost.isEmpty() ? "" : cost + ". ") + sk.getDescription(App.CURRENT_LANGUAGE);
            c.add(new Ui.Choice(sk.getName(App.CURRENT_LANGUAGE), detail, Images.asset("skills", sk.getImageFile()), !sk.disabled, () -> useSkill(sk)));
        }
        Ui.get().setChoices(Game.t("Skills").equals("Skills") ? Tr.t("Skills") : Game.t("Skills"), c, this::showActions);
    }

    /** ViewBattle.onSelectSkillToUse() */
    private void useSkill(Skill skill) {
        if (skill.disabled) {
            Enums.SkillCastingResult r = LibBattle.getSkillCastAttributesCostResults(player, skill);
            Speech.say(r == Enums.SkillCastingResult.NOT_HP ? s(R.string.NotHp) : s(R.string.NotMp), true);
            return;
        }
        if (skill.type == Enums.SkillTypes.ATTACK) {
            if (skill.targets == Enums.TargetOptions.MULTI_TARGET) {
                for (int i = 0; i < 2; i++) if (enemies[i] != null && !dead[i]) applyToEnemy(i, skill.getInflictInfoToBattle(player, enemies[i], ctx()));
                skill.reduceCastCostFromCaster(player);
                finalizeTurn();
                return;
            }
            chooseTarget(t -> {
                Ui.get().hideChoices();
                InflictInfo info = skill.getInflictInfoToBattle(player, enemies[t], ctx());
                skill.reduceCastCostFromCaster(player);
                applyToEnemy(t, info);
                finalizeTurn();
            }, this::openSkills);
            return;
        }
        Ui.get().hideChoices();
        InflictInfo info = skill.getInflictInfoToBattle(player, null, ctx());
        applyToBattler(info, player, player);
        skill.reduceCastCostFromCaster(player);
        finalizeTurn();
    }

    private void openItems() {
        List<ItemReference> usable = new ArrayList<>();
        for (ItemReference ref : player.inventory) {
            Item item = App.DataManager.getItem(ref.itemId);
            if (item instanceof ConsumableItem ci && ci.inBattleUsage && ci.checkRequirements(App.getPlayerChar(), ctx()) == Enums.RequirementVerification.OK) usable.add(ref);
        }
        if (usable.isEmpty()) { Speech.say(s(R.string.listNoItems), true); return; }
        List<Ui.Choice> c = Ui.list();
        for (ItemReference ref : usable) {
            ConsumableItem ci = (ConsumableItem) App.DataManager.getItem(ref.itemId);
            c.add(new Ui.Choice(ci.getName(App.CURRENT_LANGUAGE) + (ref.amount > 1 ? ", " + ref.amount : ""), ci.getDescription(App.CURRENT_LANGUAGE),
                    Images.asset("items", ci.getImageFile()), true, () -> useItem(ci)));
        }
        Ui.get().setChoices(Game.t("Items").equals("Items") ? Tr.t("Items") : Game.t("Items"), c, this::showActions);
    }

    /** ViewBattle.onSelectItemToUse() */
    private void useItem(ConsumableItem item) {
        LibInventory.removeFromInventory(item.getUId(), 1, (Creature) player);
        if (item.canInflictDamage()) {
            chooseTarget(t -> {
                Ui.get().hideChoices();
                InflictInfo info = item.getInflictValues();
                info.soundEffect = item.usageSound;
                applyToEnemy(t, info);
                finalizeTurn();
            }, () -> { LibInventory.addToInventory(item.getUId(), 1, (Creature) player); openItems(); });
            return;
        }
        Ui.get().hideChoices();
        applyToBattler(item.getInflictValues(), player, player);
        finalizeTurn();
    }

    /** ViewBattle.tryEscape() */
    private void tryEscape() {
        Ui.get().hideChoices();
        escapeResults = LibBattle.tryEscape(battle.enemyBattlers, player, ctx());
        if (escapeResults != Enums.BattleEscapeResults.FAILED) {
            message(escapeResults == Enums.BattleEscapeResults.ESCAPED_LUCK ? s(R.string.escapedbattleLuck) : s(R.string.escapedbattleAgility));
            result = Enums.BattleResult.ESCAPED;
            finishBattle(LibBattle.executeBattleEndResults(battle, result, ctx()));
            return;
        }
        message(s(R.string.escapefail));
        finalizeTurn();
    }

    // ---------------------------------------------------------------- enemy turn

    private void startEnemyTurn(Enums.BattleParticipants who) {
        int i = who == Enums.BattleParticipants.ENEMY1 ? 0 : who == Enums.BattleParticipants.ENEMY2 ? 1 : -1;
        if (i < 0) { finalizeTurn(); return; }
        Battler e = enemies[i];
        if (e == null || dead[i] || hp(e) <= 0) { currentTurn--; finalizeTurn(); return; }
        message("--- " + s(R.string.turn) + ": " + currentTurn + " ---");
        if (e.isParalyzed) { message(s(R.string.IsParalyzed, e.displayName)); finalizeTurn(); return; }
        InflictInfo info = LibBattle.defineEnemyActionType(e, ctx());
        if (info.type == Enums.InflictActionType.SKILL) {
            if (info.skill.type == Enums.SkillTypes.ATTACK) {
                info.skill.getInflictInfoToBattle(e, player, ctx());
                applyToBattler(info, player, e);
            } else {
                applyToEnemy(i, info);
            }
            info.skill.reduceCastCostFromCaster(e);
        } else {
            // UIBattleEnemy.calculateEnemyAttack()
            info.element = Enums.Elements.PHYSICAL;
            info.inflictValues.get(0).calculationMethod = Enums.StatusModifier.DECREASE;
            LibBattle.calculateFinalValues(info, e, player);
            applyToBattler(info, player, e);
        }
        after(400, this::finalizeTurn);
    }

    // ---------------------------------------------------------------- applying effects

    private void applyToEnemy(int i, InflictInfo info) { applyToBattler(info, enemies[i], player); refreshStage(); }

    /** ViewBattle.applyInflictInfoToBattler() */
    private void applyToBattler(InflictInfo info, Battler target, Battler source) {
        LibBattle.doInflictValuesTo(info, target);
        if (info.soundEffect != null && !info.soundEffect.isEmpty()) Sound.play(info.soundEffect);
        if (info.statusModifiers != null) for (StatusModifier sm : info.statusModifiers) LibBattle.addStatusToBattler(sm, target);
        info.displayMessage = LibBattle.prepareDisplayMessage(info, source, target, ctx());
        message(info.displayMessage);
    }

    /** ViewBattle.finalizeTurn() */
    private void finalizeTurn() {
        processStatusModifiers();
        turnIndex++;
        if (turnIndex >= turnOrder.length) turnIndex = 0;
        turnOwner = turnOrder.length == 0 ? Enums.BattleParticipants.PLAYER : turnOrder[turnIndex];
        if (!LibBattle.isAlive(player)) {
            result = Enums.BattleResult.DEFEAT;
            message(player.displayName + " " + s(R.string.isdead) + "...");
        } else {
            checkEnemiesAfterTurn();
            if (remaining > 0) {
                if (turnOwner == Enums.BattleParticipants.PLAYER) message(s(R.string.isyourturn) + ", " + player.displayName);
            } else {
                result = Enums.BattleResult.VICTORY;
            }
        }
        updateView();
    }

    private void checkEnemiesAfterTurn() {
        for (int i = 0; i < 2; i++) {
            Battler e = enemies[i];
            if (e != null && !dead[i] && hp(e) <= 0) {
                e.isDead = true;
                dead[i] = true;
                message(s(R.string.waskilled, e.displayName));
                if (e.battlerListeners != null) e.battlerListeners.onDie();
                remaining--;
            }
        }
        refreshStage();
    }

    /** ViewBattle.processBattleStatusModifiers() / processStatusModifier() */
    private void processStatusModifiers() {
        processStatus(player);
        for (Battler b : battle.enemyBattlers) if (LibBattle.isAlive(b)) processStatus(b);
        refreshStage();
    }

    private void processStatus(Battler b) {
        List<Integer> remove = new ArrayList<>();
        if (b.currentStatus != null) {
            for (int i = 0; i < b.currentStatus.size(); i++) {
                StatusModifier sm = b.currentStatus.get(i);
                if (sm.active) {
                    sm.reduceTurns();
                    if (!sm.turnDurationActive()) remove.add(i);
                } else {
                    remove.add(i);
                }
            }
        }
        for (int i : remove) if (b.currentStatus != null && b.currentStatus.size() > i) b.currentStatus.get(i).removeFromCreature(b);
        if (b.isParalyzed) message(s(R.string.IsParalyzed, b.displayName));
        if (b.isPoisoned || b.isBleeding) message(s(R.string.IsPoisoned, b.displayName));
        if (b.isDefenseless) message(s(R.string.IsDefenseless, b.displayName));
        if (b.isPoisoned) {
            int dmg = com.cnx.cnxgameengine.utils.LibUtils.getRandom(1, 3);
            b.attributesManager.decreaseCurrentHp(dmg);
            message(s(R.string.LoseByPoison, b.displayName, String.valueOf(dmg)));
        }
    }

    // ---------------------------------------------------------------- end of battle

    /** ViewBattle.finishBattle() + the result dialog's OK. */
    private void finishBattle(BattleResults results) {
        if (finished) return;
        finished = true;
        Ui.get().hideChoices();
        App.Shell.flowManager.currentBattleId = null;
        App.getPlayerChar().storyInfo.currentBattleUId = null;
        adjustCharacterAfterBattle();
        if (results == null) { leave(); return; }
        results.result = result;
        updateRecompense(results);
        App.Shell.flowManager.shouldCheckRandomBattle = false;
        Sound.play(result == Enums.BattleResult.VICTORY ? "victory" : result == Enums.BattleResult.DEFEAT ? "player_dead" : "button_click_2");
        String title = result == Enums.BattleResult.VICTORY ? s(R.string.VictoryExclamation)
                : result == Enums.BattleResult.ESCAPED ? s(R.string.escaped) : s(R.string.Defeated);
        Panels.recompense(title, results.recompense, () -> {
            App.Shell.flowManager.isPlayerBattling = false;
            if (result == Enums.BattleResult.DEFEAT) App.getPlayerChar().isDead = true;
            else if (results.recompense != null) results.recompense.giveToChar(App.getPlayerChar());
            try {
                if (battle.listeners != null) {
                    if (result == Enums.BattleResult.DEFEAT) battle.listeners.onDefeat(ctx());
                    else if (result == Enums.BattleResult.VICTORY) battle.listeners.onVictory(ctx());
                    else if (result == Enums.BattleResult.ESCAPED) battle.listeners.onEscape(ctx());
                }
            } catch (Throwable t) {
                Log.error("Battle script failed", t);
            }
            GameEngine.saveGame(ctx());
            leave();
        });
    }

    private void leave() {
        App.Shell.flowManager.isPlayerBattling = false;
        App.Shell.flowManager.shouldUpdateGameflow = true;
        if (Screens.current() == this) Screens.backToGameplay();
    }

    /** ViewBattle.updateRecompenseBasedOnResults() */
    private void updateRecompense(BattleResults r) {
        if (result == Enums.BattleResult.VICTORY) {
            r.recompense.calculateRecompense(App.getPlayerChar().attributesManager.getCurrent(Enums.AttributeName.LUCK));
            r.recompense.setTranslatedName(s(R.string.VictoryExclamation));
            r.recompense.imgFile = "icon_battle_victory";
        } else if (result == Enums.BattleResult.ESCAPED) {
            r.recompense = new Recompense("");
            r.recompense.setTranslatedName(s(R.string.escaped));
            r.recompense.addDescriptionTranslation(App.CURRENT_LANGUAGE,
                    escapeResults == Enums.BattleEscapeResults.ESCAPED_LUCK ? s(R.string.escapedbattleLuck) : s(R.string.escapedbattleAgility));
        } else {
            r.recompense = new Recompense("");
            r.recompense.setTranslatedName(s(R.string.Defeated));
        }
    }

    /** ViewBattle.adjustCharacterAttributesAfterBattle() */
    private void adjustCharacterAfterBattle() {
        Character pc = App.getPlayerChar();
        pc.attributesManager.set(Enums.AttributeName.HP, hp(player));
        pc.attributesManager.set(Enums.AttributeName.MP, mp(player));
        pc.inventory = player.inventory;
        pc.isParalyzed = false;
        pc.isDefenseless = false;
        pc.currentStatus.clear();
        if (player.isPoisoned) {
            pc.isPoisoned = true;
            StatusModifier sm = new StatusModifier(Enums.StatusModifier.ADD, Enums.Status.POISONED, 100, 9999);
            sm.durationMinutes = 9999L;
            sm.inBattleUsable = false;
            sm.addToPlayer(pc);
        }
    }

    // ================================================================ display + speech

    @Override public void refresh() {}

    private void sayStatusNow() { sayStatus(); }

    @Override
    public boolean sayStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(player.displayName).append(Tr.t(": health ")).append(hp(player)).append(Tr.t(" of ")).append(player.attributesManager.getMaxHp())
                .append(Tr.t(", mana ")).append(mp(player)).append(Tr.t(" of ")).append(player.attributesManager.getMaxMp()).append(statusText(player)).append(". ");
        for (int i = 0; i < 2; i++) {
            Battler e = enemies[i];
            if (e == null) continue;
            sb.append(e.displayName).append(dead[i] ? Tr.t(": defeated. ") : Tr.t(": health ") + hp(e) + Tr.t(" of ") + e.attributesManager.getMaxHp() + statusText(e) + ". ");
        }
        sb.append(Tr.t("Turn ")).append(Math.max(1, currentTurn)).append(".");
        Speech.say(sb.toString(), true);
        return true;
    }

    private static String statusText(Battler b) {
        List<String> s = new ArrayList<>();
        if (b.isPoisoned) s.add("poisoned");
        if (b.isBleeding) s.add("bleeding");
        if (b.isParalyzed) s.add("paralyzed");
        if (b.isDefenseless) s.add("defenseless");
        return s.isEmpty() ? "" : ", " + String.join(", ", s);
    }

    /** Adds a battle message: shown in the text box and spoken in order. */
    private void message(String m) {
        if (m == null) return;
        // "[B]" is the game's line break: a new line on screen, a pause in speech
        String shown = m.replace("[B]", "\n").trim();
        if (shown.isEmpty()) return;
        String spoken = m.replace("[B]", ". ").replaceAll("^\\.\\s*", "").trim();
        log.add(shown);
        while (log.size() > 6) log.remove(0);
        Ui.get().showTextQuiet(battle.getName(App.CURRENT_LANGUAGE), Tr.t("Turn ") + Math.max(1, currentTurn), String.join("\n", log));
        Speech.queue(shown.startsWith("---") ? Tr.t("Turn ") + currentTurn : spoken);
        Ui.get().setSceneSpeech(String.join(". ", log));
    }

    private void buildStage() {
        stageNode = new VBox(14);
        stageNode.setAlignment(Pos.TOP_CENTER);
        stageNode.setPadding(new Insets(16, 0, 0, 0));
        stageNode.setMouseTransparent(true);
        Ui.get().setSceneNode(stageNode);
        refreshStage();
    }

    private void refreshStage() {
        if (stageNode == null) return;
        stageNode.getChildren().clear();
        HBox row = new HBox(60);
        row.setAlignment(Pos.CENTER);
        for (int i = 0; i < 2; i++) {
            Battler e = enemies[i];
            if (e == null) continue;
            String img = e.imgFile == null ? "" : e.imgFile.replace("enemies/", "");
            ImageView iv = new ImageView(Images.asset("enemies", img));
            iv.setFitHeight(enemies[1] == null ? 340 : 270);
            iv.setFitWidth(420);
            iv.setPreserveRatio(true);
            iv.setOpacity(dead[i] ? 0.25 : 1);
            ProgressBar bar = new ProgressBar(Math.max(0, hp(e)) / (double) Math.max(1, e.attributesManager.getMaxHp()));
            bar.getStyleClass().add("bar-hp");
            bar.setPrefWidth(220);
            Label name = new Label(e.displayName + (dead[i] ? "" : "   " + Math.max(0, hp(e)) + "/" + e.attributesManager.getMaxHp()) + statusText(e));
            name.getStyleClass().add("battle-name");
            VBox card = new VBox(6, iv, bar, name);
            card.setAlignment(Pos.CENTER);
            row.getChildren().add(card);
        }
        ProgressBar php = new ProgressBar(Math.max(0, hp(player)) / (double) Math.max(1, player.attributesManager.getMaxHp()));
        php.getStyleClass().add("bar-hp");
        php.setPrefWidth(200);
        ProgressBar pmp = new ProgressBar(Math.max(0, mp(player)) / (double) Math.max(1, player.attributesManager.getMaxMp()));
        pmp.getStyleClass().add("bar-mp");
        pmp.setPrefWidth(200);
        Label pl = new Label(player.displayName + Tr.t("   HP ") + Math.max(0, hp(player)) + "/" + player.attributesManager.getMaxHp()
                + Tr.t("   MP ") + mp(player) + "/" + player.attributesManager.getMaxMp() + statusText(player));
        pl.getStyleClass().add("battle-name");
        HBox you = new HBox(12, pl, php, pmp);
        you.setAlignment(Pos.CENTER);
        you.getStyleClass().add("battle-player");
        you.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        stageNode.getChildren().addAll(you, row);
    }

    private static void after(long ms, Runnable r) {
        PauseTransition p = new PauseTransition(Duration.millis(ms));
        p.setOnFinished(e -> {
            try { r.run(); } catch (Throwable t) { Log.error("Battle step failed", t); }
        });
        p.play();
    }
}
