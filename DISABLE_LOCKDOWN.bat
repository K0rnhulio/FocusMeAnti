@echo off
:: FocusMe Ironclad Lockdown Removal
:: Automatically requests Administrator privileges

net session >nul 2>&1
if %errorLevel% NEQ 0 (
    echo Requesting Administrator privileges...
    powershell -Command "Start-Process cmd -ArgumentList '/c \"\"%~dp0DISABLE_LOCKDOWN.bat\"\"' -Verb RunAs"
    exit /b
)

title FocusMe Ironclad Lockdown Removal
cls
echo ========================================================
echo          FocusMe Ironclad Lockdown Removal
echo ========================================================
echo.

:: 1. Unlock Zen Browser
echo [1/2] Unlocking Zen Browser...
if exist "C:\Program Files\Zen Browser\distribution\policies.json" (
    del /f /q "C:\Program Files\Zen Browser\distribution\policies.json"
    echo [SUCCESS] Zen Browser policies.json removed!
) else (
    echo [NOTICE] Zen Browser policies.json was not found.
)

:: 2. Unlock Google Chrome
echo.
echo [2/2] Unlocking Google Chrome...
reg delete "HKLM\SOFTWARE\Policies\Google\Chrome\URLBlocklist" /f >nul 2>&1
echo [SUCCESS] Google Chrome URLBlocklist policy removed!

echo.
echo ========================================================
echo   LOCKDOWN REMOVED!
echo   Extension settings are now unlocked.
echo   Please RESTART Chrome and Zen Browser.
echo ========================================================
echo.
pause
