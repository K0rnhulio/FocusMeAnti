# FocusMe - Ironclad Browser Lockdown (Disable / Unlock)
# Requires Administrator Privileges

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "Elevating to Administrator privileges..." -ForegroundColor Yellow
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    exit
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  FocusMe Ironclad Lockdown Removal" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Unlock Zen Browser
$zenPaths = @(
    "C:\Program Files\Zen Browser",
    "C:\Program Files (x86)\Zen Browser",
    "$env:LOCALAPPDATA\Zen Browser",
    "$env:LOCALAPPDATA\Programs\zen"
)

foreach ($path in $zenPaths) {
    $policyJsonPath = Join-Path $path "distribution\policies.json"
    if (Test-Path $policyJsonPath) {
        Remove-Item -Path $policyJsonPath -Force
        Write-Host "[✓] Zen Browser policy removed: $policyJsonPath" -ForegroundColor Green
    }
}

# 2. Unlock Google Chrome
$chromePolicyPath = "HKLM:\SOFTWARE\Policies\Google\Chrome\URLBlocklist"
if (Test-Path $chromePolicyPath) {
    Remove-Item -Path $chromePolicyPath -Recurse -Force
    Write-Host "[✓] Google Chrome URLBlocklist policy removed" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  LOCKDOWN REMOVED" -ForegroundColor Green
Write-Host "  • Extension management unlocked for Chrome and Zen Browser." -ForegroundColor White
Write-Host "  Please restart Chrome and Zen Browser." -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"
