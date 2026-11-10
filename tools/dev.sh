#!/usr/bin/env bash
# Dev loop (Git Bash): compile the desktop stand-ins + game; "run" starts it from the sources.
#   bash tools/dev.sh          compile only
#   bash tools/dev.sh run      compile and run   (JOPTS="-D..." adds Java options, SAVES=<folder> for saves)
set -e
G="$(cd "$(dirname "$0")/.." && pwd)"
cd "$G"
FX="$G/tools/javafx-sdk-25.0.1/lib"; [ -d "$FX" ] || FX="$G/../tools/javafx-sdk-25.0.1/lib"
AJ="$G/tools/android.jar"; [ -f "$AJ" ] || AJ="$LOCALAPPDATA/Android/Sdk/platforms/android-35/android.jar"
AJ="$(cygpath -w "$AJ")"
rm -rf build/shims build/classes && mkdir -p build/shims build/classes build/tmp
find shims/src -name '*.java' > build/tmp/shim-sources.txt
javac -nowarn -encoding UTF-8 -cp "$AJ" -d build/shims @build/tmp/shim-sources.txt 2>&1 | grep -v '^Note:' || true
CP="build/shims;gamejars/game-classes7.jar"
for j in gamejars/classes*.jar lib/imageio/*.jar; do CP="$CP;$j"; done
CP="$CP;$AJ"
FXCP=""
for j in javafx.base javafx.graphics javafx.controls javafx.media; do FXCP="$FXCP;$(cygpath -w "$FX/$j.jar")"; done
find src -name '*.java' > build/tmp/app-sources.txt
javac -nowarn -encoding UTF-8 -cp "$CP$FXCP" -d build/classes @build/tmp/app-sources.txt 2>&1 | grep -v '^Note:' | head -${ERRMAX:-60}
cp src/com/cnx/desktop/game/game.css build/classes/com/cnx/desktop/game/
echo "compiled"
if [ "$1" = "run" ]; then
  java --enable-native-access=ALL-UNNAMED -Det2.native="$(cygpath -w "$G/native")" $JOPTS --module-path "$(cygpath -w "$FX")" --add-modules javafx.controls,javafx.media \
    -Det2.data="$(cygpath -w "$G/gamedata")" -Det2.saves="${SAVES:-$TEMP/et2-dev-saves}" \
    -cp "build/classes;$CP" com.cnx.desktop.game.Launcher
fi
