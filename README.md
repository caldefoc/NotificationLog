# Notification Log

An Android app that keeps a searchable log of every notification your phone receives, including the ones you swipe away. It listens through `NotificationListenerService`, stores each notification in a local Room database, and shows them as a list you can sort, save as a text log, or save as a screenshot.

Nothing leaves the device — the database is local SQLite, and there is no server component.

Started January 2026 as a small utility and pushed in April; it's the earliest of the Android projects in this collection.

## What it does

- **Captures notifications** as they arrive via a `NotificationListenerService`, recording the package name, app label, title, body and timestamp.
- **Stores them in Room** (`notifications` table, one row per notification, auto-incrementing id), so history survives reboots and clearing the notification shade.
- **Lists them** in a RecyclerView with a sort menu and a file/save menu.
- **Capture toggle** — you can stop and start recording without revoking notification access.
- **Exports** — "Save Text Log" writes the current list as `notification_log_<timestamp>.txt` to `Documents/NotificationLog`; "Save Screenshot" draws the list view to `notification_log_<timestamp>.png` in `Pictures/NotificationLog` (both via MediaStore on API 29+, direct file writes below that).
- **Clear all**, behind a confirmation dialog.

## Android side

| | |
|---|---|
| Language | Kotlin (no Compose — classic Views + DataBinding) |
| Build | Gradle 8.14.3, AGP 8.13.2, Kotlin 2.2.21, KSP 2.2.21-2.0.5, JDK 21 |
| SDK | minSdk 24, targetSdk 36, compileSdk 36 |
| Storage | Room 2.8.5 (KSP), schema exported to `app/schemas` |
| Architecture | Activity + ViewModel + Repository + DAO, `viewModelScope` for coroutines |
| Signing | `keystore.properties` at the repo root, read by `app/build.gradle` — **gitignored, never committed** |

## Permissions — why it needs them

Two, and only two:

| Permission | Why |
|---|---|
| Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`) | The whole point: the system only lets a listener service read notifications if the user grants it in Settings |
| `POST_NOTIFICATIONS` (API 33+) | The app posts its own status/warning notification |

There is deliberately **no `INTERNET` permission and no `ACCESS_NETWORK_STATE`**. The manifest declares neither, so the app cannot make a network request even by accident — a far stronger privacy claim than promising not to upload anything.

The database does not leave the device either: `allowBackup="false"` with `dataExtractionRules` excluding both cloud backup and device transfer, so a new phone starts empty.

## Building

JDK 21 is required — the project compiles to Java 21 bytecode. If your default JDK is older, point Gradle at a 21 install:

```properties
# gradle.properties
org.gradle.java.home=C:\\Path\\To\\Your\\JDK-21
```

Then:

```bash
./gradlew validateJdk      # custom check: fails loudly if the JDK is wrong
./gradlew assembleDebug
./gradlew assembleRelease  # only works if keystore.properties exists
```

`scripts/validate-jdk.gradle` is applied from the root `build.gradle` and fails the build early with a readable message rather than a wall of Kotlin compiler errors. See `README-JDK-SETUP.md` for the short version.

For a release build, create `keystore.properties` (gitignored):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without that file the `release` build type has an empty signing config and will not produce an installable APK.

## First run

1. Install the debug APK.
2. Tap **Enable Notification Access** → the system settings page opens → grant access to Notification Log.
3. On Android 13+, grant the notification permission prompt.
4. Tap **Start Capture**.

Until step 2 is done the list stays empty and the status line says so — that's the most common confusion point, and the app does try to explain it with a dialog.

## Layout

```
app/src/main/java/com/notificationlog/
  MainActivity.kt               list UI, menus, permission flow, capture toggle
  NotificationListener.kt       the NotificationListenerService that records arrivals
  NotificationArchiveService.kt background archive service
  MainViewModel.kt, viewmodel/  ViewModel exposing the list as LiveData/Flow
  data/NotificationDao.kt       Room queries
  data/NotificationDatabase.kt  Room database definition
  repository/                   access layer over the DAO
  model/NotificationItem.kt     the stored entity
  adapter/NotificationAdapter.kt  RecyclerView adapter
  util/NotificationHelper.kt    posting the app's own notifications
scripts/validate-jdk.gradle     JDK 21 guard
```

## Known gaps

- **No tests** of any kind, and no CI.
- **No search.** The list can be sorted and exported, but not filtered by text or by app.
- Notification bodies are captured verbatim, which means OTP codes and private messages land in the database. There is no filtering by app and no encryption at rest, so the file is readable by root, or by anyone who can read app-private storage on an unlocked device. Cloud backup and device transfer are disabled, so it is at least not copied off the device.
- Sorting and export are basic — the text log is a dump of the current list, not a formatted archive.
- Release builds need a local `keystore.properties`; the signing key is not in the repository, so a fresh clone can only build debug.

## Licence

Copyright (C) 2026 CaldeFoc

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, **version 3 only** — not "or any later version".

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. The full text is in [LICENSE](LICENSE).

The licence covers the source code. It grants no rights to the CaldeFoc name or marks.
