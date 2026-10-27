# Verification — 27 October 2026

## Environment

- Windows 11 host; JetBrains Runtime JDK 21.0.3.
- Gradle Wrapper 8.14.3, Android Gradle Plugin 8.9.2, Kotlin 2.2.20.
- Android SDK Platform 35 and Build Tools 35.0.0.
- Pixel 6 AVD, Android 15 / API 35, Google APIs x86_64, WHPX acceleration.
- Portrait display 1080 × 2400, 420 dpi; rotation also checked in the UI suite.

## Results

| Check | Result |
| --- | --- |
| `:app:assembleDebug` | PASS; installable debug APK produced |
| `:app:testDebugUnitTest` | PASS; 10 tests, 0 failures/errors |
| `:app:lintDebug` | PASS; 0 errors, 11 warnings (details below) |
| `:app:connectedDebugAndroidTest` | PASS; 5 tests, 0 failures/skips on the API 35 emulator |
| Screenshot capture/export | PASS; 5 actual display captures, referenced in README |

The ten JVM tests comprise seven domain tests and three ViewModel tests:

- Fourth loan refused; repeated borrowing does not reset the loan date.
- Returning keeps notes, saved status and pages while freeing a loan slot.
- Progress requires a loan and clamps to valid page bounds.
- Notes are trimmed/bounded; bookmarks toggle independently of loans.
- Goal boundaries are enforced.
- Rapid queued increments survive delayed persistence without losing updates.
- Save failure keeps the previous state and allows retry.
- Unknown IDs are ignored; an old snackbar completion cannot clear a newer notice.

The five instrumentation tests comprise four behavioral tests and one screenshot scenario:

- Borrow → record pages → save note → change goal → return; history stays intact.
- Search input survives Activity recreation; an unsaved note survives recreation and actual landscape/portrait changes; active loan state stays intact.
- A newly launched Activity restores the saved loan and pages from preferences.
- Malformed JSON falls back to an empty library and the app remains usable.
- Capture Discover, book details, notes/progress, My shelf and Progress with seeded fictional history.

`test-results.json` contains case names and counts extracted from the generated JUnit XML, plus screenshot dimensions and checksums. Full HTML reports are regenerated under `app/build/reports/` by the commands in README. Screenshots were inspected visually for readable content and layout.

## Non-blocking diagnostics and scope

Lint reports nine notices about newer dependency versions, one recommendation to specify Android 12+ data-extraction rules, and one recommendation to use the preferences KTX editing helper. Versions are pinned for this verified build. Preference writes deliberately use `commit()` on the IO dispatcher so a failed write can be detected before publishing state. The manifest disables Android cloud backup; OEM device-transfer behavior has not been tested.

Gradle also reports deprecated APIs used by the Android build tooling. The wrapper is pinned to Gradle 8; upgrading to Gradle 9 is outside this build's verified configuration.

Runtime validation was performed on the listed emulator. Minimum SDK 26 is declared and checked by lint, but an API 26 device and physical hardware were not tested. Network/real library integration is not implemented or claimed. This report does not substitute for the student's live defense.
