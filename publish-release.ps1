$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "  QuickWrite - Release Build & Publish"
Write-Host "========================================"
Write-Host ""

if (-not (Test-Path -LiteralPath "signing.properties")) {
    Write-Host "[ERROR] signing.properties not found." -ForegroundColor Red
    Write-Host "Please copy signing.properties.example to signing.properties and fill in your credentials."
    exit 1
}

Write-Host "[1/2] Building release APK (incremental)..."
& .\gradlew.bat :app:assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Build failed." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/2] Copying APK to release folder..."
$null = New-Item -ItemType Directory -Path "release" -Force
$source = "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path -LiteralPath $source)) {
    Write-Host "[ERROR] APK not found at $source" -ForegroundColor Red
    exit 1
}
Copy-Item -LiteralPath $source -Destination "release\QuickWrite-release.apk" -Force

Write-Host ""
Write-Host "========================================"
Write-Host "  Build Successful!"
Write-Host "  Output: release\QuickWrite-release.apk"
Write-Host "========================================"
