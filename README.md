# First 10 — Morning Behavioral Interruption for Android

> **Protect the first 10 minutes of your day from automatic, habitual phone usage.**

First 10 is a native, privacy-first, completely offline Android application designed to interrupt automatic phone checking immediately after waking up and unlocking your phone.

When you unlock your phone for the first qualifying time of the day, First 10 starts a persistent 10-minute focus protection period. Distracting applications (social media, games, news) are blocked and replaced with a calm morning routine interface, while essential phone calls, emergency services, and whitelisted applications remain fully accessible.

---

## Key Features

1. **True Unlock Detection**
   - Triggers strictly on successful device unlock (`ACTION_USER_PRESENT` after entering PIN/fingerprint/pattern), never on simple screen-on events.
   - Activates once per local calendar day by default.

2. **Persistent Timer & Reboot Recovery**
   - The authoritative countdown is stored as a persistent expiration timestamp (`System.currentTimeMillis() + duration`).
   - Screen-off, app switching, low-memory process termination, or device reboots will **never** reset or cancel the active countdown.

3. **Dynamic Application Management**
   - Automatically discovers all installed launchable applications using Android's `PackageManager`.
   - **Restricted Applications**: Blocked during the morning focus period.
   - **Always Allowed Applications**: Phone dialer, in-call interface, emergency services, and custom user essentials are **always** accessible.

4. **Calm Morning Routine & Interactive Checklist**
   - When attempting to open a restricted app, First 10 displays a fullscreen morning routine:
     1. Brush your teeth
     2. Freshen up
     3. Take a bath
     4. Drink a glass of water
     5. Get dressed
     6. Have breakfast
     7. Start studying / focused work
   - Tasks can be checked off directly on the blocking screen.
   - **Important principle**: Completing tasks builds mindfulness, but only expiration of the countdown unlocks restricted apps.

5. **Exclusion Period Scheduler**
   - Configure recurring exclusion periods (e.g. overnight sleep hours: `10:00 PM – 06:00 AM`).
   - Unlocks during an exclusion period do not trigger the restriction and do not consume your daily focus quota.
   - Full support for overnight schedules crossing midnight and custom weekday selections.

6. **PIN-Protected Settings & Security**
   - Salted SHA-256 password hashing.
   - Automatic 30-second lockout after 5 consecutive incorrect attempts.
   - Documented security question recovery mechanism.

7. **Device Administrator Protection**
   - Optional Device Administrator API integration introduces friction against impulsive uninstallation during moments of weakness.
   - Standard Android administrator controls allow the device owner to disable it through Android Settings at any time.

8. **Stealth Friction Mode**
   - Device owners can optionally hide First 10's launcher shortcut using an activity alias.
   - Documented recovery: Reopen anytime by dialing `*#*#1010#*#*` in your phone dialer or via Android Settings → Apps → First 10 → Open.

9. **Xiaomi MIUI 12.5 & Redmi Note 7 Pro Optimizations**
   - In-app setup guide with direct intent shortcuts for:
     - Autostart permissions.
     - Battery Saver → "No restrictions".
     - Display pop-up windows while running in the background.
     - Locking First 10 in MIUI Recent Apps.

10. **100% Offline & Private**
    - **Zero internet permission** (`android.permission.INTERNET` is not even requested in the manifest).
    - No accounts, no trackers, no cloud analytics, no remote dependencies.

---

## Technical Architecture

```
                  ┌──────────────────────────────────────────────┐
                  │          Android System Broadcasts           │
                  │   (ACTION_USER_PRESENT, BOOT_COMPLETED)      │
                  └──────────────────────┬───────────────────────┘
                                         │
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │              RestrictionEngine               │
                  │  - Checks master toggle                      │
                  │  - Validates today's quota (once per day)    │
                  │  - Checks active exclusion schedules         │
                  └──────────────────────┬───────────────────────┘
                                         │ [Qualifies]
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │          First10MonitoringService            │
                  │  - Foreground Service (specialUse)           │
                  │  - Ongoing countdown notification            │
                  │  - UsageStatsManager foreground polling      │
                  │  - Call state listener (suppresses on calls) │
                  └──────────────────────┬───────────────────────┘
                                         │ [Restricted App Detected]
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │               BlockingActivity               │
                  │  - Fullscreen morning gradient UI            │
                  │  - Live MM:SS focus countdown                │
                  │  - Mindful motivational interruption message │
                  │  - Interactive morning routine checklist     │
                  │  - Emergency bypass (10s hold + confirm)     │
                  └──────────────────────────────────────────────┘
```

---

## Mobile-Only GitHub Actions Build Workflow

You do not need a computer or Android Studio to build, test, and install First 10. The entire lifecycle is automated through GitHub Actions.

### Step 1: Open the Repository on Your Phone
1. Open your mobile browser (Chrome, Firefox, or Safari) and go to your GitHub repository:
   `https://github.com/<your-username>/First10`
2. Switch to **Desktop site** mode in your browser menu if you prefer the full GitHub interface.

### Step 2: Triggering a Build
Whenever you commit changes or push a tag, GitHub Actions automatically builds the project:
1. Tap the **Actions** tab at the top of the repository.
2. Select the **Android Build & Release** workflow.
3. Tap **Run workflow** → select branch `main` → tap **Run workflow**.

