# NexText

A privacy-first, 100% on-device smart SMS organizer for Android (`com.nextext.sms`), built with Kotlin, Jetpack Compose, Material 3, Room + FTS4, and WorkManager.

> **Zero Network Permissions:** NexText does not declare `android.permission.INTERNET`. All SMS categorization, OTP extraction, financial tracking, bill reminders, and search indexing run strictly offline on your device.

## Features

- **Liquid Glass Design Language**: Pitch Black OLED-ready UI featuring a translucent **Liquid Glass** floating bottom capsule with interactive refraction, outward glass magnification on touch, and smooth spring-driven droplet tab transitions. Also includes an optional **Material 3** design mode, **Cyan** default accent (with customizable color accents), and **Swap Menu Bar Positions** to flip top and bottom navigation bars.
- **Smart On-Device Categorization**: Automatically sorts incoming and synced SMS into **Personal**, **Transactions**, **Promotions**, and **Spam** (with support for Indian DLT headers like `AD-HDFCBK`, `VK-SBIINB`, `AX-AXISBK`, plus custom per-sender overrides, blocklists, **Starred**, and **Archive** in the overflow menu).
- **Rich Notification Actions & Direct Thread Open**: Incoming SMS notifications support **Copy OTP**, **Direct Reply**, **Mark Read**, and **Delete** right from the shade, and tapping a notification opens that exact conversation thread.
- **1-Tap OTP Copy & Auto-Clean**: Detects 4–8 digit verification codes with a **Copy OTP** action directly in notifications, an inline Copy button in conversations, and optional 24-hour auto-cleanup for old OTPs.
- **Passbook & Expense Tracker**: Extracts INR (`₹`/`Rs.`), USD, EUR, and GBP debits, credits, UPI payments, card spends, ATM withdrawals, account last-4 digits, and available balances into a monthly spend dashboard.
- **Reminders & Trips / Deliveries**: Automatically extracts credit card bill due dates, EMIs, mobile recharges, IRCTC train PNRs, flight bookings, and courier/order tracking updates, plus custom reminders.
- **Default SMS App & Dual-SIM Support**: Implements `SmsReceiver` (`SMS_DELIVER`), `MmsReceiver` (`WAP_PUSH_DELIVER`), `HeadlessSmsSendService`, Dual-SIM selection, Quick Replies toggle, and full-text offline search (SQLite FTS4).
- **Backup & Auto-Backup Scheduling**: Export and restore messages, reminders, blocked senders, and category rules to a local or cloud-synced (Google Drive) JSON file via Android's Storage Access Framework, with configurable **Daily / Weekly / Monthly** auto-backup scheduling.

## Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose + Liquid Glass Design Language + Material 3 (Haze blur & custom AGSL/graphicsLayer refraction, Cyan/Dynamic/Pitch Black themes)
- **Database**: Room + SQLite FTS4
- **Preferences & Background Work**: Jetpack DataStore + WorkManager
- **CI/CD**: GitHub Actions (`.github/workflows/build.yml`) builds Debug & Release APKs and publishes GitHub Releases automatically.

## Building

### GitHub Actions (Recommended)
Every push to `main` automatically runs `testDebugUnitTest` and builds `NexText-Debug-APK` under the **Actions** tab.
Triggering the workflow manually (`workflow_dispatch`) or pushing a version tag (e.g. `v1.1.0`) also builds the Release APK and publishes a GitHub Release.

### Local Gradle
```bash
gradle testDebugUnitTest assembleDebug
```
