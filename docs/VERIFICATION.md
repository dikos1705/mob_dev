# Task 3 verification — 2026-11-21

Local environment: Windows 11, Temurin JDK 21.0.13.1, Gradle 8.14.3, Android SDK/build tools 35, Pixel 6 emulator on Android 15 / API 35 with WHPX acceleration.

| Check | Observed result |
| --- | --- |
| Debug APK assembly | Passed |
| JVM tests | **13 passed**, zero failures |
| Room/repository instrumentation | **5 passed** |
| Compose UI/screenshot instrumentation | **7 passed** |
| Combined device run | **OK (12 tests)**, 35.842 seconds |
| Android lint | Completed with no errors; warnings concern newer dependency versions and inherited backup configuration |
| Additional-feature screenshots | Five actual 1080×2400 device captures in `docs/task3-screenshots/` |

The device tests use real Room databases, not mocked storage. They cover catalog CRUD and cascades, page/format edit conditions, session validation, transactional progress updates, ordered undo, repeated undo rejection, concurrent loan/progress actions, on-disk database close/reopen, one-time preference import and a collection that remains empty after every book is deleted.

UI tests exercise create/edit forms and errors, deletion cancel/confirm, borrowing/returning, manual page changes, notes, goals, search, saved books, session recording, disabled invalid actions, undo confirmation, cross-screen statistics, activity relaunch, form recreation and landscape rotation. The database reopen test proves persisted progress, notes, goals, sessions and deletions survive closing the storage connection and opening it again. Relaunch tests do not claim to simulate every Android process-kill scenario.

All five screenshot files are captured by `uiAutomation.takeScreenshot()` from the running app. The capture test prepares fictional history via the production repository, then records and undoes a new session through the visible UI. These images are not mockups or re-used midterm screenshots.

The initial UI run exposed snackbar-covered button clicks and an incorrect substring assertion. The final run passed after adding dismissible feedback and making test interactions wait for enabled controls. The Room tests passed throughout. Only final passing counts are reported above.

The instrumentation tests clear this app's database and legacy preferences on a test emulator; they should not run against a device containing user data to keep. They do not clear unrelated app data. Uninstall/clear-storage durability is not claimed.

GitHub Actions is configured to repeat build, JVM tests, lint and device tests, then publish the debug APK, reports and screenshots. Read the branch's Actions run for its actual CI result.
