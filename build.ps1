<#
  Builds the Windows edition of Endless Tales 2.

    .\build.ps1                 # compile + app image + portable zip + installer  (all in dist\)
    .\build.ps1 -Run            # compile and start the game from the sources (no packaging)
    .\build.ps1 -NoInstaller    # skip the installer (no Inno Setup needed)

  Results:
    dist\EndlessTales2\EndlessTales2.exe               the game folder (app image)
    dist\EndlessTales2-<ver>-portable.zip              portable: unzip and run, saves stay in the folder
    dist\EndlessTales2-Setup-<ver>.exe                 installer (Start menu, desktop shortcut, uninstaller)

  Needs (see BUILDING.md): JDK 25 (javac, jar, jpackage), the JavaFX SDK 25.0.1 in tools\, android.jar
  (Android SDK platform 35, used only as compile-time stubs) and Inno Setup 6 for the installer.
#>
param(
    [switch]$Run,
    [switch]$NoInstaller,
    [string]$Version = "2.0.3",
    [string]$JavaFx = "",
    [string]$AndroidJar = "",
    [string]$Iscc = ""
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Need($path, $what) { if (-not $path -or -not (Test-Path -LiteralPath $path)) { throw "$what not found: $path  (see BUILDING.md)" } }
function First([string[]]$candidates) { foreach ($c in $candidates) { if ($c -and (Test-Path -LiteralPath $c)) { return $c } }; return $candidates[0] }
function Exec($label, [scriptblock]$cmd) {
    Write-Host "== $label"
    $ErrorActionPreference = 'Continue'
    & $cmd
    $code = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($code -ne 0) { throw "$label failed (exit $code)" }
}

if (-not $JavaFx) { $JavaFx = First @("$PSScriptRoot\tools\javafx-sdk-25.0.1", "$PSScriptRoot\..\tools\javafx-sdk-25.0.1") }
if (-not $AndroidJar) { $AndroidJar = First @("$PSScriptRoot\tools\android.jar", "$env:LOCALAPPDATA\Android\Sdk\platforms\android-35\android.jar") }
Need $JavaFx "JavaFX SDK 25.0.1"
Need $AndroidJar "android.jar"
Need "gamedata\res" "game data (gamedata\res)"
Need "gamedata\assets" "game data (gamedata\assets)"
Need "gamejars\game-classes7.jar" "game logic (gamejars)"
Need "native\nvdaControllerClient.dll" "NVDA controller client"

# ---------------------------------------------------------------- 1. compile
Remove-Item build\shims, build\classes -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force build\shims, build\classes | Out-Null
$shimSrc = (Get-ChildItem shims\src -Recurse -Filter *.java).FullName
Exec "compile desktop stand-ins" { javac -nowarn -encoding UTF-8 -cp $AndroidJar -d build\shims @shimSrc }
$gameJars = @("gamejars\game-classes7.jar") + (Get-ChildItem gamejars\classes*.jar).FullName + (Get-ChildItem lib\imageio\*.jar).FullName
$fxJars = @("javafx.base", "javafx.graphics", "javafx.controls", "javafx.media") | ForEach-Object { "$JavaFx\lib\$_.jar" }
$cp = (@("build\shims") + $gameJars + @($AndroidJar) + $fxJars) -join ';'
$appSrc = (Get-ChildItem src -Recurse -Filter *.java).FullName
Exec "compile game" { javac -nowarn -encoding UTF-8 -cp $cp -d build\classes @appSrc }
Copy-Item src\com\cnx\desktop\game\game.css build\classes\com\cnx\desktop\game\

if ($Run) {
    $runCp = (@("build\classes", "build\shims") + $gameJars + @($AndroidJar)) -join ';'
    java --enable-native-access=ALL-UNNAMED "-Det2.native=$PSScriptRoot\native" "-Det2.data=$PSScriptRoot\gamedata" `
        --module-path "$JavaFx\lib" --add-modules javafx.controls,javafx.media -cp $runCp com.cnx.desktop.game.Launcher
    exit $LASTEXITCODE
}

# ---------------------------------------------------------------- 2. app image
$stage = "build\package"
Remove-Item $stage, dist\EndlessTales2 -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $stage, "$stage\javafx\lib", "$stage\javafx\bin", "$stage\native", "$stage\gamedata", dist | Out-Null
# app.jar = desktop stand-ins + game UI; it must come first on the class path (it replaces android.jar classes)
Copy-Item -Recurse build\shims\* build\classes\ -Force
Exec "app.jar" { jar --create --file "$stage\app.jar" --main-class com.cnx.desktop.game.Launcher -C build\classes . }
$gameJars | ForEach-Object { Copy-Item $_ $stage }
Copy-Item $AndroidJar "$stage\zz-android-stubs.jar"
$fxJars | ForEach-Object { Copy-Item $_ "$stage\javafx\lib" }
Copy-Item "$JavaFx\bin\*" "$stage\javafx\bin"
Copy-Item native\* "$stage\native"
Write-Host "== copy game data"
Copy-Item -Recurse gamedata\res "$stage\gamedata\res"
Copy-Item -Recurse gamedata\assets "$stage\gamedata\assets"

$logo = Get-ChildItem "gamedata\res\drawable*\game_logo_512.*" | Select-Object -First 1
$imageioCp = (Get-ChildItem lib\imageio\*.jar).FullName -join ';'
Exec "icon" { java -cp $imageioCp tools\MakeIcon.java $logo.FullName build\EndlessTales2.ico }

Exec "jpackage" {
    jpackage --type app-image --name EndlessTales2 --app-version $Version --vendor "Endless Tales Windows Port" `
        --description "Endless Tales 2 - Windows edition" --icon build\EndlessTales2.ico `
        --input $stage --main-jar app.jar --main-class com.cnx.desktop.game.Launcher --dest dist `
        --java-options "--enable-native-access=ALL-UNNAMED" `
        --java-options '--module-path=$APPDIR\javafx\lib' `
        --java-options "--add-modules=javafx.controls,javafx.media" `
        --java-options "-Xmx2g"
}

# class path order: app.jar first (desktop stand-ins), Android stubs last, JavaFX only on the module path
$cfg = "dist\EndlessTales2\app\EndlessTales2.cfg"
$lines = Get-Content $cfg
$jars = $lines | Where-Object { $_ -like 'app.classpath=*' } | ForEach-Object { $_.Substring(14) }
$order = @('$APPDIR\app.jar') + ($jars | Where-Object { $_ -notmatch 'app\.jar$|zz-android|\\javafx\\' }) + @('$APPDIR\zz-android-stubs.jar')
$other = $lines | Where-Object { $_ -notlike 'app.classpath=*' }
$out = @()
foreach ($l in $other) { $out += $l; if ($l -eq '[Application]') { $order | Select-Object -Unique | ForEach-Object { $out += "app.classpath=$_" } } }
[IO.File]::WriteAllLines((Resolve-Path $cfg), [string[]]$out, (New-Object Text.UTF8Encoding($false)))  # no BOM
Copy-Item README.md dist\EndlessTales2\README.md

# ---------------------------------------------------------------- 3. portable zip
# portable.txt next to the exe keeps saves in a "saves" folder inside the game folder
$portable = "dist\EndlessTales2-$Version-portable.zip"
Remove-Item $portable -ErrorAction SilentlyContinue
Set-Content -Path dist\EndlessTales2\portable.txt -Encoding utf8 -Value "This file makes the game portable: saves are kept in the 'saves' folder here. Delete it to use %APPDATA%\EndlessTales2 instead."
Exec "portable zip" { tar.exe -a -c -f $portable -C dist EndlessTales2 }
Remove-Item dist\EndlessTales2\portable.txt   # the installer must not be portable

# ---------------------------------------------------------------- 4. installer
if (-not $NoInstaller) {
    if (-not $Iscc) { $Iscc = First @("$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe", "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe", "$env:ProgramFiles\Inno Setup 6\ISCC.exe") }
    Need $Iscc "Inno Setup 6 (ISCC.exe) - or use -NoInstaller"
    Exec "installer" { & $Iscc /Q "/DAppVersion=$Version" installer\EndlessTales2.iss }
}

Write-Host ""
Write-Host "Built:"
Get-ChildItem dist -File | ForEach-Object { Write-Host ("  dist\{0}  ({1:N0} MB)" -f $_.Name, ($_.Length / 1MB)) }
Write-Host "  dist\EndlessTales2\EndlessTales2.exe"
