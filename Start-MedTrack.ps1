param(
    [switch]$Build,
    [string]$SdkPath = (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$workRoot = Split-Path $projectRoot -Parent
$env:GRADLE_USER_HOME = Join-Path $workRoot 'gradle-cache'
$env:ANDROID_AVD_HOME = Join-Path $workRoot 'android-home\avd'
$env:TEMP = Join-Path $workRoot 'tmp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null
$adb = Join-Path $SdkPath 'platform-tools\adb.exe'
$emulator = Join-Path $SdkPath 'emulator\emulator.exe'
$apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if ($Build -or !(Test-Path -LiteralPath $apk)) {
    Push-Location $projectRoot
    try {
        & .\gradlew.bat :app:assembleDebug --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' }
    } finally { Pop-Location }
}
$env:ANDROID_SDK_ROOT = Join-Path $workRoot 'android-sdk'
$serial = 'emulator-5580'
$devices = & $adb devices
if (!($devices -match '^emulator-5580\s+device')) {
    if (!(Test-Path -LiteralPath (Join-Path $env:ANDROID_SDK_ROOT 'system-images\android-35\default\x86_64\kernel-ranchu'))) {
        throw "The verified emulator image is missing from $env:ANDROID_SDK_ROOT."
    }
    Start-Process -FilePath $emulator -ArgumentList @('-avd','MedTrackReview','-port','5580','-no-snapshot','-no-boot-anim','-gpu','swiftshader_indirect') -WindowStyle Normal -RedirectStandardOutput (Join-Path $workRoot 'emulator.log') -RedirectStandardError (Join-Path $workRoot 'emulator-error.log')
}
$ready = $false
for ($attempt = 0; $attempt -lt 120; $attempt++) {
    $boot = & $adb -s $serial shell getprop sys.boot_completed 2>$null
    if ($boot -eq '1') { $ready = $true; break }
    Start-Sleep -Seconds 2
}
if (!$ready) { throw "Emulator did not finish booting. Check $(Join-Path $workRoot 'emulator-error.log')." }
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'App installation failed.' }
& $adb -s $serial shell am start -n com.medtrack.app/.MainActivity
Write-Output 'MedTrack is running in the MedTrackReview emulator.'
