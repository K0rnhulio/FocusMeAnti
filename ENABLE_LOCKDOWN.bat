@echo off
:: FocusMe Ironclad Lockdown Installer
:: Automatically requests Administrator privileges

net session >nul 2>&1
if %errorLevel% NEQ 0 (
    echo Requesting Administrator privileges...
    powershell -Command "Start-Process cmd -ArgumentList '/c \"\"%~dp0ENABLE_LOCKDOWN.bat\"\"' -Verb RunAs"
    exit /b
)

title FocusMe Ironclad Lockdown Installer
cls
echo ========================================================
echo          FocusMe Ironclad Lockdown Installer
echo ========================================================
echo.

:: 1. Lock Zen Browser
echo [1/2] Locking Zen Browser...
if exist "C:\Program Files\Zen Browser" (
    if not exist "C:\Program Files\Zen Browser\distribution" mkdir "C:\Program Files\Zen Browser\distribution"
    (
        echo {
        echo   "policies": {
        echo     "BlockAboutAddons": true,
        echo     "BlockAboutDebugging": true,
        echo     "BlockAboutProfiles": true,
        echo     "ExtensionSettings": {
        echo       "focusme-discipline@k0rnhulio.com": {
        echo         "private_browsing": true
        echo       }
        echo     }
        echo   }
        echo }
    ) > "C:\Program Files\Zen Browser\distribution\policies.json"
    echo [SUCCESS] Zen Browser policies.json created!
) else (
    echo [NOTICE] C:\Program Files\Zen Browser not found directly.
)

:: 2. Lock Google Chrome
echo.
echo [2/2] Locking Google Chrome...
reg add "HKLM\SOFTWARE\Policies\Google\Chrome\URLBlocklist" /v "1" /t REG_SZ /d "chrome://extensions" /f >nul
reg add "HKLM\SOFTWARE\Policies\Google\Chrome\URLBlocklist" /v "2" /t REG_SZ /d "chrome://flags" /f >nul
echo [SUCCESS] Google Chrome URLBlocklist policy applied in Registry!

echo.
echo ========================================================
echo   LOCKDOWN SUCCESSFULLY APPLIED!
echo.
echo   * Zen Browser: about:addons ^& about:debugging BLOCKED
echo   * Google Chrome: chrome://extensions BLOCKED
echo.
echo   Please RESTART Chrome and Zen Browser now!
echo ========================================================
echo.
pause
