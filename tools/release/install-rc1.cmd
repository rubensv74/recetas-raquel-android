@echo off
setlocal EnableExtensions

set "REPO_ROOT=%~dp0..\.."
for %%I in ("%REPO_ROOT%") do set "REPO_ROOT=%%~fI"

set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "APK=%REPO_ROOT%\app\build\outputs\apk\release\app-release.apk"

if not exist "%ADB%" (
  echo ERROR: adb.exe was not found at:
  echo %ADB%
  exit /b 1
)

if not exist "%APK%" (
  echo ERROR: Signed RC1 APK was not found:
  echo %APK%
  echo Run tools\release\verify-rc1.cmd first.
  exit /b 1
)

echo Recetoria RC1 device installation
echo APK: %APK%
echo.

"%ADB%" start-server >nul 2>&1

set "DEVICE_COUNT=0"
for /f "skip=1 tokens=1,2" %%A in ('"%ADB%" devices') do (
  if "%%B"=="device" set /a DEVICE_COUNT+=1
)

if "%DEVICE_COUNT%"=="0" (
  echo ERROR: No authorized Android device is connected.
  echo Enable USB debugging, connect the phone, and accept the RSA authorization prompt.
  echo.
  "%ADB%" devices
  exit /b 2
)

echo Connected devices:
"%ADB%" devices
echo.
echo Installing RC1...
"%ADB%" install -r "%APK%"
if errorlevel 1 (
  echo.
  echo INSTALLATION FAILED.
  exit /b 3
)

echo.
echo RC1 INSTALLATION: PASS
echo Package: com.rmm.recetasraquel
exit /b 0
