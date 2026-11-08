# Service API

<cite>
**Referenced Files in This Document**
- [NotificationListener.kt](file://app/src/main/java/com/notificationlog/NotificationListener.kt)
- [ListenerConnection.kt](file://app/src/main/java/com/notificationlog/ListenerConnection.kt)
- [AndroidManifest.xml](file://app/src/main/AndroidManifest.xml)
- [MainActivity.kt](file://app/src/main/java/com/notificationlog/MainActivity.kt)
- [MainViewModel.kt](file://app/src/main/java/com/notificationlog/MainViewModel.kt)
- [NotificationApp.kt](file://app/src/main/java/com/notificationlog/NotificationApp.kt)
- [NotificationRepository.kt](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt)
- [NotificationDatabase.kt](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt)
- [NotificationDao.kt](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt)
- [NotificationItem.kt](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt)
- [notification_service.xml](file://app/src/main/res/xml/notification_service.xml)
</cite>

## Update Summary
**Changes Made**
- Added comprehensive documentation for the new ListenerConnection state management system
- Updated service binding section to reflect enhanced connection state tracking
- Enhanced error handling documentation for OEM-specific issues (MIUI/HyperOS AutoStartManagerService)
- Improved user feedback mechanisms for connection states
- Updated architecture diagrams to include the new ListenerConnection component
- Enhanced troubleshooting guide with OEM-specific scenarios

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
This document provides comprehensive API documentation for the NotificationListener service and related background operation interfaces. It covers the service lifecycle, notification capture mechanisms, broadcast communication protocols, service registration in AndroidManifest.xml, intent filters, permissions, notification processing pipeline, data transformation methods, background execution constraints, and practical examples for binding, event handling, and state management. It also addresses service restart scenarios, memory management, and battery optimization considerations.

**Updated** Enhanced with new ListenerConnection state management system that provides accurate connection status tracking and improved error handling for OEM-specific issues like MIUI/HyperOS AutoStartManagerService behavior.

## Project Structure
The project follows a modular Android architecture with a foreground-aware NotificationListener service, a Room database for persistence, and a UI layer built with Activities and ViewModels. The service integrates with the Android notification listener framework and communicates with the UI via local broadcasts. A new ListenerConnection component manages connection state across the application lifecycle.

```mermaid
graph TB
subgraph "Android App"
Manifest["AndroidManifest.xml"]
App["NotificationApp (Application)"]
Activity["MainActivity (UI)"]
VM["MainViewModel"]
Service["NotificationListener (Service)"]
Conn["ListenerConnection (State Manager)"]
end
subgraph "Data Layer"
Repo["NotificationRepository"]
DB["NotificationDatabase (Room)"]
DAO["NotificationDao"]
Entity["NotificationItem (Entity)"]
end
Manifest --> Service
App --> Repo
Repo --> DB
DB --> DAO
DAO --> Entity
Activity --> VM
VM --> Repo
Service --> Repo
Service --> Conn
Conn --> VM
Service --> Activity
VM --> Activity
```

**Diagram sources**
- [AndroidManifest.xml:29-43](file://app/src/main/AndroidManifest.xml#L29-L43)
- [NotificationApp.kt:8-27](file://app/src/main/java/com/notificationlog/NotificationApp.kt#L8-L27)
- [NotificationRepository.kt:6-27](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L27)
- [NotificationDatabase.kt:7-10](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L10)
- [NotificationDao.kt:6-26](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L26)
- [NotificationItem.kt:8-19](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L8-L19)
- [MainActivity.kt:43-108](file://app/src/main/java/com/notificationlog/MainActivity.kt#L43-L108)
- [MainViewModel.kt:15-48](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L15-L48)
- [NotificationListener.kt:23-173](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L173)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)

**Section sources**
- [AndroidManifest.xml:1-45](file://app/src/main/AndroidManifest.xml#L1-L45)
- [NotificationApp.kt:1-28](file://app/src/main/java/com/notificationlog/NotificationApp.kt#L1-L28)
- [NotificationRepository.kt:1-28](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L1-L28)
- [NotificationDatabase.kt:1-10](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L1-L10)
- [NotificationDao.kt:1-26](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L1-L26)
- [NotificationItem.kt:1-19](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L1-L19)
- [MainActivity.kt:1-1076](file://app/src/main/java/com/notificationlog/MainActivity.kt#L1-L1076)
- [MainViewModel.kt:1-285](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L1-L285)
- [NotificationListener.kt:1-211](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L1-L211)
- [ListenerConnection.kt:1-32](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L1-L32)

## Core Components
- **NotificationListener**: Android NotificationListenerService subclass responsible for capturing notifications, persisting them, and broadcasting updates to the UI.
- **ListenerConnection**: Singleton state manager that tracks actual service binding status, providing LiveData for connection state and handling thread-safe state updates.
- **MainActivity**: UI controller that registers broadcast receivers, manages permissions, toggles capture state, and displays captured notifications.
- **MainViewModel**: AndroidViewModel that observes service status, loads notifications from the repository, and exposes LiveData to the UI.
- **NotificationRepository**: Repository layer coordinating Room DAO operations for CRUD on NotificationItem entities.
- **NotificationDatabase and NotificationDao**: Room database and DAO for persistent storage of notifications.
- **NotificationItem**: Parcelable Room entity representing a captured notification.
- **NotificationApp**: Application class initializing the Room database and exposing a global repository instance.

Key responsibilities:
- **Service lifecycle**: onListenerConnected, onListenerDisconnected, onNotificationPosted, onNotificationRemoved, onDestroy.
- **Connection state management**: Real-time tracking of actual service binding status vs. permission settings.
- **Broadcast protocol**: NOTIFICATIONS_UPDATED and LISTENER_DISCONNECTED for inter-process communication.
- **Persistence**: Room database integration for storing notifications.
- **OEM-specific error handling**: Special handling for MIUI/HyperOS AutoStartManagerService behavior.
- **UI integration**: Local broadcasts to refresh UI and show warnings when access is revoked.

**Updated** Added ListenerConnection as a core component for managing service binding state and improved error handling for OEM-specific scenarios.

**Section sources**
- [NotificationListener.kt:23-211](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L211)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [MainActivity.kt:43-144](file://app/src/main/java/com/notificationlog/MainActivity.kt#L43-L144)
- [MainViewModel.kt:15-115](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L15-L115)
- [NotificationRepository.kt:6-27](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L27)
- [NotificationDatabase.kt:7-10](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L10)
- [NotificationDao.kt:6-26](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L26)
- [NotificationItem.kt:8-19](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L8-L19)
- [NotificationApp.kt:8-27](file://app/src/main/java/com/notificationlog/NotificationApp.kt#L8-L27)

## Architecture Overview
The system uses Android's NotificationListenerService to capture notifications in real-time. The service transforms StatusBarNotification into NotificationItem entities, persists them via Room, and broadcasts updates to MainActivity. A new ListenerConnection component provides accurate connection state tracking, distinguishing between permission granted and actual service binding. MainActivity registers receivers to receive these broadcasts and refreshes its UI accordingly. The ViewModel mediates between UI and repository, providing LiveData streams for reactive updates.

```mermaid
sequenceDiagram
participant OS as "Android OS"
participant Service as "NotificationListener"
participant Conn as "ListenerConnection"
participant Repo as "NotificationRepository"
participant DB as "Room Database"
participant DAO as "NotificationDao"
participant Item as "NotificationItem"
participant UI as "MainActivity"
participant VM as "MainViewModel"
OS->>Service : "onNotificationPosted(StatusBarNotification)"
Service->>Service : "Extract title, content, timestamp"
Service->>Repo : "insert(NotificationItem)"
Repo->>DAO : "insert(item)"
DAO->>DB : "persist item"
Service->>UI : "sendBroadcast(NOTIFICATIONS_UPDATED)"
UI->>VM : "receive broadcast"
VM->>Repo : "loadNotifications()"
Repo->>DAO : "getNotificationsSortedByDate/App"
DAO-->>Repo : "List<NotificationItem>"
Repo-->>VM : "List<NotificationItem>"
VM-->>UI : "LiveData updates"
UI-->>UI : "Refresh RecyclerView"
Note over Conn : "Tracks actual bind state<br/>vs permission setting"
```

**Diagram sources**
- [NotificationListener.kt:109-153](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L109-L153)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [NotificationRepository.kt:16-18](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L16-L18)
- [NotificationDao.kt:18-19](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L18-L19)
- [MainActivity.kt:57-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L57-L66)
- [MainViewModel.kt:59-77](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L59-L77)

## Detailed Component Analysis

### NotificationListener Service
The NotificationListener service extends NotificationListenerService and implements:
- Lifecycle callbacks: onListenerConnected, onListenerDisconnected, onNotificationPosted, onNotificationRemoved, onDestroy.
- Notification capture: Extracts title, content, timestamp, and app name; checks captureEnabled preference; persists via repository; broadcasts updates.
- Connection state management: Updates ListenerConnection state on connect/disconnect events.
- Reconnection handling: Requests rebind on API 33+ and shows a high-priority warning notification when access is lost.
- Broadcast communication: Emits NOTIFICATIONS_UPDATED and LISTENER_DISCONNECTED with package-scoped intents.
- OEM-specific error handling: Enhanced exception handling for rebind requests and notification posting.

Implementation highlights:
- Background execution: Uses CoroutineScope with Dispatchers.IO and SupervisorJob for non-cancellable background tasks.
- Persistence: Initializes Room database and repository lazily; inserts NotificationItem asynchronously.
- Safety: Catches exceptions during insertion and logs errors; cancels a warning notification on successful connection.
- Restart scenario: Calls requestRebind on API 33+ to recover from disconnection.
- Thread safety: Uses postValue for thread-safe state updates in ListenerConnection.

```mermaid
classDiagram
class NotificationListener {
+onListenerConnected()
+onListenerDisconnected()
+onNotificationPosted(sbn)
+onNotificationRemoved(sbn)
+onDestroy()
-showAccessRevokedNotification()
-cancelNotificationByKey(key)
-job : SupervisorJob
-scope : CoroutineScope
-repository : NotificationRepository
}
class ListenerConnection {
+connected : LiveData~Boolean~
+isConnected : Boolean
+setConnected(connected : Boolean)
-_connected : MutableLiveData~Boolean~
}
class NotificationRepository {
+getAllNotifications()
+getNotificationsSortedByDate()
+getNotificationsSortedByApp()
+insert(notification)
+delete(notification)
+deleteAll()
}
class NotificationDatabase {
+notificationDao() : NotificationDao
}
class NotificationDao {
+getAll() : NotificationItem[]
+getNotificationsSortedByDate() : NotificationItem[]
+getNotificationsSortedByApp() : NotificationItem[]
+insert(notification)
+delete(notification)
+deleteAll()
}
class NotificationItem {
+id : Long
+packageName : String
+appName : String
+title : String
+content : String
+timestamp : Long
+key : String?
}
NotificationListener --> NotificationRepository : "uses"
NotificationListener --> ListenerConnection : "updates state"
NotificationRepository --> NotificationDatabase : "wraps"
NotificationDatabase --> NotificationDao : "exposes"
NotificationDao --> NotificationItem : "persists"
```

**Diagram sources**
- [NotificationListener.kt:23-211](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L211)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [NotificationRepository.kt:6-27](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L27)
- [NotificationDatabase.kt:7-10](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L10)
- [NotificationDao.kt:6-26](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L26)
- [NotificationItem.kt:8-19](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L8-L19)

**Section sources**
- [NotificationListener.kt:23-211](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L211)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)

### ListenerConnection State Management
The ListenerConnection component provides centralized state management for service binding status:
- **Thread-safe state updates**: Uses MutableLiveData with postValue for safe cross-thread communication.
- **Real-time connection tracking**: Observes actual service binding rather than just permission settings.
- **OEM-specific handling**: Addresses MIUI/HyperOS AutoStartManagerService behavior where permissions are granted but binds are refused.
- **LiveData integration**: Exposes connection state as LiveData for reactive UI updates.

Key features:
- Singleton pattern for global state access.
- Automatic state synchronization between service and UI components.
- Prevention of false positive "capture running" states when service is unbound.

**Updated** New component that solves the critical issue where apps could claim to be capturing while actually being deaf due to OEM-specific binding refusals.

**Section sources**
- [ListenerConnection.kt:1-32](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L1-L32)

### Enhanced Service Binding and Error Handling
The service binding has been significantly enhanced with improved error handling and user feedback:

**Connection State Tracking:**
- Distinguishes between three states: NOT_GRANTED, NOT_CONNECTED, CONNECTED
- Tracks actual service binding status vs. permission settings
- Provides immediate feedback when connection state changes

**OEM-Specific Error Handling:**
- Handles MIUI/HyperOS AutoStartManagerService behavior
- Detects when permissions are granted but binds are refused
- Shows appropriate user feedback for different failure scenarios

**Improved User Feedback:**
- Clickable service status indicator for reconnection attempts
- Toast messages for connection success/failure
- Dialog prompts for manual intervention when needed
- Persistent notification warnings when access is lost

```mermaid
flowchart TD
A["Permission Granted"] --> B{"Actual Bind Success?"}
B -- "Yes" --> C["CONNECTED State"]
B -- "No" --> D["NOT_CONNECTED State"]
D --> E["Auto-rebind Attempt"]
E --> F{"Rebind Success?"}
F -- "Yes" --> C
F -- "No" --> G["Show User Feedback"]
G --> H["Manual Intervention Required"]
C --> I["Normal Operation"]
```

**Diagram sources**
- [MainViewModel.kt:82-99](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L82-L99)
- [MainActivity.kt:442-468](file://app/src/main/java/com/notificationlog/MainActivity.kt#L442-L468)

**Section sources**
- [MainViewModel.kt:82-122](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L82-L122)
- [MainActivity.kt:442-501](file://app/src/main/java/com/notificationlog/MainActivity.kt#L442-L501)

### MainActivity and Broadcast Communication
MainActivity registers two local broadcast receivers:
- NOTIFICATIONS_UPDATED: Refreshes the notification list and updates the last captured timestamp label.
- LISTENER_DISCONNECTED: Shows a dialog prompting the user to re-enable notification access.

Lifecycle integration:
- Registers receivers in onResume with RECEIVER_NOT_EXPORTED on API 33+ and standard registration otherwise.
- Unregisters receivers in onPause to prevent leaks.
- Observes service status LiveData from MainViewModel to update UI state and buttons.
- Implements auto-rebind functionality for NOT_CONNECTED state.

Enhanced features:
- Clickable service status text for manual reconnection attempts.
- Improved error messaging and user guidance.
- Better state management with proper cleanup.

```mermaid
sequenceDiagram
participant Service as "NotificationListener"
participant Conn as "ListenerConnection"
participant UI as "MainActivity"
participant VM as "MainViewModel"
Service->>Conn : "setConnected(true/false)"
Conn->>VM : "LiveData observer triggered"
VM->>VM : "refreshCaptureState()"
VM->>UI : "captureState LiveData updated"
UI->>UI : "applyCaptureState(state)"
Service->>UI : "sendBroadcast(NOTIFICATIONS_UPDATED)"
UI->>UI : "notificationsUpdatedReceiver.onReceive()"
UI->>VM : "viewModel.loadNotifications(sort)"
VM-->>UI : "LiveData notifications updated"
UI-->>UI : "Adapter submitList(...)"
Service->>UI : "sendBroadcast(LISTENER_DISCONNECTED)"
UI->>UI : "listenerDisconnectedReceiver.onReceive()"
UI-->>UI : "Show dialog to re-enable access"
```

**Diagram sources**
- [MainActivity.kt:51-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L51-L66)
- [MainActivity.kt:110-144](file://app/src/main/java/com/notificationlog/MainActivity.kt#L110-L144)
- [NotificationListener.kt:46-77](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L46-L77)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [MainViewModel.kt:65-76](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L65-L76)

**Section sources**
- [MainActivity.kt:43-144](file://app/src/main/java/com/notificationlog/MainActivity.kt#L43-L144)
- [NotificationListener.kt:46-77](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L46-L77)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [MainViewModel.kt:65-76](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L65-L76)

### Service Registration and Permissions
Service registration in AndroidManifest.xml:
- Declares the NotificationListener service with BIND permission and action filter.
- Includes meta-data pointing to notification_service.xml resource.
- Exports the service and enables it.

Permissions:
- INTERNET and ACCESS_NETWORK_STATE for network-related operations.
- POST_NOTIFICATIONS for Android 13+ to post notifications.

Intent filters and meta-data:
- Action android.service.notification.NotificationListenerService.
- Meta-data resource for notification listener summary.

```mermaid
flowchart TD
Start(["Manifest Parsing"]) --> ServiceDecl["Service Declaration<br/>android:name='.NotificationListener'"]
ServiceDecl --> BindPerm["android:permission='BIND_NOTIFICATION_LISTENER_SERVICE'"]
ServiceDecl --> ActionFilter["Intent Filter<br/>Action: NotificationListenerService"]
ServiceDecl --> MetaData["Meta-data<br/>android.service.notification.NotificationListenerService"]
ServiceDecl --> Exported["android:exported='true'"]
ServiceDecl --> Enabled["android:enabled='true'"]
Start --> Perm["Uses Permissions<br/>INTERNET, ACCESS_NETWORK_STATE,<br/>POST_NOTIFICATIONS"]
Perm --> End(["Ready"])
Exported --> End
```

**Diagram sources**
- [AndroidManifest.xml:29-43](file://app/src/main/AndroidManifest.xml#L29-L43)
- [AndroidManifest.xml:5-7](file://app/src/main/AndroidManifest.xml#L5-L7)

**Section sources**
- [AndroidManifest.xml:1-45](file://app/src/main/AndroidManifest.xml#L1-L45)
- [notification_service.xml:1-5](file://app/src/main/res/xml/notification_service.xml#L1-L5)

### Notification Processing Pipeline and Data Transformation
Pipeline stages:
- Capture: onNotificationPosted extracts title, content, timestamp, and app name from StatusBarNotification extras.
- Preference gate: Reads capture_enabled from SharedPreferences; skips processing if disabled.
- Model creation: Constructs NotificationItem with extracted fields.
- Persistence: Inserts NotificationItem asynchronously via repository and DAO.
- Broadcast: Emits NOTIFICATIONS_UPDATED with last_captured timestamp extra.
- UI refresh: MainActivity receives broadcast and reloads notifications.

Data transformation:
- App name resolution via PackageManager.getApplicationLabel.
- Timestamp preservation from StatusBarNotification.postTime.
- Key preservation for potential future cancellation.

```mermaid
flowchart TD
A["StatusBarNotification"] --> B["Extract extras<br/>title, text, postTime"]
B --> C["Resolve app name via package manager"]
C --> D{"capture_enabled?"}
D -- "No" --> E["Log and ignore"]
D -- "Yes" --> F["Create NotificationItem"]
F --> G["Repository.insert(NotificationItem)"]
G --> H["DAO.insert(...)"]
H --> I["Broadcast NOTIFICATIONS_UPDATED<br/>with last_captured"]
I --> J["MainActivity receives and refreshes UI"]
```

**Diagram sources**
- [NotificationListener.kt:109-153](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L109-L153)
- [NotificationRepository.kt:16-18](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L16-L18)
- [NotificationDao.kt:18-19](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L18-L19)
- [MainActivity.kt:57-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L57-L66)

**Section sources**
- [NotificationListener.kt:109-153](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L109-L153)
- [NotificationRepository.kt:16-18](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L16-L18)
- [NotificationDao.kt:18-19](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L18-L19)
- [MainActivity.kt:57-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L57-L66)

### Enhanced Service Binding, Restart Scenarios, and State Management
**Enhanced Service Binding:**
- Uses ListenerConnection for accurate state tracking instead of relying solely on Settings.Secure.getString.
- Three-state model: NOT_GRANTED, NOT_CONNECTED, CONNECTED.
- Automatic rebind attempts for OEM-specific scenarios.

**Restart Scenarios:**
- onListenerDisconnected triggers rebind request on API 33+ and shows a high-priority warning notification.
- Enhanced error handling for rebind failures with detailed logging.
- The UI listens for LISTENER_DISCONNECTED and prompts the user to re-enable access.

**Advanced State Management:**
- SharedPreferences stores capture_enabled, dark_mode, show_running_notif.
- MainViewModel exposes LiveData for service status and notification count.
- MainActivity updates UI state based on service status and preferences.
- Thread-safe state updates using postValue for cross-thread communication.

```mermaid
sequenceDiagram
participant OS as "Android OS"
participant Service as "NotificationListener"
participant Conn as "ListenerConnection"
participant UI as "MainActivity"
participant VM as "MainViewModel"
OS->>Service : "onListenerDisconnected()"
Service->>Conn : "setConnected(false)"
Service->>Service : "requestRebind() on API 33+"
Service->>UI : "sendBroadcast(LISTENER_DISCONNECTED)"
UI->>UI : "listenerDisconnectedReceiver.onReceive()"
UI->>UI : "Show dialog to re-enable access"
UI->>VM : "checkNotificationServiceStatus()"
VM->>VM : "refreshCaptureState()"
VM-->>UI : "captureState LiveData updated"
UI-->>UI : "Update button states and status text"
```

**Diagram sources**
- [NotificationListener.kt:46-77](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L46-L77)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [MainActivity.kt:51-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L51-L66)
- [MainViewModel.kt:82-99](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L82-L99)

**Section sources**
- [NotificationListener.kt:46-77](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L46-L77)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [MainActivity.kt:51-66](file://app/src/main/java/com/notificationlog/MainActivity.kt#L51-L66)
- [MainViewModel.kt:82-99](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L82-L99)

## Dependency Analysis
The service depends on:
- Room database for persistence.
- Repository for DAO abstraction.
- SharedPreferences for runtime configuration.
- Notification channels for user feedback.
- **ListenerConnection for state management**.

MainActivity depends on:
- MainViewModel for reactive state.
- SharedPreferences for user preferences.
- Notification channels for persistent status notification.
- **ListenerConnection for connection state observation**.

```mermaid
graph LR
Service["NotificationListener"] --> Repo["NotificationRepository"]
Service --> Conn["ListenerConnection"]
Repo --> DB["NotificationDatabase"]
DB --> DAO["NotificationDao"]
DAO --> Item["NotificationItem"]
UI["MainActivity"] --> VM["MainViewModel"]
VM --> Repo
VM --> Conn
Service --> UI
UI --> Channels["Notification Channels"]
```

**Diagram sources**
- [NotificationListener.kt:23-34](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L34)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [NotificationRepository.kt:6-27](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L27)
- [NotificationDatabase.kt:7-10](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L10)
- [NotificationDao.kt:6-26](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L26)
- [NotificationItem.kt:8-19](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L8-L19)
- [MainActivity.kt:43-108](file://app/src/main/java/com/notificationlog/MainActivity.kt#L43-L108)
- [MainViewModel.kt:15-48](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L15-L48)

**Section sources**
- [NotificationListener.kt:23-34](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L23-L34)
- [ListenerConnection.kt:18-31](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L18-L31)
- [NotificationRepository.kt:6-27](file://app/src/main/java/com/notificationlog/repository/NotificationRepository.kt#L6-L27)
- [NotificationDatabase.kt:7-10](file://app/src/main/java/com/notificationlog/data/NotificationDatabase.kt#L7-L10)
- [NotificationDao.kt:6-26](file://app/src/main/java/com/notificationlog/data/NotificationDao.kt#L6-L26)
- [NotificationItem.kt:8-19](file://app/src/main/java/com/notificationlog/model/NotificationItem.kt#L8-L19)
- [MainActivity.kt:43-108](file://app/src/main/java/com/notificationlog/MainActivity.kt#L43-L108)
- [MainViewModel.kt:15-48](file://app/src/main/java/com/notificationlog/MainViewModel.kt#L15-L48)

## Performance Considerations
- Background threading: Uses Dispatchers.IO and SupervisorJob to avoid blocking the main thread during database writes.
- Asynchronous processing: Repository and DAO operations are suspend functions executed on IO dispatcher.
- Broadcast efficiency: Single broadcast per insertion to minimize UI refresh overhead.
- Memory management: Cancels background job in onDestroy to prevent leaks.
- Battery optimization: Avoids wake locks; relies on OS notification delivery. Persistent notification is optional and controlled by user preference.
- **Connection state efficiency**: ListenerConnection uses postValue for efficient cross-thread state updates without unnecessary recomputation.

**Updated** Added performance consideration for the new ListenerConnection component which efficiently manages state updates across threads.

## Troubleshooting Guide
Common issues and resolutions:
- **Notification access revoked**: The service shows a high-priority warning notification and broadcasts LISTENER_DISCONNECTED. MainActivity displays a dialog and opens system settings for re-enabling.
- **No notifications captured**: Verify capture_enabled preference and ensure notification access is granted. Check that the service is registered and exported in the manifest.
- **UI not updating**: Confirm receivers are registered in onResume and unregistered in onPause. Ensure broadcasts are sent with the correct action and package scope.
- **Database errors**: Repository catches exceptions during insert and logs errors; verify Room initialization and DAO queries.
- **OEM-specific binding issues**: If the app claims to be capturing but nothing is written, check for MIUI/HyperOS AutoStartManagerService interference. The app should automatically attempt rebind and show appropriate user feedback.
- **Connection state inconsistencies**: Use the three-state model (NOT_GRANTED, NOT_CONNECTED, CONNECTED) to diagnose issues. NOT_CONNECTED indicates permission granted but service unbound.

**Updated** Enhanced troubleshooting guide with OEM-specific scenarios and connection state diagnostics.

**Section sources**
- [NotificationListener.kt:46-77](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L46-L77)
- [NotificationListener.kt:146-148](file://app/src/main/java/com/notificationlog/NotificationListener.kt#L146-L148)
- [MainActivity.kt:645-661](file://app/src/main/java/com/notificationlog/MainActivity.kt#L645-L661)
- [MainActivity.kt:110-144](file://app/src/main/java/com/notificationlog/MainActivity.kt#L110-L144)
- [ListenerConnection.kt:6-17](file://app/src/main/java/com/notificationlog/ListenerConnection.kt#L6-L17)

## Conclusion
The NotificationListener service provides a robust foundation for capturing, persisting, and displaying Android notifications. Its integration with Room ensures reliable data storage, while local broadcasts enable responsive UI updates. The new ListenerConnection component significantly improves reliability by accurately tracking service binding status versus permission settings, addressing critical OEM-specific issues like MIUI/HyperOS AutoStartManagerService behavior. Proper service registration, permissions, and lifecycle management are essential for consistent behavior across Android versions. The enhanced architecture supports restart scenarios gracefully and offers clear pathways for diagnostics and user intervention, with improved error handling and user feedback mechanisms throughout the application.

**Updated** The addition of ListenerConnection state management and enhanced OEM-specific error handling makes the service more reliable and user-friendly, particularly on devices with aggressive background process management.