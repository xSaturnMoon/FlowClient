$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$modDir = Join-Path $root "FlowClientBrandingMod"
$launcherDir = Join-Path $root "Launcher"
$distExe = Join-Path $root "dist\FlowClient.exe"
$launcherMods = Join-Path $launcherDir "Assets\Mods"
$distMods = Join-Path $root "dist\Assets\Mods"

Write-Host "Building FlowClient mod (1.20.1)..."
Push-Location $modDir
& .\gradlew.bat :1.20.1:build :1.20.1:copyToLauncher --no-daemon -q
if ($LASTEXITCODE -ne 0) { throw "Mod build failed for 1.20.1." }

if (Get-Command java -ErrorAction SilentlyContinue) {
    $javaVersion = (java -version 2>&1 | Select-Object -First 1)
    if ($javaVersion -match 'version "25') {
        Write-Host "Building FlowClient mod (26.2)..."
        & .\gradlew.bat :26.2:build :26.2:copyToLauncher --no-daemon -q
        if ($LASTEXITCODE -ne 0) { Write-Warning "26.2 mod build failed; 1.20.1 jar is still available." }
    } else {
        Write-Warning "Java 25 not detected; skipping 26.2 mod build."
    }
}
Pop-Location

New-Item -ItemType Directory -Force -Path $launcherMods, $distMods | Out-Null
Get-ChildItem $launcherMods -Filter "flowclient-branding-*.jar" | ForEach-Object {
    Copy-Item $_.FullName (Join-Path $distMods $_.Name) -Force
    $jdk25 = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk-25*' } |
        Select-Object -First 1

    if ($null -ne $jdk25) {
        $env:JAVA_HOME = $jdk25.FullName
        $env:PATH = "$($jdk25.FullName)\bin;$env:PATH"
        Write-Host "Building FlowClient mod (26.2) with $($jdk25.FullName)..."

        & .\gradlew.bat :26.2:build :26.2:copyToLauncher --no-daemon -q

        if ($LASTEXITCODE -ne 0) { Write-Warning "26.2 mod build failed; 1.20.1 jar is still available." }
    } else {
        $javaVersion = (java -version 2>&1 | Select-Object -First 1)
        if ($javaVersion -match 'version "25') {
            Write-Host "Building FlowClient mod (26.2)..."
            & .\gradlew.bat :26.2:build :26.2:copyToLauncher --no-daemon -q
            if ($LASTEXITCODE -ne 0) { Write-Warning "26.2 mod build failed; 1.20.1 jar is still available." }
        } else {
            Write-Warning "Java 25 not detected; skipping 26.2 mod build."
        }
if (-not (Test-Path $distExe)) {
    throw "FlowClient.exe not found at $distExe"
}

Write-Host "Starting FlowClient..."
Start-Process $distExe
Write-Host "Done: $distExe"
