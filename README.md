# Campus Library — Kotlin Fundamentals and OOP

A small **Kotlin/JVM console application** for Mobile Programming, Task 1.
Author: Dias. Repository: [dikos1705/mob_dev](https://github.com/dikos1705/mob_dev).
Browse and search a library catalog, borrow and return items, and view statistics.
Printed books and e-books have different loan policies. All sample catalog records are fictional.
There are no Android components or UI dependencies.

## Run

Requirements: **JDK 21** and an internet connection for the first dependency download.
The Gradle Wrapper is included; a separate Gradle installation is not needed.
On Windows, use a folder with a Latin-only path, for example `C:\Projects\mob_dev`.
Some Gradle/JDK combinations fail to load test classes from non-ASCII paths ([Gradle issue](https://github.com/gradle/gradle/issues/30391)).

Windows (PowerShell):

```powershell
.\gradlew.bat run --console=plain
.\gradlew.bat run --args="--interactive" --console=plain
.\gradlew.bat test
```

macOS/Linux:

```sh
./gradlew run --console=plain
./gradlew run --args="--interactive" --console=plain
./gradlew test
```

The default mode is a reproducible demo; `--demo` selects it explicitly. Interactive mode provides a numbered menu.
Use **1** to list items, **2** to search title/author/genre, **3** to borrow, **4** to return, **5** for statistics, and **0** to exit.
Borrow and return ask for the numeric item ID. Invalid input is handled without crashing; EOF exits.

In **IntelliJ IDEA**, open this folder as a Gradle project, choose **JDK 21** for the project and Gradle JVM, wait for synchronization, and run `main()` in `Main.kt`.
To use the menu, set the program argument to `--interactive`. Android Studio can open the same JVM project, but Android SDK setup is unnecessary.

## Where the requirements are demonstrated

| Requirement | Implementation |
| --- | --- |
| Variables and data types | `Main.kt`: `val`, `var`, `Int?`, `String`, `Boolean`; typed IDs, pages, loan duration |
| Conditions and loops | `borrow()` checks rules; menu uses `while` and `when`; catalog output uses `for` |
| List | `Library.catalog`, a `MutableList<LibraryItem>` |
| Set | `genres()` converts genre names to a unique `Set<String>` |
| Map | Members indexed by member ID; active loans indexed by item ID |
| `map`, `filter`, `reduce` | `findItems()`, `genres()`, `totalPages()`, and statistics output; empty input is handled before `reduce` |
| Functions, higher-order functions, lambdas | `findItems(predicate: (LibraryItem) -> Boolean)` accepts lambdas for genre, availability, and text searches |
| Classes and objects | `Library`, book classes, and the singleton `object DemoCatalog` |
| Inheritance | `PrintedBook` and `EBook` extend abstract `LibraryItem` |
| Interfaces and polymorphism | `Borrowable`; `borrow()` uses the interface to select 14-day printed-book or 7-day e-book behavior; catalog calls overridden `description()` |
| Data class | `BookDetails`, `Member`, and `Loan` in `Models.kt` |
| Sealed class | `BorrowResult.Success` / `BorrowResult.Rejected`, handled by an exhaustive `when` |
| Suspend function and coroutine | `DemoCatalog.load()` uses `suspend` and `delay`; `main()` uses `runBlocking`, `launch`, and `join` to load the catalog before accepting operations |
| Entry point | Top-level `main(args: Array<String>)` in `Main.kt` |

The coroutine simulates a slow catalog source. `delay(200)` suspends the coroutine; it is not a real network call.
The program prints a loading message, waits for completion with `join()`, and then uses the loaded records.

## Rules and scope

- A member can have up to three active loans by default.
- Each catalog ID represents one physical copy or one digital license; both can be loaned to only one member at a time.
- Only the borrowing member may return an item. Returning it makes it available again.
- Unknown IDs, duplicate registration/catalog IDs, unavailable items, and loan limits are handled explicitly.
- Data is stored in memory and resets on restart. There is no database, authentication, real e-book download, or due-date scheduling.
- The menu uses one registered demo member, Dias. The domain supports multiple members, as the tests demonstrate.

## Demo walkthrough and tests

The demo lists five items, filters programming titles, borrows a printed book for 14 days and an e-book for 7 days,
rejects a second loan of the same copy, returns the printed book, and prints **1 active loan**, **3 unique genres**, and **1100 catalog pages**.

`LibraryTest.kt` checks catalog loading, polymorphic loan rules, duplicate borrowing, returns and ownership,
invalid IDs, loan limits, collection results, empty catalogs, duplicate IDs, and domain validation.
The test report is generated at `build/reports/tests/test/index.html`.
All 9 tests passed on JDK 21 and Gradle 8.14.3 in a clean Latin-path checkout.
The demo, interactive borrow/return/search, invalid input, EOF, and argument handling were also checked.
See [the captured demo output](docs/demo-output.txt).

## Learning references

- [Kotlin Gradle configuration](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Kotlin coroutine basics](https://kotlinlang.org/docs/coroutines-basics.html)

See [DEFENSE_RU.md](DEFENSE_RU.md) for a short explanation in Russian.
