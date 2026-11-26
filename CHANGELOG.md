# Changelog

All notable changes to NexText are documented here. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning is [SemVer](https://semver.org/).

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
