# FocusMe - Ironclad Browser Lockdown (Enable)
# Requires Administrator Privileges

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "Elevating to Administrator privileges..." -ForegroundColor Yellow
    Start-Process powershell.exe -Verb RunAs -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
    exit
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  FocusMe Ironclad Lockdown Installer" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Lock Zen Browser
$zenPaths = @(
    "C:\Program Files\Zen Browser",
    "C:\Program Files (x86)\Zen Browser",
    "$env:LOCALAPPDATA\Zen Browser",
    "$env:LOCALAPPDATA\Programs\zen"
)

$zenFound = $false
foreach ($path in $zenPaths) {
    if (Test-Path $path) {
        $distDir = Join-Path $path "distribution"
        if (-not (Test-Path $distDir)) {
            New-Item -ItemType Directory -Path $distDir -Force | Out-Null
        }
        
        $policyJsonPath = Join-Path $distDir "policies.json"
        $policyContent = @'
{
  "policies": {
    "BlockAboutAddons": true,
    "BlockAboutDebugging": true,
    "BlockAboutProfiles": true,
    "ExtensionSettings": {
      "focusme-anti@zen.extension": {
        "private_browsing": true
      }
    }
  }
}
'@
        Set-Content -Path $policyJsonPath -Value $policyContent -Force
        Write-Host "[✓] Zen Browser locked: $policyJsonPath" -ForegroundColor Green
        $zenFound = $true
    }
}

if (-not $zenFound) {
    Write-Host "[!] Zen Browser directory not found in standard paths. Creating global Mozilla policy..." -ForegroundColor Yellow
    $mozPolicyPath = "HKLM:\SOFTWARE\Policies\Mozilla\Firefox"
    if (-not (Test-Path $mozPolicyPath)) {
        New-Item -Path $mozPolicyPath -Force | Out-Null
    }
    Set-ItemProperty -Path $mozPolicyPath -Name "BlockAboutAddons" -Value 1 -Force
}

# 2. Lock Google Chrome
$chromePolicyPath = "HKLM:\SOFTWARE\Policies\Google\Chrome\URLBlocklist"
if (-not (Test-Path $chromePolicyPath)) {
    New-Item -Path $chromePolicyPath -Force | Out-Null
}

Set-ItemProperty -Path $chromePolicyPath -Name "1" -Value "chrome://extensions" -Force
Set-ItemProperty -Path $chromePolicyPath -Name "2" -Value "chrome://flags" -Force
Write-Host "[✓] Google Chrome locked: chrome://extensions blocked via Registry" -ForegroundColor Green

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  LOCKDOWN ACTIVE!" -ForegroundColor Green
Write-Host "  • Zen Browser: about:addons & about:debugging BLOCKED" -ForegroundColor White
Write-Host "  • Google Chrome: chrome://extensions BLOCKED" -ForegroundColor White
Write-Host "  Please completely restart Chrome and Zen Browser." -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"
