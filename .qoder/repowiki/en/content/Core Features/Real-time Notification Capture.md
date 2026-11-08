# Real-time Notification Capture

<cite>
**Referenced Files in This Document**
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)
- [NotificationRepository.kt](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [NotificationDatabase.kt](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt)
- [NotificationItem.kt](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt)
- [NotificationAdapter.kt](file://app/src/main/java/com/notificationlog/adapter/NotificationAdapter.kt)
- [NotificationApp.kt](file://app/src/main/java/com/notificationlog/NotificationApp.kt)
- [AndroidManifest.xml](file://app/src/main/AndroidManifest.xml)
- [notification_service.xml](file://app/src/main/res/xml/notification_service.xml)
</cite>

## Update Summary
**Changes Made**
- Enhanced NotificationListener with mutex-based synchronization for thread-safe operations
- Implemented duplicate prevention mechanism using notification keys and content comparison
- Added updateCount field tracking for improved data integrity
- Updated database schema with migration support for new fields
- Enhanced repository methods for duplicate handling and data compaction

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)

## Introduction
This document explains the real-time notification capture feature implemented in the application. It covers the NotificationListenerService background operation, Android notification system integration, the onNotificationPosted lifecycle, notification extraction and transformation pipeline, permissions and consent flow, coroutine-based asynchronous processing, Room database insertion, and broadcast communication with the main activity. The system now includes enhanced thread safety, duplicate prevention, and improved data integrity mechanisms.

## Project Structure
The notification capture feature spans several layers:
- Android Service layer: NotificationListenerService subclass handles background notification events with mutex-based synchronization.
- Presentation layer: MainActivity and MainViewModel manage UI, user actions, and state.
- Data layer: Room database with DAO and Repository encapsulate persistence and queries with duplicate prevention.
- Model layer: NotificationItem defines the persisted entity with updateCount tracking.
- Adapter layer: RecyclerView adapter renders captured notifications.

```mermaid
graph TB
subgraph "Android Service Layer"
NL["NotificationListener<br/>Background capture with mutex sync"]
end
subgraph "Presentation Layer"
MA["MainActivity<br/>UI & user actions"]
VM["MainViewModel<br/>State & queries"]
AD["NotificationAdapter<br/>RecyclerView binding"]
end
subgraph "Data Layer"
DB["NotificationDatabase<br/>Room DB v2"]
DAO["NotificationDao<br/>Queries & inserts"]
REPO["NotificationRepository<br/>Domain operations"]
end
subgraph "Model Layer"
NI["NotificationItem<br/>Entity with updateCount"]
end
NL --> REPO
REPO --> DAO
DAO --> DB
VM --> REPO
MA --> VM
MA --> AD
AD --> NI
```

**Diagram sources**
- [NotificationListener.kt:23-211](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L211)
- [MainActivity.kt:43-717](file://app/src/main/java/com/notificationlog/MainActivity.kt#L43-L717)
- [MainViewModel.kt:15-121](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L15-L121)
- [NotificationRepository.kt:6-92](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L92)
- [NotificationDao.kt:6-68](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L68)
- [NotificationDatabase.kt:7-43](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L43)
- [NotificationItem.kt:8-35](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L8-L35)
- [NotificationAdapter.kt:13-59](file://app/src/main/java/com/notificationlog/adapter/NotificationAdapter.kt#L13-L59)

**Section sources**
- [AndroidManifest.xml:29-43](file://app/src/main/AndroidManifest.xml#L29-L43)
- [notification_service.xml:1-5](file://app/src/main/res/xml/notification_service.xml#L1-L5)

## Core Components
- **Enhanced NotificationListener**: Background service that receives notifications via onNotificationPosted with mutex-based synchronization, transforms them, prevents duplicates, persists asynchronously, and notifies the UI.
- **MainActivity**: Orchestrates permissions, service status, persistent notification, and UI updates via broadcasts.
- **MainViewModel**: Manages service status checks, loads notifications, and exposes LiveData to the UI.
- **Enhanced NotificationRepository**: Encapsulates DAO operations for Room with duplicate prevention and data compaction capabilities.
- **Enhanced NotificationDao**: Defines queries and insert/delete operations with bumpUpdateCount functionality.
- **Updated NotificationDatabase**: Room database definition with migration support for v2 schema.
- **Enhanced NotificationItem**: Entity stored in Room with updateCount and lastUpdated fields.
- **NotificationAdapter**: RecyclerView adapter for displaying notifications.

**Section sources**
- [NotificationListener.kt:23-211](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L211)
- [MainActivity.kt:43-717](file://app/src/main/java/com/notificationlog/MainActivity.kt#L43-L717)
- [MainViewModel.kt:15-121](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L15-L121)
- [NotificationRepository.kt:6-92](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L92)
- [NotificationDao.kt:6-68](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L68)
- [NotificationDatabase.kt:7-43](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L43)
- [NotificationItem.kt:8-35](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L8-L35)
- [NotificationAdapter.kt:13-59](file://app/src/main/java/com/notificationlog/adapter/NotificationAdapter.kt#L13-L59)

## Architecture Overview
The system integrates Android's NotificationListenerService with Room persistence and a reactive UI. The service runs in the background with mutex-based synchronization, captures notifications with duplicate prevention, and dispatches broadcasts to inform the UI. The UI listens for these broadcasts and refreshes the list.

```mermaid
sequenceDiagram
participant Sys as "Android System"
participant NL as "NotificationListener"
participant Repo as "NotificationRepository"
participant DAO as "NotificationDao"
participant DB as "NotificationDatabase"
participant MA as "MainActivity"
participant VM as "MainViewModel"
Sys->>NL : "onNotificationPosted(StatusBarNotification)"
NL->>NL : "Extract title, text, package, postTime"
NL->>NL : "Mutex lock for thread safety"
NL->>Repo : "latestForKey(key)"
Repo->>DAO : "latestForKey(...)"
DAO-->>Repo : "Latest notification or null"
alt Duplicate detected (same key + content)
NL->>Repo : "bumpUpdateCount(id, timestamp)"
Repo->>DAO : "UPDATE SET updateCount + 1"
else New notification
NL->>Repo : "insert(NotificationItem)"
Repo->>DAO : "INSERT"
DAO->>DB : "persist"
end
NL->>MA : "Broadcast 'NOTIFICATIONS_UPDATED' with last_captured"
MA->>VM : "observe broadcast and reload notifications"
VM->>Repo : "loadNotifications(sorted)"
Repo->>DAO : "queries"
DAO-->>Repo : "List<NotificationItem>"
Repo-->>VM : "List<NotificationItem>"
VM-->>MA : "LiveData updates"
MA-->>MA : "Adapter submitList(...)"
```

**Diagram sources**
- [NotificationListener.kt:121-190](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L121-L190)
- [NotificationRepository.kt:22-30](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L22-L30)
- [NotificationDao.kt:41-49](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L41-L49)
- [MainActivity.kt:51-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L51-L66)
- [MainViewModel.kt:59-77](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L59-L77)

## Detailed Component Analysis

### Enhanced NotificationListener Service
Responsibilities:
- Receives notifications via onNotificationPosted with mutex-based synchronization.
- Extracts metadata (title, text, package, post time).
- Resolves app label from package manager.
- Checks user preference to capture or ignore.
- Performs duplicate detection using notification keys and content comparison.
- Inserts into Room using a coroutine on IO dispatcher with thread safety.
- Broadcasts updates to MainActivity.
- Handles connection/disconnection and shows a warning notification when access is revoked.

Key behaviors:
- **Mutex-based synchronization**: Uses `insertMutex` to prevent race conditions during concurrent notification processing.
- **Duplicate prevention**: Compares incoming notifications with existing ones using key and content matching.
- **Update counting**: Increments `updateCount` for repeated notifications instead of creating duplicates.
- onListenerConnected: Cancels a previous warning notification.
- onListenerDisconnected: Requests rebind on Android 13+, shows a high-priority warning notification, and broadcasts a disconnection event.
- onNotificationPosted: Builds NotificationItem, performs duplicate check, launches coroutine to persist, and broadcasts completion.
- onNotificationRemoved: Logs removal events.
- cancelNotificationByKey: Safely cancels a notification with SecurityException handling.

```mermaid
classDiagram
class NotificationListener {
+onListenerConnected()
+onListenerDisconnected()
+onNotificationPosted(sbn)
+onNotificationRemoved(sbn)
-showAccessRevokedNotification()
-cancelNotificationByKey(key)
-insertMutex : Mutex
-scope : CoroutineScope
-repository : NotificationRepository
}
class NotificationRepository {
+getAllNotifications()
+getNotificationsSortedByDate()
+getNotificationsSortedByApp()
+insert(notification)
+bumpUpdateCount(id, lastUpdated)
+latestForKey(key)
+compactDuplicates()
+delete(notification)
+deleteAll()
+deleteMatching(pattern)
}
class NotificationDao {
+getAll()
+getNotificationsSortedByDate()
+getNotificationsSortedByApp()
+insert(notification)
+bumpUpdateCount(id, lastUpdated)
+latestForKey(key)
+delete(notification)
+deleteAll()
+deleteMatching(pattern)
}
class NotificationDatabase {
+notificationDao()
+MIGRATION_1_2
}
class NotificationItem {
+id : Long
+packageName : String
+appName : String
+title : String
+content : String
+timestamp : Long
+key : String?
+updateCount : Int
+lastUpdated : Long
}
NotificationListener --> NotificationRepository : "uses"
NotificationRepository --> NotificationDao : "delegates"
NotificationDao --> NotificationDatabase : "operates on"
NotificationRepository --> NotificationItem : "persists"
```

**Diagram sources**
- [NotificationListener.kt:23-211](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L211)
- [NotificationRepository.kt:6-92](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L92)
- [NotificationDao.kt:6-68](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L68)
- [NotificationDatabase.kt:7-43](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L43)
- [NotificationItem.kt:8-35](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L8-L35)

**Section sources**
- [NotificationListener.kt:46-77](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L46-L77)
- [NotificationListener.kt:121-190](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L121-L190)
- [NotificationListener.kt:192-209](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L192-L209)

### Enhanced Notification Extraction and Transformation Pipeline
Extraction steps:
- Retrieve package name and extras from StatusBarNotification.
- Extract title and text from extras.
- Use postTime as timestamp.
- Resolve app name from package manager; fallback to package name on failure.
- Read user preference flag to decide whether to capture.

Enhanced transformation:
- **Thread-safe processing**: All database operations are wrapped in mutex locks.
- **Duplicate detection**: Compares incoming notification with latest entry using key and content.
- **Update counting**: Increments updateCount for identical notifications instead of creating duplicates.
- **Timestamp tracking**: Updates lastUpdated field for better chronological ordering.

```mermaid
flowchart TD
Start(["onNotificationPosted"]) --> GetExtras["Get extras from notification"]
GetExtras --> ExtractFields["Extract title, text, package, postTime"]
ExtractFields --> ResolveApp["Resolve app name from package"]
ResolveApp --> CheckFlag{"Capture enabled?"}
CheckFlag --> |No| Skip["Skip capture and log"]
CheckFlag --> |Yes| AcquireLock["Acquire mutex lock"]
AcquireLock --> CheckDuplicate{"Duplicate detected?"}
CheckDuplicate --> |Yes| BumpCount["Bump updateCount + 1"]
CheckDuplicate --> |No| BuildItem["Build NotificationItem"]
BumpCount --> Broadcast["Broadcast 'NOTIFICATIONS_UPDATED'"]
BuildItem --> Persist["Persist via Repository.insert"]
Persist --> Broadcast
Broadcast --> ReleaseLock["Release mutex lock"]
ReleaseLock --> End(["Exit"])
Skip --> End
```

**Diagram sources**
- [NotificationListener.kt:121-190](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L121-L190)

**Section sources**
- [NotificationListener.kt:121-190](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L121-L190)

### Enhanced Database Insertion Workflow
- Repository delegates DAO insert with duplicate prevention.
- DAO uses REPLACE conflict strategy to handle duplicates.
- **New bumpUpdateCount method**: Atomically increments updateCount and updates lastUpdated timestamp.
- **Latest key lookup**: Retrieves most recent notification by key for duplicate detection.
- MainViewModel loads notifications on UI thread after receiving broadcast.

```mermaid
sequenceDiagram
participant NL as "NotificationListener"
participant Repo as "NotificationRepository"
participant DAO as "NotificationDao"
participant DB as "NotificationDatabase"
participant MA as "MainActivity"
participant VM as "MainViewModel"
NL->>Repo : "latestForKey(key)"
Repo->>DAO : "SELECT latest by key"
DAO-->>Repo : "Latest notification or null"
alt Duplicate found
NL->>Repo : "bumpUpdateCount(id, timestamp)"
Repo->>DAO : "UPDATE SET updateCount + 1"
else New notification
NL->>Repo : "insert(NotificationItem)"
Repo->>DAO : "INSERT"
DAO->>DB : "persist"
end
NL->>MA : "Broadcast 'NOTIFICATIONS_UPDATED'"
MA->>VM : "observe and reload"
VM->>Repo : "loadNotifications(sorted)"
Repo->>DAO : "queries"
DAO-->>Repo : "List<NotificationItem>"
Repo-->>VM : "List<NotificationItem>"
VM-->>MA : "LiveData updates"
```

**Diagram sources**
- [NotificationRepository.kt:22-30](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L22-L30)
- [NotificationDao.kt:41-49](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L41-L49)
- [MainViewModel.kt:59-77](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L59-L77)

**Section sources**
- [NotificationRepository.kt:22-30](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L22-L30)
- [NotificationDao.kt:41-49](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L41-L49)
- [MainViewModel.kt:59-77](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L59-L77)

### Broadcast Communication with MainActivity
- NotificationListener sends two broadcasts:
  - "NOTIFICATIONS_UPDATED" with last_captured timestamp extra.
  - "LISTENER_DISCONNECTED" when access is revoked.
- MainActivity registers receivers in onResume and unregisters in onPause.
- Receivers trigger UI updates and dialogs.

```mermaid
sequenceDiagram
participant NL as "NotificationListener"
participant MA as "MainActivity"
participant VM as "MainViewModel"
NL->>MA : "Broadcast 'NOTIFICATIONS_UPDATED' with last_captured"
MA->>VM : "loadNotifications(sorted)"
VM-->>MA : "LiveData updates"
MA-->>MA : "Update UI and last captured time"
NL->>MA : "Broadcast 'LISTENER_DISCONNECTED'"
MA->>MA : "Show dialog to re-enable access"
```

**Diagram sources**
- [NotificationListener.kt:62-65](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L62-L65)
- [NotificationListener.kt:142-144](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L142-L144)
- [MainActivity.kt:51-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L51-L66)

**Section sources**
- [NotificationListener.kt:62-65](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L62-L65)
- [NotificationListener.kt:142-144](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L142-L144)
- [MainActivity.kt:51-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L51-L66)

### Permission Requirements and Consent Flow
- Notification Access Setting: MainActivity checks Settings.Secure for enabled listeners and prompts the user to enable it if missing.
- Android 13+ Permission: POST_NOTIFICATIONS is declared in manifest and requested at runtime.
- Consent flow:
  - On startup, MainActivity checks service status and permission.
  - If service is disabled, a dialog opens Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS.
  - If permission is missing on Android 13+, requests POST_NOTIFICATIONS.
  - MainActivity maintains a persistent notification when enabled by user preference.

```mermaid
flowchart TD
Start(["App Startup"]) --> CheckService["Check enabled_notification_listeners"]
CheckService --> ServiceEnabled{"Service enabled?"}
ServiceEnabled --> |No| PromptService["Show dialog to enable service"]
ServiceEnabled --> |Yes| CheckPerm["Check POST_NOTIFICATIONS (Android 13+)"]
PromptService --> OpenSettings["Open notification listener settings"]
CheckPerm --> PermGranted{"Permission granted?"}
PermGranted --> |No| RequestPerm["Request POST_NOTIFICATIONS"]
PermGranted --> |Yes| Continue["Continue"]
RequestPerm --> Continue
```

**Diagram sources**
- [MainActivity.kt:345-362](file://app/src/main/java/com/notificationlog/MainActivity.kt#L345-L362)
- [MainActivity.kt:364-373](file://app/src/main/java/com/notificationlog/MainActivity.kt#L364-L373)
- [AndroidManifest.xml:7](file://app/src/main/AndroidManifest.xml#L7)

**Section sources**
- [MainActivity.kt:313-322](file://app/src/main/java/com/notificationlog/MainActivity.kt#L313-L322)
- [MainActivity.kt:345-362](file://app/src/main/java/com/notificationlog/MainActivity.kt#L345-L362)
- [MainActivity.kt:364-373](file://app/src/main/java/com/notificationlog/MainActivity.kt#L364-L373)
- [AndroidManifest.xml:7](file://app/src/main/AndroidManifest.xml#L7)

### Android Notification System Integration
- Service declaration in AndroidManifest with BIND_NOTIFICATION_LISTENER_SERVICE permission and meta-data pointing to notification_service.xml.
- notification_service.xml defines the service summary shown in system settings.
- MainActivity creates channels for service status and warnings.

```mermaid
graph TB
AM["AndroidManifest.xml<br/>Service declaration"] --> NL["NotificationListener"]
NS["notification_service.xml<br/>Service summary"] --> AM
MA["MainActivity<br/>Create channels"] --> AM
```

**Diagram sources**
- [AndroidManifest.xml:29-43](file://app/src/main/AndroidManifest.xml#L29-L43)
- [notification_service.xml:1-5](file://app/src/main/res/xml/notification_service.xml#L1-L5)
- [MainActivity.kt:630-643](file://app/src/main/java/com/notificationlog/MainActivity.kt#L630-L643)

**Section sources**
- [AndroidManifest.xml:29-43](file://app/src/main/AndroidManifest.xml#L29-L43)
- [notification_service.xml:1-5](file://app/src/main/res/xml/notification_service.xml#L1-L5)
- [MainActivity.kt:630-643](file://app/src/main/java/com/notificationlog/MainActivity.kt#L630-L643)

## Dependency Analysis
- **Enhanced NotificationListener** depends on:
  - Room database initialized lazily.
  - NotificationRepository for persistence with duplicate prevention.
  - Android NotificationManager for warning notifications.
  - SharedPreferences for capture flag.
  - **Mutex for thread-safe operations**.
- MainActivity depends on:
  - MainViewModel for state and queries.
  - NotificationAdapter for UI rendering.
  - Settings for service status checks.
- MainViewModel depends on:
  - NotificationApp.repository for DAO operations.
  - Settings for service status checks.
- **Enhanced NotificationRepository** depends on:
  - NotificationDatabase for DAO access.
  - **New methods for duplicate handling and data compaction**.

```mermaid
graph LR
NL["NotificationListener<br/>with mutex sync"] --> REPO["NotificationRepository<br/>with duplicate prevention"]
REPO --> DAO["NotificationDao<br/>with bumpUpdateCount"]
DAO --> DB["NotificationDatabase<br/>v2 schema"]
MA["MainActivity"] --> VM["MainViewModel"]
VM --> REPO
MA --> AD["NotificationAdapter"]
MA --> NL
```

**Diagram sources**
- [NotificationListener.kt:27-37](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L27-L37)
- [NotificationRepository.kt:6-92](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L92)
- [NotificationDao.kt:6-68](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L68)
- [NotificationDatabase.kt:7-43](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L43)
- [MainActivity.kt:96](file://app/src/main/java/com/notificationlog/MainActivity.kt#L96)
- [MainViewModel.kt:62](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L62)
- [NotificationAdapter.kt:13-59](file://app/src/main/java/com/notificationlog/adapter/NotificationAdapter.kt#L13-L59)

**Section sources**
- [NotificationListener.kt:27-37](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L27-L37)
- [NotificationRepository.kt:6-92](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L92)
- [NotificationDao.kt:6-68](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L68)
- [NotificationDatabase.kt:7-43](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L43)
- [MainActivity.kt:96](file://app/src/main/java/com/notificationlog/MainActivity.kt#L96)
- [MainViewModel.kt:62](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L62)
- [NotificationAdapter.kt:13-59](file://app/src/main/java/com/notificationlog/adapter/NotificationAdapter.kt#L13-L59)

## Performance Considerations
- **Enhanced thread safety**: Mutex-based synchronization prevents race conditions during concurrent notification processing.
- Asynchronous processing: Repository insertions occur on Dispatchers.IO within a SupervisorJob-scoped CoroutineScope to prevent cancellation propagation and reduce overhead.
- **Improved duplicate handling**: Instead of creating duplicate rows, the system now bumps updateCount for identical notifications, reducing database size and improving query performance.
- UI responsiveness: MainViewModel loads notifications on IO and posts results to Main thread via withContext.
- Memory management: RecyclerView uses DiffUtil to minimize UI churn; adapter binds efficiently with data binding.
- Battery optimization:
  - Avoid unnecessary wake-ups; rely on system callbacks.
  - Use minimal logging and only when needed.
  - Cancel ongoing persistent notification when capture is disabled or not requested by user.
- Storage efficiency: Room uses REPLACE on conflict to avoid duplicates; consider pruning older entries if the dataset grows large.
- **New compactDuplicates method**: Provides efficient database cleanup for historical duplicate data.

## Troubleshooting Guide
Common issues and resolutions:
- Permission revocation:
  - Symptoms: Service stops capturing; a high-priority warning notification appears; MainActivity shows a dialog.
  - Resolution: Re-enable notification access in system settings; the service attempts rebind on Android 13+.
- Service disconnection:
  - Symptoms: LISTENER_DISCONNECTED broadcast received; UI indicates access lost.
  - Resolution: Confirm service is enabled; restart the app if necessary.
- Notification filtering:
  - Symptom: Notifications not captured despite service running.
  - Cause: capture_enabled flag is false.
  - Resolution: Toggle capture on/off via UI; verify SharedPreferences state.
- Database errors:
  - Symptoms: Exceptions during insert or query.
  - Resolution: Verify Room initialization and DAO operations; ensure database is built before use.
- UI not updating:
  - Symptoms: New notifications not reflected.
  - Resolution: Ensure broadcast receivers are registered in onResume and unregistered in onPause; verify MainViewModel loads notifications after broadcast.
- **Duplicate handling issues**:
  - Symptom: Multiple entries for same notification appearing.
  - Cause: Race conditions during concurrent processing or old data before v2 migration.
  - Resolution: Use the compactDuplicates feature to clean up historical data; ensure mutex synchronization is working properly.

**Section sources**
- [NotificationListener.kt:46-77](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L46-L77)
- [NotificationListener.kt:121-190](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L121-L190)
- [MainActivity.kt:51-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L51-L66)
- [MainActivity.kt:313-322](file://app/src/main/java/com/notificationlog/MainActivity.kt#L313-L322)

## Conclusion
The real-time notification capture feature integrates Android's NotificationListenerService with Room persistence and a reactive UI. The enhanced implementation provides robust background operation with mutex-based synchronization, user-controlled capture toggling, duplicate prevention mechanisms, and clear feedback mechanisms for permission and service status. By leveraging coroutines, broadcasts, and improved data integrity through updateCount tracking, it balances responsiveness and reliability while offering straightforward troubleshooting pathways and efficient duplicate handling.