### Step 3: Downloading the Debug APK
1. In the **Actions** tab, tap on the latest running or completed workflow run.
2. Scroll down to the **Artifacts** section at the bottom of the page.
3. Tap **First10-Debug-APK** to download the zip file onto your Android device.
4. Extract the zip file in your phone's file manager and tap `app-debug.apk` to install!

---

## Setup & Permission Guide

When launching First 10 for the first time, grant the necessary local permissions:

1. **Usage Access**: Allows First 10 to detect when a restricted application is launched in the foreground.
2. **Display Over Other Apps**: Allows First 10 to display the morning routine over restricted applications.
3. **Battery Optimization**: Set First 10 to "Unrestricted" / "No restrictions" so Android does not kill the monitoring service.
4. **Device Administrator (Optional)**: Adds friction against impulsive uninstallation before your morning routine finishes.
5. **Xiaomi MIUI 12.5 Users**:
   - Go to **Permission Center** → **MIUI 12.5 Guide**.
   - Enable **Autostart**.
   - Under App Permissions, enable **Display pop-up windows while running in the background**.
   - Open Recent Apps, long-press First 10, and tap the **Padlock icon**.

---

## Owner Recovery Instructions

If you enabled **Stealth Friction Mode** (hidden launcher icon) or forgot your PIN:

### Reopening Hidden First 10:
- **Method 1 (Dialer Code)**: Open your phone dialer app and dial `*#*#1010#*#*`. First 10 will open immediately.
- **Method 2 (Android Settings)**: Open Android Settings → Apps → Manage Apps → First 10 → Tap **Open**.
- **Method 3 (Device Admin)**: Open Android Settings → Security → Device Admin apps → First 10.

### Forgotten PIN Recovery:
1. Tap the **?** button on the PIN entry keypad.
2. Answer your personal recovery question set during onboarding.
3. Enter and confirm your new 4-digit PIN.

---

## 29-Point Physical Device Testing Checklist

Perform these tests on your physical Android device (e.g. Redmi Note 7 Pro on Android 10 / MIUI 12.5 or newer):

- [ ] **1. Successful unlock**: Unlocking the phone with fingerprint/PIN starts the 10-minute focus countdown.
- [ ] **2. Screen ON only**: Turning screen on without unlocking does NOT trigger the countdown.
- [ ] **3. Application blocking**: Opening a restricted app displays the full-screen morning routine.
- [ ] **4. Always Allowed apps**: Opening phone dialer or whitelisted apps is completely unrestricted.
- [ ] **5. Incoming calls**: Incoming calls ring normally; call controls remain accessible.
- [ ] **6. Answering/Rejecting calls**: Call can be answered or rejected without obstruction.
- [ ] **7. Ongoing calls**: Ongoing call screen and notification remain fully usable.
- [ ] **8. Emergency calling**: Emergency dialer is never blocked.
- [ ] **9. Exclusion periods**: Unlocks during configured exclusion hours do not start restriction.
- [ ] **10. Overnight exclusions**: Overnight schedules crossing midnight (e.g. 10 PM to 6 AM) behave accurately.
- [ ] **11. Overlapping exclusions**: Multiple overlapping schedules work without conflicts.
- [ ] **12. First unlock post-exclusion**: First unlock after exclusion ends starts restriction.
- [ ] **13. Multiple unlocks**: Unlocking repeatedly does not restart the countdown timer.
- [ ] **14. Screen-off behavior**: Turning the screen off does not pause or freeze the timer.
- [ ] **15. Process termination**: Force stopping First 10 in recents does not reset the expiration timestamp.
- [ ] **16. Reboot recovery**: Restarting phone while timer is active restores the remaining countdown.
- [ ] **17. PIN protection**: Entering settings requires the correct 4-digit PIN.
- [ ] **18. Task persistence**: Task checkboxes retain completed state across app restarts.
- [ ] **19. Task completion rule**: Completing all tasks does NOT unlock restricted apps early.
- [ ] **20. Device Administrator activation**: Successfully activates through official Android dialog.
- [ ] **21. Device Administrator deactivation**: Can be deactivated through Android Settings when intended.
- [ ] **22. Normal uninstallation**: Normal uninstall remains possible after deactivating device admin.
- [ ] **23. Offline operation**: Tested in Airplane Mode with Wi-Fi and mobile data off; works completely.
- [ ] **24. Revocation detection**: Revoking usage access or overlay displays a warning banner.
- [ ] **25. Launcher shortcut removal**: Hiding launcher icon removes it from the home screen.
- [ ] **26. Launcher shortcut restoration**: Restoring launcher icon reappears on the home screen.
- [ ] **27. Settings discoverability**: App remains visible in Android Settings → Apps even when hidden.
- [ ] **28. MIUI 12.5 survival**: Survives MIUI background cleaner with autostart and no battery restrictions.
- [ ] **29. Battery efficiency**: Low RAM footprint (<35MB) and negligible battery consumption.

---

## License

Copyright (c) 2026 First 10 Contributors.  
Licensed under the Apache License, Version 2.0.
