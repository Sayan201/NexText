# Changelog

All notable changes to NexText are documented here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning is [SemVer](https://semver.org/).

## [1.6.6] — 2026-11-28

### Fixed
- **The droplet reads as floating above the capsule.** It casts a soft contact shadow onto the slab before it draws, and the capsule's real top and bottom rims are painted through the glass at their true position. The slab is clear glass with a 1 dp rim, so a refracted sample alone was almost invisible — now you see the capsule edge pass behind the bubble.
- **Light theme reaches the message pages.** Inbox, Thread, Finance, Smart Hub, smart cards, menus and dialogs were painting literal `Color.Black` / `Color.White` and ignoring the scheme, so only Settings turned light. Every surface and foreground now reads from `MaterialTheme.colorScheme`.
- **Changing the theme colour repaints the app.** Because the screens no longer hardcode black, a new accent propagates to every screen instead of stopping at Settings.

### Changed
- **Pitch Black and Dark were the same theme; Pitch Black is gone.** Dark *is* the AMOLED black scheme. A stored `PITCH_BLACK` preference migrates to `DARK` on read, so nothing is lost on upgrade.
- The navigation bar carries a little more vertical padding, giving the droplet room to clear the capsule symmetrically instead of clipping at the screen edge.

## [1.6.5] — 2026-11-27

### Changed
- **The droplet pops symmetrically.** It is centred on the capsule now, so it breaks the top and bottom edges by the same margin on every frame. The one-sided upward lift is gone.
- **It only exists while you are touching the bar.** The droplet dissolves in on press, drag or travel and dissolves out once the bar settles, leaving the accent chip as the resting state — no resident bubble.

### Kept
- The glass-over-glass read: the droplet still refracts the capsule it sits on, so the slab's body and rim bend through it where the two overlap.

## [1.6.4] — 2026-11-27

### Fixed
- **The selection droplet leaves the capsule.** It was confined inside the glass slab; it now renders outside the bar's bounds. At rest it sits a little proud of the rim, and on tap, press or drag it inflates past the capsule's top and bottom edges on an overshooting spring (damping 0.42), so the bubble visibly pops out of the bar and settles back into it.
- **The droplet now refracts the capsule itself.** The glass slab is recorded into its own layer and composited *under* the screen backdrop inside the droplet's shader input, so the capsule's body and rim bend through the bubble where the two overlap. Previously the droplet's only sample was the content behind the bar, so it swallowed a bite out of the capsule's outline.
- **In-app update asset selection** prefers `app-github-release.apk` and ignores any asset with `debug` in the name, so a release carrying both APKs can no longer hand a debug build to an install.

### Changed
- The droplet's corner radius tracks its height, so the inflated state is a full stadium bubble instead of a stretched rounded rectangle.
- The droplet is always present rather than fading out to a flat accent chip, so the selected destination reads as glass at rest as well as mid-switch.

## [1.6.3] — 2026-11-27

### Changed
- **Selector is now a jelly lens, not a chip that sits.** Ported `IndicatorJellyPhysics` from the reference (measured from UIKit): the lifted lens dissolves in over the resting chip on press/drag/travel, stretches along the travel direction (+19% width, −10% height), and squashes/bulges on the rebound past the target. Velocity is sampled per frame from the sliding pill.
- **Selected content magnifies 1.16×** while the lens is lifted, matching the reference's `magnification`.
- **Frost slider now has a live swatch.** A 44 dp glass tile sits beside the descriptor and blurs "Aa" from sharp to milky as you drag, so the change is impossible to miss.

### Removed
- The icon scale-pop and indicator scale-pop guesses from 1.6.0/1.6.1 — the reference motion is the jelly lens, not a scale.

## [1.6.2] — 2026-11-27

### Added
- **`github` and `fdroid` product flavors.** The `fdroid` flavor ships no `INTERNET` and no `REQUEST_INSTALL_PACKAGES` permission — the AppUpdater is replaced by an offline stub, so the F-Droid build carries zero network and zero self-install code.
- **Fastlane metadata** under `fastlane/metadata/android/en-US/` — title, short and full descriptions, five phone screenshots, and per-versionCode changelogs.

### Changed
- CI builds and unit-tests both flavors (`testGithubDebugUnitTest`, `testFdroidDebugUnitTest`) and assembles both release variants.

## [1.6.1] — 2026-11-27

### Fixed
- Selector pop now fires on the actual tap (was keyed on the animation-derived active flag, so it rarely triggered). Icon springs 0.7 → 1.0 on selection, scales down to 0.82 while pressed, and the indicator chip pops 0.72 → 1.0.

## [1.6.0] — 2026-11-27

### Changed
- **Selector motion now matches Zangetsu.** The oversized glass droplet that sat under the tab is gone. The selected tab is a modest accent chip that slides, and the **icon springs from 0.7 to 1.0** with an overshoot (380 ms, easeOutBack) the moment the tab becomes selected — the same pop the reference app uses.
- **Frost slider is legible.** The settings preview now sits over high-contrast text and the blur range was widened (1 → 16 dp) with a stronger milky lift, so the difference between 0% and 100% is obvious. A live descriptor under the slider reads Crystal clear / Light frost / Frosted / Heavy frost.

### Fixed
- The droplet no longer overhangs the capsule at rest.

## [1.5.0] — 2026-11-27

### Added
- **Liquid Glass droplet on the nav bar.** The selected tab now sits inside a persistent glass pill that pops wider and taller on every switch. Rim refraction, a 12-tap frosted body, a milky film and a soft outer glow, ported from Zangetsu's `bodyMode: clear` glass settings.
- **Frost slider.** *Glass navigation bar size and style* now controls frost: **0% is crystal clear** — pure edge refraction, no blur — and **100% is heavy frost**. The pill's body is frosted from the inside out, never covered by a scrim.

### Changed
- **Opacity is gone, and so is the black tint.** The old slider darkened the glass toward black; frost now works by blurring and lifting the backdrop, so the bar keeps its colour and the content behind it stays readable.
- **The selected tab keeps its bubble at rest.** Previously the droplet only appeared mid-switch and collapsed to a flat accent tint. It is now always visible, so the active destination reads as glass even when the bar is idle.
- **Active icon uses the accent colour** on clear glass instead of flipping to white, matching the accent-on-glass look of the reference.
- The stored preference key is unchanged (`glass_opacity_pct`), so existing installs keep their saved value. New installs default to 35%.
- Settings copy and the summary row now say **Frost** instead of **Opacity**.

## [1.4.0] — 2026-11-27

### Added
- **HyperOS-style Smart SMS Cards** for tickets, OTP, banking, billing and delivery messages, with the action the message implies surfaced on the card.

### Changed
- **SMS list separator contrast raised to 85%** so threads separate cleanly on AMOLED instead of bleeding into one another.

## [1.3.0] — 2026-11-27

### Added
- **In-app updates from GitHub Releases.** On launch (and on demand from Settings) NexText fetches `api.github.com/repos/Sayan201/NexText/releases/latest`, compares the tag with the installed `versionName`, and shows the release notes in a flyout when a newer build exists. The APK downloads to cache with a progress bar and hands off to Android's package installer.
- **Update capsule.** Dismissing a version keeps a yellow **Update** capsule in the inbox header, so the release stays one tap away.

### Changed
- **`INTERNET` is now declared.** It is used by one file, `update/AppUpdater.kt`, for the release check and the APK download. Nothing else in the app opens a connection.
- **`REQUEST_INSTALL_PACKAGES` added** so a downloaded update can be handed to the package installer.
- `FileProvider` returns, scoped to `cache/updates/` only, to expose the downloaded APK to the installer.
- The Drive backup path is unchanged: it still writes through SAF and needs no network permission of its own.

### Documentation
- README rewritten around the real network surface: what stays on the device, every request the app can make, and what the update check does not send.

## [1.2.0] — 2026-11-26

### Added
- **Persistent Google Drive backup folder.** Pick a folder once via the system folder picker (`ACTION_OPEN_DOCUMENT_TREE`); NexText keeps the grant with `takePersistableUriPermission` and writes into it from then on. The URI is the account binding — backups go to the Google account chosen in the picker, and fail or queue if that account is signed out.
- **Automatic Drive upload.** The `AutoBackupWorker` now writes the backup straight into the linked Drive folder on the Daily / Weekly / Monthly schedule. The Google Drive app performs the sync, so this works with Android's device-backup toggle off.
- **Single-file overwrite.** Backups are serialized in memory and written to `nextext-backup.json` with `openOutputStream(doc, "wt")`. No timestamped files, no stacking — one file on the phone (none, actually) and one in Drive.
- **Drive status in Settings.** Linked folder name, last upload time, *Back up now*, *Restore*, *Change folder*, and *Unlink* actions. A warning appears when a schedule is enabled with no folder linked.
- **Self-healing link.** If the folder grant is revoked (Drive data cleared, app uninstalled, permission removed), the worker clears the link so Settings asks for a folder again instead of failing silently.

### Changed
- **Manual "Back up to Drive" is now a direct folder write.** It no longer creates a timestamped cache file and hands it to the Drive app via `ACTION_SEND`. Same single file, same overwrite semantics as the schedule.
- **Auto Backup subtitle corrected.** It previously claimed the local snapshot synced through Android's Google Drive device backup. `auto_backups/` was never in `backup_rules.xml`, so that was false. The schedule now genuinely uploads, and the text says how.
- **Restore from Drive** reads the linked folder directly instead of opening the SAF file picker.
- `BackupManager` refactored: `exportToBytes()` / `importFromBytes()` added so no intermediate file is ever written; `exportToUri()` and `importFromUri()` now share `buildPayload()` / `importPayload()`.

### Removed
- `FileProvider` (`${applicationId}.fileprovider`) and `res/xml/file_paths.xml`. Nothing shares a file URI any more.
- Legacy `files/auto_backups/` local snapshots — deleted on the first run of the new worker.

### Notes
- Still **no `INTERNET` permission**: the Drive app owns the network and the OAuth session.
- Requires the Google Drive app to be installed. De-Googled devices have no DocumentsProvider for Drive.
- There is no server-side confirmation, no programmatic account picker, and the backup file is visible in Drive. Hidden app-data storage (`drive.appdata`) requires the Drive REST API.

### Documentation
- README rebuilt around real screenshots (`docs/screenshots/`), a feature tour, the full Drive backup reference, a download table, and an FAQ.
- Release notes for `v1.2.0` mirror the same documentation.

## [1.1.0] — 2026-10-30

### Added
- Liquid Glass refinements, Starred overflow menu, swapped navigation bar positions, Quick Replies, About/Version/Check Updates.
- Auto Backup schedule (Daily / Weekly / Monthly).

### Changed
- README updated for the Liquid Glass design language.

## [1.0.0] — Initial release

- On-device SMS categorization, OTP extraction and auto-clean, finance/transaction tracking, reminders and delivery tracking, Dual-SIM support, FTS4 search, local JSON backup and restore.
