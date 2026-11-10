# Building the Windows edition

Notes for whoever builds the game from this repository. Players don't need any of this — see the README.

## Needs

- **JDK 25** (javac, jar, jpackage) on the PATH.
- **JavaFX SDK 25.0.1** for Windows from [gluonhq.com](https://gluonhq.com/products/javafx/), unzipped to
  `tools\javafx-sdk-25.0.1`.
- **android.jar** from the Android SDK, platform 35 (`%LOCALAPPDATA%\Android\Sdk\platforms\android-35\android.jar`),
  or copied to `tools\android.jar`. It is used only as compile-time stubs.
- **Inno Setup 6** for the installer (`winget install JRSoftware.InnoSetup`). Without it, use `-NoInstaller`.

## Build

```powershell
.\build.ps1                 # app + portable zip + installer, all in dist\
.\build.ps1 -NoInstaller    # app + portable zip only
.\build.ps1 -Run            # compile and play from the sources
```

Development loop in Git Bash:

```bash
bash tools/dev.sh run
```

## Self-test

The self-test plays with the keyboard only and writes screenshots and a speech log. Its window
ignores the real keyboard and stays silent, so don't use it while it runs:

```bash
JOPTS="-Det2.autotest=shots -Det2.autotest.plan=options -Det2.speechlog=speech.log -Det2.silent=true -Det2.mute=true" bash tools/dev.sh run
```

Plans: `tour` (default), `options`, `battle`, `screens`, `spanish`.

## How the port works

- `gamejars/` — the original game's compiled logic, converted from the Android build to desktop
  Java bytecode. `tools/FixJars.java` (ASM) made them work on the desktop JVM, and it replaced the
  ~50 methods listed in `tools/patches.txt` (the ones that opened Android dialogs) with calls into
  `com.cnx.desktop.game.Hooks`.
- `gamedata/` — the game's texts (`res/values*`), fonts, images, music, sounds and data files (`assets/`).
- `shims/` — desktop versions of the Android classes the logic uses (Context, Resources reading
  `gamedata/res`, AssetManager, MediaPlayer → JavaFX audio, Handler, Bundle …).
- `src/` — the JavaFX game: `Ui` (visual-novel layers), `Nav` + `Speech` + `Keys` (accessibility),
  `Tr` (texts of the Windows screens in three languages), `screens/` (every game screen).
- `native/` — NVDA controller client.
- `installer/` — Inno Setup script.
- Portable mode: a `portable.txt` file next to `EndlessTales2.exe` keeps saves in its `saves` folder.

## Licenses of the parts

- [JavaFX](https://openjfx.io/) — GPL v2 with the Classpath Exception.
- [NVDA Controller Client](https://github.com/nvaccess/nvda) — LGPL 2.1; see `native/nvdaControllerClient-license.txt`.
- [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys) (WebP images) — BSD 3-Clause.
- Installer made with [Inno Setup](https://jrsoftware.org/isinfo.php).
