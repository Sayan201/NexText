# NexText

A privacy-first, 100% on-device smart SMS organizer for Android (`com.nextext.sms`), built with Kotlin, Jetpack Compose, Material 3, Room + FTS4, and WorkManager.

> **Zero Network Permissions:** NexText does not declare `android.permission.INTERNET`. All SMS categorization, OTP extraction, financial tracking, bill reminders, and search indexing run strictly offline on your device. Google Drive backups are written through the Storage Access Framework, so the Drive app performs the upload — NexText itself never touches the network.

## Features

- **Liquid Glass Design Language**: Pitch Black OLED-ready UI featuring a translucent **Liquid Glass** floating bottom capsule with interactive refraction, outward glass magnification on touch, and smooth spring-driven droplet tab transitions. Also includes an optional **Material 3** design mode, **Cyan** default accent (with customizable color accents), and **Swap Menu Bar Positions** to flip top and bottom navigation bars.
- **Smart On-Device Categorization**: Automatically sorts incoming and synced SMS into **Personal**, **Transactions**, **Promotions**, and **Spam** (with support for Indian DLT headers like `AD-HDFCBK`, `VK-SBIINB`, `AX-AXISBK`, plus custom per-sender overrides, blocklists, **Starred**, and **Archive** in the overflow menu).
- **Rich Notification Actions & Direct Thread Open**: Incoming SMS notifications support **Copy OTP**, **Direct Reply**, **Mark Read**, and **Delete** right from the shade, and tapping a notification opens that exact conversation thread.
- **1-Tap OTP Copy & Auto-Clean**: Detects 4–8 digit verification codes with a **Copy OTP** action directly in notifications, an inline Copy button in conversations, and optional 24-hour auto-cleanup for old OTPs.
- **Passbook & Expense Tracker**: Extracts INR (`₹`/`Rs.`), USD, EUR, and GBP debits, credits, UPI payments, card spends, ATM withdrawals, account last-4 digits, and available balances into a monthly spend dashboard.
- **Reminders & Trips / Deliveries**: Automatically extracts credit card bill due dates, EMIs, mobile recharges, IRCTC train PNRs, flight bookings, and courier/order tracking updates, plus custom reminders.
- **Default SMS App & Dual-SIM Support**: Implements `SmsReceiver` (`SMS_DELIVER`), `MmsReceiver` (`WAP_PUSH_DELIVER`), `HeadlessSmsSendService`, Dual-SIM selection, Quick Replies toggle, and full-text offline search (SQLite FTS4).
- **Backup & Auto-Backup Scheduling**: Export and restore messages, reminders, blocked senders, and category rules as a single JSON file. Pick a **Google Drive folder once** and NexText keeps exactly one backup file there, overwriting it on every **Daily / Weekly / Monthly** run — nothing stacks on the phone or in Drive. Local SAF export/import is still available. No Cloud project, OAuth client, API key, or `INTERNET` permission required.

## Google Drive Backup (SAF — no API key, no OAuth)

NexText writes its backup through Android's **Storage Access Framework**, not the Google Drive REST API. The Google Drive app itself performs the upload, so this app never opens a socket.

### How it works

1. **Settings → Backup & restore → Choose Drive folder.**
2. The system folder picker opens. Switch to the Google account you want in its drawer, then pick (or create) a folder — for example `NexText Backups`.
3. NexText calls `takePersistableUriPermission` and stores the folder URI. That URI *is* the account binding: every later backup goes to the folder under whichever account you selected. If that account is signed out of the phone, the write fails or queues in the Drive app — it never silently lands in another account.
4. Each backup run serializes the database to bytes in memory and writes them into a single document, `nextext-backup.json`, in that folder using `openOutputStream(doc, "wt")`. Truncate-and-write means the previous backup is replaced, not appended.

### Guarantees and limits

| Property | Behavior |
|---|---|
| Local copies | **None.** The backup is built in memory; no cache file is created. Legacy `files/auto_backups/` is deleted on the next worker run. |
| Drive copies | **One.** The same `nextext-backup.json` is overwritten each run. |
| Schedule | WorkManager periodic work, 1 / 7 / 30 days, exponential backoff, survives reboot. |
| System backup toggle | **Irrelevant.** Android Auto Backup is a separate 25 MB system snapshot; this path does not use it. |
| Server confirmation | **None.** The app confirms the bytes were written to the provider, not that Google received them. |
| Requirements | The Google Drive app must be installed and the folder grant must still be valid. |
| Account change | Unlink and pick a folder again. The app cannot enumerate accounts or query which one owns a URI. |

### Restoring

**Restore** reads `nextext-backup.json` from the linked folder and merges it — blocked senders, category overrides, messages, and reminders — exactly like the local SAF restore.

### If an upload fails

- **Grant revoked** (Drive data cleared, app uninstalled, permission removed): the write throws, the link is cleared automatically, and Settings shows *Choose Drive folder* again.
- **Account signed out / offline**: WorkManager retries with exponential backoff; the link is kept.
- **Folder deleted**: the next run recreates the document if the folder grant survives, otherwise the link is dropped.

### Why this needs no `INTERNET` permission

The Drive app holds the network access and the OAuth session. NexText only writes a document through a `ContentProvider` — the same mechanism used to save to a local file. The trade-off is that the backup file is visible in your Drive, and there is no programmatic account picker or hidden app-data folder (those require the Drive REST API with `drive.appdata`).


## Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose + Liquid Glass Design Language + Material 3 (Haze blur & custom AGSL/graphicsLayer refraction, Cyan/Dynamic/Pitch Black themes)
- **Database**: Room + SQLite FTS4
- **Preferences & Background Work**: Jetpack DataStore + WorkManager
- **CI/CD**: GitHub Actions (`.github/workflows/build.yml`) builds Debug & Release APKs and publishes GitHub Releases automatically.

## Building

### GitHub Actions (Recommended)
Every push to `main` automatically runs `testDebugUnitTest` and builds `NexText-Debug-APK` under the **Actions** tab.
Triggering the workflow manually (`workflow_dispatch`) or pushing a version tag (e.g. `v1.2.0`) also builds the Release APK and publishes a GitHub Release.

### Local Gradle
```bash
gradle testDebugUnitTest assembleDebug
```
