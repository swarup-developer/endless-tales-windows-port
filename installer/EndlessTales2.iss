; Inno Setup script for the Endless Tales 2 Windows edition.
; Built by build.ps1:  ISCC.exe /DAppVersion=2.0.3 installer\EndlessTales2.iss
; Installs the jpackage app image from dist\EndlessTales2. Saves live in %APPDATA%\EndlessTales2
; and are kept when the game is uninstalled.

#ifndef AppVersion
  #define AppVersion "2.0.3"
#endif
#define AppName "Endless Tales 2"
#define AppExe "EndlessTales2.exe"

[Setup]
AppId={{6F0B8C2E-5D1A-4C1B-9E57-2B7E0E7A5C21}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion} (Windows edition)
AppPublisher=Endless Tales Windows Port
AppPublisherURL=https://github.com/swarup-developer/endless-tales-windows-port
AppSupportURL=https://github.com/swarup-developer/endless-tales-windows-port/issues
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
; installs for the current user without asking for administrator rights; the setup lets you pick "all users" instead
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir=..\dist
OutputBaseFilename=EndlessTales2-Setup-{#AppVersion}
SetupIconFile=..\build\EndlessTales2.ico
UninstallDisplayIcon={app}\{#AppExe}
Compression=lzma2/fast
SolidCompression=no
WizardStyle=modern
ShowLanguageDialog=auto

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"
Name: "portuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "..\dist\EndlessTales2\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{group}\{cm:UninstallProgram,{#AppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent
