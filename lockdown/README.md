# 🔒 FocusMe Ironclad Lockdown

This folder contains scripts to prevent impulsive deactivation or removal of the FocusMe extension during dopamine cravings.

---

## ⚡ How to Enable Lockdown

1. Ensure the **FocusMe** extension is loaded in **Google Chrome** (`chrome://extensions`) and **Zen Browser** (`about:debugging`).
2. Right-click **`enable-ironclad-lock.ps1`** → select **"Run with PowerShell"** (or run as Administrator).
3. Restart **Chrome** and **Zen Browser**.

### What Happens:
- In **Zen Browser**: Navigating to `about:addons` or `about:debugging` displays *"Access Restricted by Administrator"*. The extension cannot be disabled or deleted.
- In **Google Chrome**: Navigating to `chrome://extensions` or `chrome://flags` displays *"Blocked by Administrator"*. The toggle page cannot be accessed.

---

## 🔓 How to Temporarily Unlock

When you legitimately need to update or modify extension files:
1. Right-click **`disable-ironclad-lock.ps1`** → select **"Run with PowerShell"**.
2. Restart Chrome and Zen Browser to access `chrome://extensions` and `about:addons` again.
