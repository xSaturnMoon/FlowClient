; ============================================================
;  Flow Client - Inno Setup Script
;  Installs to %LOCALAPPDATA%\Programs\FlowClient (no admin)
; ============================================================

#define AppName      "Flow Client"
#define AppPublisher "xSaturnMoon"
#define AppVersion   "1.0.7"
#define AppExeName   "FlowClient.exe"
#define SourceDir    "dist"

[Setup]
AppId={{F4A3C2B1-8E7D-4F9A-B6C5-3D2E1A0F8B9C}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL=https://github.com/xSaturnMoon/FlowClient
AppSupportURL=https://github.com/xSaturnMoon/FlowClient/issues
AppUpdatesURL=https://github.com/xSaturnMoon/FlowClient/releases

DefaultDirName={localappdata}\Programs\FlowClient
DefaultGroupName={#AppName}
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=

OutputDir=installer-output
OutputBaseFilename=FlowClientSetup-v{#AppVersion}
SetupIconFile=Launcher\flow.ico

Compression=lzma2/ultra64
SolidCompression=yes
LZMAUseSeparateProcess=yes

WizardStyle=modern
WizardSizePercent=110
DisableWelcomePage=no
DisableProgramGroupPage=yes

UninstallDisplayName={#AppName}
UninstallDisplayIcon={app}\{#AppExeName}
VersionInfoVersion={#AppVersion}
VersionInfoCompany={#AppPublisher}
VersionInfoDescription=Flow Client - Minecraft Launcher

DisableDirPage=yes

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "italian";  MessagesFile: "compiler:Languages\Italian.isl"

[Tasks]
Name: "desktopicon"; Description: "Crea icona sul &Desktop"; GroupDescription: "Icone aggiuntive:"; Flags: unchecked

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"
Name: "{group}\Disinstalla Flow Client"; Filename: "{uninstallexe}"
Name: "{userdesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExeName}"; Description: "Avvia {#AppName}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{userappdata}\FlowLauncher\updates"

[Code]
procedure CurStepChanged(CurStep: TSetupStep);
var
  ResultCode: Integer;
begin
  if CurStep = ssInstall then
  begin
    Exec('taskkill.exe', '/f /im FlowClient.exe', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Sleep(500);
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ResultCode: Integer;
begin
  if CurUninstallStep = usUninstall then
  begin
    Exec('taskkill.exe', '/f /im FlowClient.exe', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Sleep(500);
  end;
end;
