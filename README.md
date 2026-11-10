# Endless Tales 2 — Windows edition

A Windows, visual-novel-style edition of the RPG **Endless Tales 2**. The original game logic
(quests, NPC dialogue, events, battles, items, crafting, travel, saves) runs unchanged on desktop
Java; the screens are new. It is built to be **fully playable with a screen reader and the keyboard
alone**, and it also works with a mouse.

Languages: English, Español, Português.

## Download

Get the latest version from the [Releases page](https://github.com/swarup-developer/endless-tales-windows-port/releases).
There are two ways to install it, and both include everything the game needs (no Java to install).

| File | For whom |
|---|---|
| `EndlessTales2-Setup-<version>.exe` | **Installer.** Run it and follow the steps. Adds a Start menu entry, an optional desktop shortcut and an uninstaller. No administrator rights needed. |
| `EndlessTales2-<version>-portable.zip` | **Portable.** Unzip the whole folder anywhere (for example a USB stick) and run `EndlessTales2.exe`. Saves stay inside the folder. |

Windows 10 or 11, 64-bit. About 750 MB of disk space once installed.

Windows SmartScreen may warn about an unknown publisher the first time, because the files are not
code-signed. Choose **More info → Run anyway**.

## Accessibility

- **Speech goes straight to NVDA** (through NV Access's official controller client, also sent to
  braille). If no screen reader is running, the **Windows voice** speaks instead. You can change this
  in Options → Speech output: Automatic, Screen reader only, Windows voice, or Off.
- **No role words.** Items say only their text, such as "Actions, 2 of 5", never "button".
  You can turn the "2 of 5" part off in Options.
- Dialogue is spoken **name first**, for example "Herald: Hello, Tester".
- Arriving somewhere reads the full place description. Coming back says just the name, and R repeats it.
- Events start with "Event:" and a sound cue. Escape cannot skip a decision by accident.
- Pop-ups read their text, then sit on OK. Long ones (character sheet, help) are read line by
  line, with sections. When a pop-up closes, you hear where you are again.
- Name entry is spoken letter by letter, and Backspace says what it removed.
- Options apply immediately, so you hear a volume or speed change while you adjust it.
- Switching back to the game window (Alt+Tab) says where you are.
- Sighted and low-vision players get a strong gold focus highlight and full mouse support.

### Keys

| Key | What it does |
|---|---|
| Up / Down, Tab / Shift+Tab | Move through the choices |
| Enter or Space | Choose |
| Escape or Backspace | Back. On the main game screen it opens the game menu |
| Left / Right | Change a setting (class, gender, volume, speech output …) |
| Home / End | First / last item |
| Page Up / Page Down | Jump between sections (character sheet, options) |
| Shift + letter | Next item starting with that letter |
| R | Repeat the current text (place, dialogue line, event, or the open pop-up) |
| S | Status: health, mana, experience, gold, level, day, time, place |
| C | Character sheet: stats, quests, inventory |
| I | Inventory |
| Ctrl | Stop speech |
| F1 | Keyboard help |
| F11 | Full screen |

## Saves and logs

- **Installer version:** `%APPDATA%\EndlessTales2`. Uninstalling the game keeps your saves.
- **Portable version:** the `saves` folder inside the game folder (because of the `portable.txt`
  file there; delete that file to use `%APPDATA%\EndlessTales2` instead).

The game saves automatically, and the game menu (Escape) has Save game as well.

**Reporting a problem:** send `game.log` from the save folder above and say what you pressed just
before. Open an issue on the [Issues page](https://github.com/swarup-developer/endless-tales-windows-port/issues).

## What is in the game

Title screen and characters (continue, choose, delete), new game, exploration with location art and
day/night, NPC conversations, events and choices, actions with rewards, travel, quests, turn-based
battles (skills, items, critical hits), shops and stables, inventory and equipment, skills and
attributes, crafting, cooking and alchemy, houses with a chest, mounts with a saddlebag, the map and
bestiary, level up and death, options, and autosave.

## Building from source

Needs:

- **JDK 25** (javac, jar, jpackage) on the PATH.
- **JavaFX SDK 25.0.1** for Windows from [gluonhq.com](https://gluonhq.com/products/javafx/), unzipped to
  `tools\javafx-sdk-25.0.1`.
- **android.jar** from the Android SDK, platform 35 (`%LOCALAPPDATA%\Android\Sdk\platforms\android-35\android.jar`),
  or copied to `tools\android.jar`. It is used only as compile-time stubs.
- **Inno Setup 6** for the installer (`winget install JRSoftware.InnoSetup`). Without it, use `-NoInstaller`.

```powershell
.\build.ps1                 # app + portable zip + installer, all in dist\
.\build.ps1 -NoInstaller    # app + portable zip only
.\build.ps1 -Run            # compile and play from the sources
```

Development loop in Git Bash:

```bash
bash tools/dev.sh run
```

Self-test without hands. It plays with the keyboard only and writes screenshots and a speech log.
Its window ignores the real keyboard and stays silent, so don't use it while it runs:

```bash
JOPTS="-Det2.autotest=shots -Det2.autotest.plan=options -Det2.speechlog=speech.log -Det2.silent=true -Det2.mute=true" bash tools/dev.sh run
```

Plans: `tour` (default), `options`, `battle`, `screens`, `spanish`.

## How the port works

- `gamejars/` — the original game's compiled logic, converted from the Android build to desktop
  Java bytecode. `tools/FixJars.java` (ASM) made them work on the desktop JVM, and it replaced the
  ~50 methods listed in `tools/patches.txt` (the ones that opened Android dialogs) with calls into
  `com.cnx.desktop.game.Hooks`.
- `gamedata/` — the game's texts (`res/values*`), images, music, sounds and data files (`assets/`).
- `shims/` — desktop versions of the Android classes the logic uses (Context, Resources reading
  `gamedata/res`, AssetManager, MediaPlayer → JavaFX audio, Handler, Bundle …).
- `src/` — the JavaFX game: `Ui` (visual-novel layers), `Nav` + `Speech` + `Keys` (accessibility),
  `Tr` (texts of the Windows screens in three languages), `screens/` (every game screen).
- `native/` — NVDA controller client.
- `installer/` — Inno Setup script.

## Credits and licenses

- Endless Tales 2 (story, art, music and game logic) by its original authors.
- [JavaFX](https://openjfx.io/) — GPL v2 with the Classpath Exception.
- [NVDA Controller Client](https://github.com/nvaccess/nvda) — LGPL 2.1; see `native/nvdaControllerClient-license.txt`.
- [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys) (WebP images) — BSD 3-Clause.
- Installer made with [Inno Setup](https://jrsoftware.org/isinfo.php).
