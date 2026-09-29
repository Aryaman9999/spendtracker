param(
    [switch]$BumpPatch,
    [switch]$BumpMinor,
    [switch]$Serve,
    [string]$Version = ""
)

$ErrorActionPreference = "Stop"
$ProjectDir = $PSScriptRoot
Set-Location $ProjectDir

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  SpendTracker Build & Deploy Pipeline   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Manage Versioning
$versionPropsPath = Join-Path $ProjectDir "app\version.properties"
$vCode = 2
$vName = "1.2.0"

if (Test-Path $versionPropsPath) {
    $props = ConvertFrom-StringData (Get-Content $versionPropsPath -Raw)
    $vCode = [int]$props.versionCode
    $vName = $props.versionName
}

if ($BumpPatch) {
    $vCode += 1
    $parts = $vName.Split('.')
    $patch = [int]$parts[2] + 1
    $vName = "$($parts[0]).$($parts[1]).$patch"
    Write-Host "[+] Bumping Patch Version: $vName (versionCode: $vCode)" -ForegroundColor Green
} elseif ($BumpMinor) {
    $vCode += 1
    $parts = $vName.Split('.')
    $minor = [int]$parts[1] + 1
    $vName = "$($parts[0]).$minor.0"
    Write-Host "[+] Bumping Minor Version: $vName (versionCode: $vCode)" -ForegroundColor Green
} elseif ($Version -ne "") {
    $vCode += 1
    $vName = $Version
    Write-Host "[+] Setting Custom Version: $vName (versionCode: $vCode)" -ForegroundColor Green
} else {
    Write-Host "[i] Current Version: $vName (versionCode: $vCode)" -ForegroundColor Yellow
}

# Save version back to properties
@"
versionCode=$vCode
versionName=$vName
"@ | Out-File -FilePath $versionPropsPath -Encoding utf8

# 2. Build APK
Write-Host "`n[+] Compiling and building debug APK..." -ForegroundColor Cyan
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Android\Sdk"

& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "[-] Build failed!" -ForegroundColor Red
    exit 1
}

# 3. Copy APK to Project Root
$builtApk = Join-Path $ProjectDir "app\build\outputs\apk\debug\app-debug.apk"
$rootApk = Join-Path $ProjectDir "SpendTracker.apk"
$versionedApk = Join-Path $ProjectDir "SpendTracker-v$vName.apk"
Copy-Item $builtApk $rootApk -Force
Copy-Item $builtApk $versionedApk -Force
Write-Host "[+] Successfully created: $rootApk" -ForegroundColor Green
Write-Host "[+] Successfully created: $versionedApk" -ForegroundColor Green

# 4. Push to Google Drive via rclone (CLI)
$cmd = Get-Command rclone -ErrorAction SilentlyContinue
$rcloneExe = if ($cmd) { $cmd.Source } else { $null }
if (-not $rcloneExe) {
    $wingetRclone = "C:\Users\DELL DN\AppData\Local\Microsoft\WinGet\Packages\Rclone.Rclone_Microsoft.Winget.Source_8wekyb3d8bbwe\rclone-v1.75.1-windows-amd64\rclone.exe"
    if (Test-Path $wingetRclone) {
        $rcloneExe = $wingetRclone
    }
}

if ($rcloneExe) {
    $remotes = @()
    try {
        $remotes = & $rcloneExe listremotes --log-level ERROR 2>$null
    } catch {
        $remotes = @()
    }
    if ($remotes -contains "gdrive:") {
        Write-Host "`n[+] Uploading to Google Drive via rclone..." -ForegroundColor Cyan
        & $rcloneExe copy $rootApk "gdrive:SpendTracker/" --log-level ERROR
        & $rcloneExe copy $versionedApk "gdrive:SpendTracker/" --log-level ERROR
        Write-Host "[+] Successfully uploaded to Google Drive:" -ForegroundColor Green
        Write-Host "    • SpendTracker/SpendTracker.apk (latest)" -ForegroundColor Green
        Write-Host "    • SpendTracker/SpendTracker-v$vName.apk" -ForegroundColor Green
    } else {
        Write-Host "`n[i] rclone is installed! Connect Google Drive once by running: rclone config" -ForegroundColor Yellow
        Write-Host "    (Choose 'n' -> Name: 'gdrive' -> Storage: 'drive' -> Follow browser prompt)" -ForegroundColor DarkGray
    }
}

# 5. Sync to OneDrive (if present)
$oneDrivePath = Join-Path $env:USERPROFILE "OneDrive\Apps\SpendTracker"
if (Test-Path (Join-Path $env:USERPROFILE "OneDrive")) {
    if (-not (Test-Path $oneDrivePath)) {
        New-Item -ItemType Directory -Path $oneDrivePath -Force | Out-Null
    }
    Copy-Item $rootApk (Join-Path $oneDrivePath "SpendTracker.apk") -Force
    Copy-Item $versionedApk (Join-Path $oneDrivePath "SpendTracker-v$vName.apk") -Force
    Write-Host "[+] Synced to OneDrive: $oneDrivePath" -ForegroundColor Green
}

# 6. Sync to Google Drive desktop virtual drive (if installed)
$gDrivePaths = @("G:\My Drive", "G:\Shared drives", "$env:USERPROFILE\Google Drive\My Drive", "$env:USERPROFILE\Google Drive")
foreach ($gPath in $gDrivePaths) {
    if (Test-Path $gPath) {
        $targetFolder = Join-Path $gPath "SpendTracker"
        if (-not (Test-Path $targetFolder)) {
            New-Item -ItemType Directory -Path $targetFolder -Force | Out-Null
        }
        Copy-Item $rootApk (Join-Path $targetFolder "SpendTracker.apk") -Force
        Write-Host "[+] Synced to Google Drive folder: $targetFolder\SpendTracker.apk" -ForegroundColor Green
        break
    }
}

Write-Host "`n🎉 Build complete: v$vName (Build $vCode)" -ForegroundColor Cyan

# 7. Optional Local Wi-Fi Direct Server
if ($Serve) {
    Write-Host "[+] Launching Wi-Fi direct download server..." -ForegroundColor Green
    python serve_apk.py
}
