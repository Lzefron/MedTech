$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$secretsPath = Join-Path $projectRoot 'release-secrets\keystore.properties'
if (!(Test-Path -LiteralPath $secretsPath)) {
    throw "Release signing configuration is missing: $secretsPath"
}

$required = @('storeFile', 'storePassword', 'keyAlias', 'keyPassword')
$values = @{}
foreach ($line in Get-Content -LiteralPath $secretsPath) {
    if ($line -match '^\s*([^#][^=]*)=(.*)$') {
        $values[$matches[1].Trim()] = $matches[2].Trim()
    }
}
foreach ($key in $required) {
    if ([string]::IsNullOrWhiteSpace($values[$key])) {
        throw "Missing '$key' in $secretsPath"
    }
}

$keystorePath = $values['storeFile']
if (![IO.Path]::IsPathRooted($keystorePath)) {
    $keystorePath = [IO.Path]::GetFullPath((Join-Path $projectRoot $keystorePath))
}
if (!(Test-Path -LiteralPath $keystorePath)) {
    throw "Upload keystore does not exist: $keystorePath"
}

$workRoot = Split-Path $projectRoot -Parent
$env:GRADLE_USER_HOME = Join-Path $workRoot 'gradle-cache'
$env:TEMP = Join-Path $workRoot 'tmp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null

Push-Location $projectRoot
try {
    & .\gradlew.bat :app:bundleRelease :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Release build failed.' }

    $releaseDirectory = Join-Path $projectRoot 'releases'
    New-Item -ItemType Directory -Force $releaseDirectory | Out-Null
    $aab = Join-Path $projectRoot 'app\build\outputs\bundle\release\app-release.aab'
    $apk = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk'
    $buildText = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'app\build.gradle.kts')
    $versionMatch = [regex]::Match($buildText, 'versionName\s*=\s*"([0-9.]+)"')
    if (!$versionMatch.Success) { throw 'Cannot determine release version.' }
    $version = $versionMatch.Groups[1].Value
    $publishedAab = Join-Path $releaseDirectory "MedTrack-v$version.aab"
    $publishedApk = Join-Path $releaseDirectory "MedTrack-v$version.apk"
    Copy-Item -LiteralPath $aab -Destination $publishedAab -Force
    Copy-Item -LiteralPath $apk -Destination $publishedApk -Force

    $checksums = @(
        "{0}  {1}" -f (Get-FileHash $publishedApk -Algorithm SHA256).Hash.ToLowerInvariant(), [IO.Path]::GetFileName($publishedApk)
        "{0}  {1}" -f (Get-FileHash $publishedAab -Algorithm SHA256).Hash.ToLowerInvariant(), [IO.Path]::GetFileName($publishedAab)
    )
    Set-Content -LiteralPath (Join-Path $releaseDirectory 'SHA256SUMS.txt') -Value $checksums -Encoding ascii
    Write-Output "Release APK: $publishedApk"
    Write-Output "Play bundle: $publishedAab"
} finally {
    Pop-Location
}
