[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Fail([string]$Message) {
    Write-Error $Message
    exit 1
}

function Get-LatestAndroidBuildTool([string]$FileName) {
    if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        Fail "LOCALAPPDATA is not available; Android SDK location cannot be resolved."
    }

    $buildToolsRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools"
    if (-not (Test-Path $buildToolsRoot)) {
        Fail "Android build-tools directory was not found: $buildToolsRoot"
    }

    $candidate = Get-ChildItem $buildToolsRoot -Directory |
        ForEach-Object {
            $version = $null
            if ([version]::TryParse($_.Name, [ref]$version)) {
                [pscustomobject]@{
                    Version = $version
                    Path = Join-Path $_.FullName $FileName
                }
            }
        } |
        Where-Object { $_ -and (Test-Path $_.Path) } |
        Sort-Object Version -Descending |
        Select-Object -First 1

    if (-not $candidate) {
        Fail "$FileName was not found under $buildToolsRoot"
    }

    return $candidate.Path
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $repoRoot

Write-Host "Recetoria RC1 release verification"
Write-Host "Repository: $repoRoot"

$branch = (& git branch --show-current).Trim()
if ($LASTEXITCODE -ne 0) {
    Fail "Could not determine the current Git branch."
}
if ($branch -ne "release/recetoria-rc1") {
    Fail "Current branch is '$branch'. Switch to 'release/recetoria-rc1' before building RC1."
}
Write-Host "Branch: $branch"

$keystorePropertiesPath = Join-Path $repoRoot "keystore.properties"
if (-not (Test-Path $keystorePropertiesPath)) {
    Fail "keystore.properties is missing from the repository root."
}

$properties = @{}
Get-Content $keystorePropertiesPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line.Split("=", 2)
        if ($parts.Count -eq 2) {
            $properties[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
}

$requiredKeys = @("storeFile", "storePassword", "keyAlias", "keyPassword")
foreach ($key in $requiredKeys) {
    if (-not $properties.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($properties[$key])) {
        Fail "keystore.properties does not contain a non-empty '$key' value."
    }
}

$storeFile = $properties["storeFile"]
if (-not [System.IO.Path]::IsPathRooted($storeFile)) {
    $storeFile = Join-Path $repoRoot $storeFile
}
$storeFile = [System.IO.Path]::GetFullPath($storeFile)
if (-not (Test-Path $storeFile)) {
    Fail "The configured release keystore does not exist: $storeFile"
}

$keyAlias = $properties["keyAlias"]
Write-Host "Keystore: $storeFile"
Write-Host "Alias: $keyAlias"
Write-Host "Signing passwords are not displayed."

$gradle = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradle)) {
    Fail "gradlew.bat was not found."
}

Write-Host ""
Write-Host "Building signed RC1..."
& $gradle clean assembleRelease
if ($LASTEXITCODE -ne 0) {
    Fail "Gradle release build failed."
}

$releaseDir = Join-Path $repoRoot "app\build\outputs\apk\release"
$signedApk = Join-Path $releaseDir "app-release.apk"
$unsignedApk = Join-Path $releaseDir "app-release-unsigned.apk"

if (-not (Test-Path $signedApk)) {
    if (Test-Path $unsignedApk) {
        Fail "Only app-release-unsigned.apk was produced. The release signing configuration was not applied."
    }
    Fail "The expected signed APK was not produced: $signedApk"
}

$apksigner = Get-LatestAndroidBuildTool "apksigner.bat"
$aapt = Get-LatestAndroidBuildTool "aapt.exe"

Write-Host ""
Write-Host "Verifying APK signature..."
& $apksigner verify --verbose --print-certs $signedApk
if ($LASTEXITCODE -ne 0) {
    Fail "apksigner verification failed."
}

Write-Host ""
Write-Host "Verifying package and version..."
$badgingOutput = & $aapt dump badging $signedApk
if ($LASTEXITCODE -ne 0) {
    Fail "aapt could not inspect the APK."
}

$packageLine = ($badgingOutput | Select-String "^package:" | Select-Object -First 1).Line
if ([string]::IsNullOrWhiteSpace($packageLine)) {
    Fail "aapt did not return the package declaration."
}

if ($packageLine -notmatch "name='com\.rmm\.recetasraquel'") {
    Fail "Unexpected applicationId. Expected com.rmm.recetasraquel. Observed: $packageLine"
}
if ($packageLine -notmatch "versionCode='1'") {
    Fail "Unexpected versionCode. Expected 1. Observed: $packageLine"
}
if ($packageLine -notmatch "versionName='1\.0\.0-rc1'") {
    Fail "Unexpected versionName. Expected 1.0.0-rc1. Observed: $packageLine"
}

Write-Host ""
Write-Host "RC1 RELEASE VERIFICATION: PASS"
Write-Host "APK: $signedApk"
Write-Host $packageLine
Write-Host "The APK is signed, has the expected package identity and is ready for the physical-device installation gate."
