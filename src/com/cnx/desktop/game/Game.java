package com.cnx.desktop.game;

import com.cnx.cnxgameengine.utils.CoreEnums;
import com.cnx.cnxgameengine.utils.LibUtils;
import com.cnx.desktop.DesktopRuntime;
import com.cnx.desktop.ResourceTable;
import com.cnx.endlesstalestwo.App;
import com.cnx.endlesstalestwo.GameEngine;
import com.cnx.endlesstalestwo.data.locations.LocationsIds;
import com.cnx.endlesstalestwo.entities.CharClass;
import com.cnx.endlesstalestwo.entities.Character;
import com.cnx.endlesstalestwo.entities.Constants;
import com.cnx.endlesstalestwo.enums.Enums;
import com.cnx.endlesstalestwo.managers.GameStorageManager;
import com.cnx.endlesstalestwo.views.CnxFragmentImplementation;
import com.cnx.endlesstalestwo.views.ViewGameplay;
import com.cnx.endlesstalestwo.views.ViewNpcConversation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Game state glue: boots the original game logic and keeps the objects the scripts expect. */
public final class Game {
    static final String LOCAL_USER = "DESKTOP_PLAYER";

    /** The Android activity the game scripts talk to (also our Context). */
    public static DesktopActivity activity;
    /** Stand-ins for the Android screens; every method scripts call on them is patched to {@link Hooks}. */
    public static CnxFragmentImplementation gameplayFragment, conversationFragment;

    private Game() {}

    /** Loads all game data. Runs on the JavaFX thread. */
    public static void boot(String languageCode) {
        setLanguage(languageCode);
        App.initializeCore();
        App.CURRENT_LANGUAGE = language();
        activity = new DesktopActivity();
        gameplayFragment = allocate(ViewGameplay.class);
        conversationFragment = allocate(ViewNpcConversation.class);

        if (GameStorageManager.getLocalUserId(activity).isEmpty()) GameStorageManager.setLocalUserId(LOCAL_USER, activity);
        App.GOOGLE_USER_ID = GameStorageManager.getLocalUserId(activity);

        long t = System.currentTimeMillis();
        boolean core = GameEngine.loadCoreData(activity);
        App.DataManager.loadGameplayData();
        // Loading the data asks for a player character before one exists; that sets the "corrupted" flag.
        App.Shell.setCorrupted(false, "");
        Log.info("Game data loaded in " + (System.currentTimeMillis() - t) + " ms (core=" + core
                + ", gameplay=" + App.DataManager.gameplayDataLoaded + ", account=" + (App.Shell.getAccount() != null) + ")");
        if (App.Shell.getAccount() == null) throw new IllegalStateException("Save account could not be created");
    }

    /**
     * Some game texts include the player's name when the data is built (e.g. the intro event), so the
     * Android game reloads the gameplay data once a character is chosen. Same here.
     */
    static void reloadGameplayData() {
        long t = System.currentTimeMillis();
        App.DataManager.loadGameplayData();
        App.Shell.setCorrupted(false, "");
        Log.info("Gameplay data reloaded for " + App.Shell.character.getName() + " in " + (System.currentTimeMillis() - t) + " ms");
    }

    public static void setLanguage(String code) {
        DesktopRuntime.language = code == null ? "" : code;
        ResourceTable.reload();
        if (App.Shell != null || App.DataManager != null) App.CURRENT_LANGUAGE = language();
    }

    static CoreEnums.AvailableLanguages language() {
        return switch (DesktopRuntime.language) {
            case "es" -> CoreEnums.AvailableLanguages.SPANISH;
            case "pt" -> CoreEnums.AvailableLanguages.PORTUGUESE;
            default -> CoreEnums.AvailableLanguages.ENGLISH;
        };
    }

    /** Creates an object without running its (Android) constructor. */
    @SuppressWarnings("unchecked")
    static <T> T allocate(Class<T> c) {
        try {
            Field f = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object unsafe = f.get(null);
            return (T) unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, c);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot allocate " + c, e);
        }
    }

    // ---------------------------------------------------------------- characters

    public static List<Character> savedCharacters() {
        return App.Shell.getAccount() == null ? new ArrayList<>() : new ArrayList<>(App.Shell.getAccountChars());
    }

    public static List<CharClass> classes() { return App.DataManager.getClasses(); }

    /** Same steps as the original ViewNewGame.startGame(). */
    public static void startNewCharacter(String name, CharClass cls, Enums.Gender gender, int avatarIndex) {
        Character c = new Character();
        c.addNameTranslation(CoreEnums.AvailableLanguages.ENGLISH, name);
        c.gender = gender;
        c.classId = cls.uId;
        c.imgFile = (gender == Enums.Gender.MALE ? "male_char_" : "female_char_") + (avatarIndex + 1);
        c.level = 1;
        c.setResidenceLocationId(LocationsIds.LISPORT);
        c.initCharacter();
        c.setInitialAttributesByClass(activity);
        App.Shell.setCorrupted(false, "");
        App.Shell.addNewCharacter(c);
        App.Shell.setSelectedCharacter(c);
        reloadGameplayData();
        App.Shell.fillFlowManagerWithCharacterStoryInfo(c);
        App.Shell.flowManager.setCurrentLocationId(Constants.CHARACTER_INITIAL_LOCALE_ID);
        App.Shell.saveGame(activity);
    }

    /** Same steps as the original ViewInitialMenu.startSavedGame() + ViewLoading. */
    public static void resumeCharacter(Character c) {
        App.Shell.setCorrupted(false, "");
        App.Shell.setSelectedCharacter(c);
        reloadGameplayData();
        App.Shell.fillFlowManagerWithCharacterStoryInfo(c);
        App.Shell.flowManager.currentLocation = null;
        App.Shell.flowManager.shouldUpdateGameflow = true;
    }

    public static void deleteCharacter(Character c) {
        App.Shell.getAccountChars().removeIf(x -> x.id != null && x.id.equals(c.id));
        if (App.Shell.getAccount() != null) App.Shell.getAccount().characters.removeIf(x -> x.id != null && x.id.equals(c.id));
        Character keep = App.Shell.character;
        App.Shell.character = null;
        GameStorageManager.saveGame(activity);
        App.Shell.character = keep != null && keep.id != null && keep.id.equals(c.id) ? null : keep;
    }

    public static void save() {
        try { GameEngine.saveGame(activity); } catch (Throwable t) { Log.error("Save failed", t); }
    }

    // ---------------------------------------------------------------- text helpers

    /** A string resource by name, e.g. t("Actions"). Falls back to the name itself. */
    public static String t(String name) {
        String s = ResourceTable.get().stringByName(name);
        return s != null ? s : name;
    }

    public static String t(int resId) {
        try { return LibUtils.getString(resId, activity); } catch (Throwable e) { return ""; }
    }

    public static Character pc() { return App.getPlayerChar(); }
}
