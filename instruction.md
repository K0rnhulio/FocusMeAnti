# 📖 FocusMe — Maintenance & Update Instructions

This guide provides the exact commands and workflow to safely update, modify, and re-lock the FocusMe extensions across Google Chrome and Zen Browser.

---

## 🔓 Step 1: Temporarily Unlock the Browsers

To access `chrome://extensions` or `about:addons` when making updates:

1. Open **Windows PowerShell as Administrator** (Press Windows Key, type *PowerShell*, right-click -> *Run as Administrator*).
2. Copy and paste this single command and press **Enter**:

```powershell
Remove-Item "C:\Program Files\Zen Browser\distribution\policies.json" -Force -ErrorAction SilentlyContinue; Remove-Item "HKLM:\SOFTWARE\Policies\Google\Chrome\URLBlocklist" -Recurse -Force -ErrorAction SilentlyContinue; Write-Host "BROWSERS UNLOCKED!" -ForegroundColor Green
```

3. **Restart Google Chrome and Zen Browser** (close and reopen all browser windows).

---

## 🛠️ Step 2: Apply Your Updates

### A. Updating Google Chrome
1. Make your code changes in the `chrome/` directory.
2. In Chrome, navigate to `chrome://extensions`.
3. Click the 🔄 **Reload** button on the **FocusMe** extension card.

---

### B. Updating Zen Browser (Firefox)
1. Make your code changes in the `firefox-zen/` directory.
2. Increment the `"version"` field in `firefox-zen/manifest.json` (e.g., from `"1.0.2"` to `"1.0.3"`).
3. Open a terminal in `FocusMeAnti` and rebuild the upload package:
   ```bash
   node package-extensions.js
   ```
4. Upload `focusme-firefox.zip` to the [Mozilla Add-on Developer Hub (Unlisted)](https://addons.mozilla.org/developers/addon/submit/upload-unlisted).
5. Once auto-signed (~2 minutes), download the new `.xpi` file and open/drag it into Zen Browser to install the update.

---

## 🔒 Step 3: Re-Apply the Ironclad Lockdown

Once your updates are tested and running:

1. In your **Administrator PowerShell** window, copy and paste this command and press **Enter**:

```powershell
New-Item -ItemType Directory -Path "C:\Program Files\Zen Browser\distribution" -Force; Set-Content -Path "C:\Program Files\Zen Browser\distribution\policies.json" -Value '{"policies":{"BlockAboutAddons":true,"BlockAboutDebugging":true,"BlockAboutProfiles":true,"ExtensionSettings":{"focusme-discipline@k0rnhulio.com":{"private_browsing":true}}}}' -Force; New-Item -Path "HKLM:\SOFTWARE\Policies\Google\Chrome\URLBlocklist" -Force; Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Google\Chrome\URLBlocklist" -Name "1" -Value "chrome://extensions" -Force; Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Google\Chrome\URLBlocklist" -Name "2" -Value "chrome://flags" -Force; Write-Host "LOCKDOWN RE-ENABLED!" -ForegroundColor Green
```

2. **Restart Google Chrome and Zen Browser.**

---

## 🛡️ Quick Status Summary

| Browser | Extension Management URL | Lockdown Behavior |
|---|---|---|
| **Google Chrome** | `chrome://extensions` | 🚫 *"Access Blocked by Administrator"* |
| **Zen Browser** | `about:addons` / `about:debugging` | 🚫 *"Access Restricted by Administrator"* |

*Both browsers are protected against impulsive deactivation or deletion during dopamine cravings.*
