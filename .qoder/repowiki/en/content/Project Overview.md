# Project Overview

<cite>
**Referenced Files in This Document**
- [README.md](file://README.md)
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)
- [NotificationDatabase.kt](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [NotificationItem.kt](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt)
- [NotificationRepository.kt](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt)
- [NotificationAdapter.kt](file://app/src/main/java/com/notificationlog/adapter/NotificationAdapter.kt)
- [NotificationApp.kt](file://app/src/main/java/com/notificationlog/NotificationApp.kt)
- [AndroidManifest.xml](file://app/src/main/AndroidManifest.xml)
- [build.gradle](file://build.gradle)
- [app/build.gradle](file://app/build.gradle)
- [gradle.properties](file://gradle.properties)
- [scripts/validate-jdk.gradle](file://scripts/validate-jdk.gradle)
- [fastlane/metadata/android/en-US/full_description.txt](file://fastlane/metadata/android/en-US/full_description.txt)
- [fastlane/metadata/android/en-US/short_description.txt](file://fastlane/metadata/android/en-US/short_description.txt)
- [fastlane/metadata/android/en-US/title.txt](file://fastlane/metadata/android/en-US/title.txt)
</cite>

## Update Summary
**Changes Made**
- Updated build system specifications to reflect AGP 8.13.2, Gradle 8.14.3, and Kotlin 2.2.21
- Added comprehensive privacy concerns documentation from README.md
- Integrated F-Droid store metadata for app distribution information
- Enhanced security and privacy sections with detailed permission explanations
- Updated system requirements and JDK setup instructions

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Privacy and Security](#privacy-and-security)
9. [Build System and Requirements](#build-system-and-requirements)
10. [Troubleshooting Guide](#troubleshooting-guide)
11. [Conclusion](#conclusion)
12. [Appendices](#appendices)

## Introduction
NotificationLog is a real-time notification capture and management application for Android that prioritizes user privacy and data security. It continuously monitors system notifications via Android's NotificationListenerService, stores them locally using Room database, and presents them in a searchable, sortable list. The application emphasizes privacy by design — it has no network permissions, no server component, and no data transmission capabilities. Users can export logs as text files, take screenshots of the current screen, and manage notification capture with on/off toggles and persistent status notifications. The app targets users who need insight into their notification stream while maintaining complete control over their sensitive data, including power users, developers investigating notification behavior, and anyone interested in auditing notification activity without compromising privacy.

## Project Structure
The project follows a feature-based structure under the app module with clear separation of concerns:
- UI layer: MainActivity and RecyclerView adapter
- Presentation layer: MainViewModel implementing MVVM
- Domain layer: NotificationRepository
- Data layer: Room database, DAO, and entity
- Background service: NotificationListener (NotificationListenerService)
- Application bootstrap: NotificationApp

```mermaid
graph TB
subgraph "UI Layer"
MA["MainActivity<br/>RecyclerView + Menus"]
AD["NotificationAdapter<br/>List rendering"]
end
subgraph "Presentation Layer"
VM["MainViewModel<br/>LiveData + coroutines"]
end
subgraph "Domain Layer"
REPO["NotificationRepository<br/>business logic"]
end
subgraph "Data Layer"
DB["NotificationDatabase<br/>Room"]
DAO["NotificationDao<br/>queries"]
ENT["NotificationItem<br/>entity"]
end
subgraph "Background Service"
LS["NotificationListener<br/>NotificationListenerService"]
end
subgraph "App Bootstrap"
APP["NotificationApp<br/>Application"]
end
MA --> VM
MA --> AD
VM --> REPO
REPO --> DB
DB --> DAO
DAO --> ENT
LS --> REPO
APP --> REPO
```

**Diagram sources**
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)
- [NotificationRepository.kt](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt)
- [NotificationDatabase.kt](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [NotificationItem.kt](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt)
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)
- [NotificationApp.kt](file://app/src/main/java/com/notificationlog/NotificationApp.kt)

**Section sources**
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)
- [NotificationApp.kt](file://app/src/main/java/com/notificationlog/NotificationApp.kt)

## Core Components
- Real-time notification capture: NotificationListenerService intercepts notifications and persists them to Room.
- Local storage: Room database with a single entity and DAO for CRUD operations.
- UI and management: MainActivity with sorting, filtering, deletion, and export capabilities.
- MVVM presentation: MainViewModel exposes LiveData streams and orchestrates repository operations.
- Export and persistence: Text log export to external storage and screenshot capture to MediaStore.

Key features:
- Notification monitoring via NotificationListenerService
- Local storage with Room database
- Export logs as text files
- Screenshot capture of the current UI
- Sorting by date or app
- Toggle capture on/off and persistent status notification
- Privacy-first design with no network access

**Section sources**
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)
- [NotificationDatabase.kt](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [NotificationItem.kt](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt)
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)

## Architecture Overview
NotificationLog implements MVVM:
- Model: Room entity and DAO
- View: MainActivity and RecyclerView
- ViewModel: MainViewModel manages LiveData and delegates to repository
- Service: NotificationListener captures notifications and updates UI via broadcasts

```mermaid
classDiagram
class NotificationItem {
+long id
+string packageName
+string appName
+string title
+string content
+long timestamp
+string key
}
class NotificationDao {
+getAll() NotificationItem[]
+getNotificationsSortedByDate() NotificationItem[]
+getNotificationsSortedByApp() NotificationItem[]
+insert(notification)
+delete(notification)
+deleteAll()
}
class NotificationDatabase {
+notificationDao() NotificationDao
}
class NotificationRepository {
+getAllNotifications()
+getNotificationsSortedByDate()
+getNotificationsSortedByApp()
+insert(notification)
+delete(notification)
+deleteAll()
}
class MainViewModel {
+serviceStatus : LiveData~Boolean~
+notificationCount : LiveData~Int~
+notifications : LiveData~NotificationItem[]~
+checkNotificationServiceStatus()
+loadNotifications(sortType)
+deleteNotification(item)
+clearNotifications()
}
class MainActivity {
+setupRecyclerView()
+setupMenuBar()
+toggleCapture()
+saveTextLog()
+saveScreenshot()
+checkPermissions()
}
class NotificationListener {
+onNotificationPosted(sbn)
+onNotificationRemoved(sbn)
+onListenerConnected()
+onListenerDisconnected()
}
NotificationDatabase --> NotificationDao : "provides"
NotificationDao --> NotificationItem : "persists"
NotificationRepository --> NotificationDatabase : "uses"
MainViewModel --> NotificationRepository : "delegates"
MainActivity --> MainViewModel : "observes"
NotificationListener --> NotificationRepository : "inserts"
```

**Diagram sources**
- [NotificationItem.kt](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [NotificationDatabase.kt](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt)
- [NotificationRepository.kt](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt)
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)

## Detailed Component Analysis

### NotificationListener (Background Capture)
Responsibilities:
- Subscribes to system notifications via NotificationListenerService
- Extracts metadata (title, text, package, timestamp)
- Inserts into Room database and notifies UI via broadcast
- Handles disconnection events and requests rebind on newer Android versions
- Posts a high-priority warning notification when access is lost

```mermaid
sequenceDiagram
participant Sys as "Android System"
participant NL as "NotificationListener"
participant Repo as "NotificationRepository"
participant DB as "Room Database"
participant Act as "MainActivity"
Sys->>NL : "onNotificationPosted(StatusBarNotification)"
NL->>NL : "Extract title/text/package/timestamp"
NL->>Repo : "insert(NotificationItem)"
Repo->>DB : "DAO.insert(...)"
DB-->>Repo : "success"
NL->>Act : "Broadcast NOTIFICATIONS_UPDATED"
Act->>Act : "observe(...) refresh list"
```

**Diagram sources**
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)
- [NotificationRepository.kt](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)

**Section sources**
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)

### MainViewModel (Presentation Logic)
Responsibilities:
- Exposes LiveData for service status, notification count, and list
- Loads sorted lists from repository on demand
- Delegates delete/clear operations
- Observes and surfaces errors

```mermaid
flowchart TD
Start([Load Notifications]) --> ChooseSort{"Sort Type"}
ChooseSort --> |Date| CallDate["repo.getNotificationsSortedByDate()"]
ChooseSort --> |App| CallApp["repo.getNotificationsSortedByApp()"]
CallDate --> UpdateLive["Update LiveData<List>"]
CallApp --> UpdateLive
UpdateLive --> End([UI Updates])
```

**Diagram sources**
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)
- [NotificationRepository.kt](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt)

**Section sources**
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)

### MainActivity (UI and Actions)
Responsibilities:
- Manages permissions and service status
- Provides menus for sort, save, and about
- Supports screenshot and text log export
- Controls capture on/off and persistent status notification
- Observes ViewModel LiveData and broadcasts for updates

```mermaid
sequenceDiagram
participant User as "User"
participant UI as "MainActivity"
participant VM as "MainViewModel"
participant Repo as "NotificationRepository"
User->>UI : "Toggle Capture"
UI->>UI : "Persist preference"
UI->>UI : "Show/Hide persistent notification"
User->>UI : "Sort By Date/App"
UI->>VM : "loadNotifications(sortType)"
VM->>Repo : "fetch sorted list"
Repo-->>VM : "List<NotificationItem>"
VM-->>UI : "LiveData updates"
UI->>UI : "Update RecyclerView"
```

**Diagram sources**
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)
- [NotificationRepository.kt](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt)

**Section sources**
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)

### Data Layer (Room)
- Entity: NotificationItem with primary key and fields for app identity and content
- DAO: Queries for full list and sorted variants, insert/delete/clear
- Database: Single-table notifications database

```mermaid
erDiagram
NOTIFICATIONS {
long id PK
string packageName
string appName
string title
string content
long timestamp
string key
}
```

**Diagram sources**
- [NotificationItem.kt](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [NotificationDatabase.kt](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt)

**Section sources**
- [NotificationItem.kt](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [NotificationDatabase.kt](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt)

### Adapter (UI Rendering)
- RecyclerView adapter using DiffUtil for efficient list updates
- Binds NotificationItem to views and handles click/delete actions

**Section sources**
- [NotificationAdapter.kt](file://app/src/main/java/com/notificationlog/adapter/NotificationAdapter.kt)

## Dependency Analysis
- AndroidX: Core, AppCompat, Material, ConstraintLayout, Lifecycle (ViewModel/LiveData), Activity/Fragment KTX
- Room: Runtime, KTX, compiler (via KSP)
- Kotlin: Parcelize for Parcelable entity
- Manifest declares INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS
- NotificationListenerService declared with required meta-data

```mermaid
graph LR
AppGradle["app/build.gradle"] --> Room["androidx.room:*"]
AppGradle --> Life["androidx.lifecycle:*"]
AppGradle --> Core["androidx.core:core-ktx"]
AppGradle --> Parcel["kotlin-parcelize"]
Manifest["AndroidManifest.xml"] --> Perm1["POST_NOTIFICATIONS"]
Manifest --> Perm2["INTERNET"]
Manifest --> Perm3["ACCESS_NETWORK_STATE"]
Manifest --> NLSvc["NotificationListenerService"]
```

**Diagram sources**
- [build.gradle](file://build.gradle)
- [app/build.gradle](file://app/build.gradle)
- [AndroidManifest.xml](file://app/src/main/AndroidManifest.xml)

**Section sources**
- [build.gradle](file://build.gradle)
- [app/build.gradle](file://app/build.gradle)
- [AndroidManifest.xml](file://app/src/main/AndroidManifest.xml)

## Performance Considerations
- Background capture runs on IO dispatcher with coroutine supervision to avoid blocking UI.
- Room queries are executed off the main thread; LiveData updates UI safely.
- DiffUtil minimizes RecyclerView churn during frequent updates.
- Export operations write to external storage asynchronously and provide user feedback.
- Consider batching inserts if high-frequency bursts occur; current design inserts per notification.

## Privacy and Security
NotificationLog implements a privacy-first approach with multiple layers of protection:

### Permission Model
The application requires only two essential permissions:
- **Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`)**: Required for the core functionality of reading system notifications
- **`POST_NOTIFICATIONS` (API 33+)**: Used exclusively for displaying the app's own status and warning notifications

### Network Isolation
- **No network permissions**: The manifest deliberately excludes `INTERNET` and `ACCESS_NETWORK_STATE` permissions
- **No network code paths**: The application contains no networking capabilities, making it impossible to transmit data externally
- **Local-only storage**: All notification data remains in the local SQLite database

### Data Protection
- **Local SQLite storage**: Notification data is stored in a private SQLite database file
- **Backup disabled**: `allowBackup="false"` prevents system backup services from copying data
- **Data extraction rules**: Custom rules exclude both cloud backup and device transfer
- **No encryption at rest**: While data stays local, it's readable by root or anyone with access to app-private storage

### Privacy Implications
Users should be aware that:
- Notification bodies are captured verbatim, potentially containing sensitive information like OTP codes or private messages
- The database file is readable by root users or anyone with access to the device's unlocked storage
- There is no filtering mechanism for sensitive notification content
- No encryption is applied to stored data

**Section sources**
- [README.md:29-41](file://README.md#L29-L41)
- [fastlane/metadata/android/en-US/full_description.txt:3-16](file://fastlane/metadata/android/en-US/full_description.txt#L3-L16)

## Build System and Requirements
The project uses modern Android development tools with strict version requirements:

### Build Configuration
- **Android Gradle Plugin (AGP)**: 8.13.2
- **Gradle**: 8.14.3
- **Kotlin**: 2.2.21
- **KSP**: 2.2.21-2.0.5
- **JDK**: 21 (required for compilation)

### SDK Requirements
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 14)
- **Compile SDK**: 36 (Android 14)

### Development Setup
- **JDK 21**: Required for building the project
- **Gradle properties**: Configured with parallel builds and caching enabled
- **Validation script**: Automated JDK version checking prevents build failures

### Release Process
- **Keystore configuration**: Requires `keystore.properties` file (gitignored for security)
- **Signing**: Release builds require proper keystore configuration
- **ProGuard**: Enabled for release builds with custom rules

**Section sources**
- [build.gradle:1-7](file://build.gradle#L1-L7)
- [app/build.gradle:1-94](file://app/build.gradle#L1-L94)
- [gradle.properties:1-38](file://gradle.properties#L1-L38)
- [scripts/validate-jdk.gradle:1-94](file://scripts/validate-jdk.gradle#L1-L94)
- [README.md:42-70](file://README.md#L42-L70)

## Troubleshooting Guide
Common issues and resolutions:
- **Notification access disabled**: The app detects disabled access and prompts to enable it. Open Settings → Apps → Notification Access and enable the app.
- **Permission denied for POST_NOTIFICATIONS (Android 13+)**: Grant the permission when prompted; the app checks and requests it at runtime.
- **Listener disconnected**: On disconnection, a high-priority warning notification appears and a broadcast informs the UI to show a dialog. Re-enable notification access.
- **Export failures**: Ensure storage permissions are granted; on Android Q+, the app uses MediaStore; on older versions, it writes to public documents directory.
- **Persistent notification visibility**: Toggle "Show running notification" in the menu to display or hide the ongoing status notification.
- **JDK version errors**: Use `./gradlew validateJdk` to check JDK version; ensure JDK 21 is installed and configured in gradle.properties.
- **Build failures**: Verify keystore.properties exists for release builds; debug builds work without signing configuration.

**Section sources**
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)
- [scripts/validate-jdk.gradle:23-56](file://scripts/validate-jdk.gradle#L23-L56)

## Conclusion
NotificationLog provides a robust, privacy-focused, real-time notification capture and management solution built on modern Android technologies. Its MVVM architecture cleanly separates UI, presentation, domain, and data layers, while NotificationListenerService ensures reliable background capture. Room-backed local storage enables efficient querying and export capabilities. The application's commitment to privacy — through minimal permissions, no network access, and local-only storage — makes it suitable for both beginners seeking a straightforward notification viewer and advanced users needing auditability and export options while maintaining complete control over their sensitive data.

## Appendices

### System Requirements and Supported Versions
- **Minimum Android version**: Android 7.0 (API 24)
- **Target and compile SDK**: Android 14 (API 36)
- **JDK**: Version 21 (required for building)
- **Build tools**: AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21
- **Permissions**:
  - POST_NOTIFICATIONS (Android 13+)
  - Notification access (system setting)
- **Additional requirement**: Notification access must be granted to the app

**Section sources**
- [build.gradle:1-7](file://build.gradle#L1-L7)
- [app/build.gradle:21-40](file://app/build.gradle#L21-L40)
- [README.md:23-27](file://README.md#L23-L27)

### Installation Prerequisites
- Install JDK 21 and configure gradle.properties accordingly
- Grant notification access to the app in system settings
- On Android 13+, grant POST_NOTIFICATIONS permission when prompted
- Launch the app; it will guide you to enable notification access if not yet granted
- For release builds, create keystore.properties with signing configuration

**Section sources**
- [README.md:42-70](file://README.md#L42-L70)
- [gradle.properties:8-14](file://gradle.properties#L8-L14)
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)

### F-Droid Store Information
The application is optimized for F-Droid distribution with comprehensive metadata:

#### Store Metadata
- **Title**: Notification Log
- **Short Description**: Searchable history of every notification, even ones you swipe away
- **Full Description**: Comprehensive description emphasizing privacy and offline functionality

#### Version History
- **Version 1.0**: Initial release with core notification capture and management features
- **Version 1.1**: Fixed app name resolution for package visibility compliance
- **Version 1.2**: Improved connection status reporting and automatic recovery mechanisms

#### Distribution Features
- **Offline-first**: Works completely without internet connectivity
- **Privacy-focused**: No analytics, ads, or network communication
- **Open source**: Published under GNU General Public License v3.0
- **Source availability**: Complete source code available on GitHub

**Section sources**
- [fastlane/metadata/android/en-US/title.txt:1-2](file://fastlane/metadata/android/en-US/title.txt#L1-L2)
- [fastlane/metadata/android/en-US/short_description.txt:1-2](file://fastlane/metadata/android/en-US/short_description.txt#L1-L2)
- [fastlane/metadata/android/en-US/full_description.txt:1-21](file://fastlane/metadata/android/en-US/full_description.txt#L1-L21)
- [fastlane/metadata/android/en-US/changelogs/1.txt:1-6](file://fastlane/metadata/android/en-US/changelogs/1.txt#L1-L6)
- [fastlane/metadata/android/en-US/changelogs/2.txt:1-6](file://fastlane/metadata/android/en-US/changelogs/2.txt#L1-L6)
- [fastlane/metadata/android/en-US/changelogs/3.txt:1-8](file://fastlane/metadata/android/en-US/changelogs/3.txt#L1-L8)