package com.notificationlog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import android.provider.Settings  // ✅ Correct
import com.notificationlog.model.NotificationItem
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _serviceStatus = MutableLiveData<Boolean>(false)
    val serviceStatus: LiveData<Boolean> = _serviceStatus
    
    private val _notificationCount = MutableLiveData<Int>(0)
    val notificationCount: LiveData<Int> = _notificationCount
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _notifications = MutableLiveData<List<NotificationItem>>(emptyList())
    val notifications: LiveData<List<NotificationItem>> = _notifications

    private val _compactResult = MutableLiveData<Int?>()
    val compactResult: LiveData<Int?> = _compactResult

    /** Rows removed by "delete all matches", so the UI can report the number. */
    private val _bulkDeleted = MutableLiveData<Int?>()
    val bulkDeleted: LiveData<Int?> = _bulkDeleted

    /** Remembered so refreshes after a delete keep the user's chosen order. */
    private var currentSort: SortType = SortType.DATE

    /**
     * Remembered for the same reason: a notification arriving while a search is open
     * used to reload the full list and silently drop the filter.
     */
    private var currentQuery: String = ""

    /** The filter currently applied, so the UI can rebuild its own state after rotation. */
    val filterQuery: String
        get() = currentQuery

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _errorMessage.postValue("Unexpected error: ${throwable.message}")
    }
    
    init {
        checkNotificationServiceStatus()
    }
    
    fun checkNotificationServiceStatus() {
        viewModelScope.launch(handler) {
            try {
                val isEnabled = withContext(Dispatchers.IO) {
                    isNotificationServiceEnabled()
                }
                _serviceStatus.value = isEnabled
            } catch (e: Exception) {
                _errorMessage.value = "Error checking service status: ${e.message}"
            }
        }
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        val context = getApplication<Application>()
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(context.packageName) == true
    }
    
    fun loadNotifications(sortType: SortType) {
        currentSort = sortType
        val query = currentQuery
        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                val repo = NotificationApp.repository
                val items = if (query.isEmpty()) {
                    when (sortType) {
                        SortType.DATE -> repo.getNotificationsSortedByDate()
                        SortType.APP -> repo.getNotificationsSortedByApp()
                    }
                } else {
                    val pattern = likePattern(query)
                    when (sortType) {
                        SortType.DATE -> repo.searchByDate(pattern)
                        SortType.APP -> repo.searchByApp(pattern)
                    }
                }
                withContext(Dispatchers.Main) {
                    _notifications.value = items
                    _notificationCount.value = items.size
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error loading notifications: ${e.message}"
                }
            }
        }
    }

    /** Applies a filter, or clears it when the query is blank. */
    fun setQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed == currentQuery) return
        currentQuery = trimmed
        loadNotifications(currentSort)
    }

    /**
     * Wraps the term for LIKE and escapes its wildcards, so "%" and "_" are searched
     * for literally instead of matching every row.
     */
    private fun likePattern(query: String): String {
        val escaped = query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }

    fun deleteNotification(item: NotificationItem) {
        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                NotificationApp.repository.delete(item)
                loadNotifications(currentSort) // Refresh list, keeping the current order
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting notification: ${e.message}"
                }
            }
        }
    }
    
    fun clearNotifications() {
        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                NotificationApp.repository.deleteAll()
                withContext(Dispatchers.Main) {
                    _notifications.value = emptyList()
                    _notificationCount.value = 0
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error clearing notifications: ${e.message}"
                }
            }
        }
    }
    
    fun setError(message: String?) {
        _errorMessage.value = message
    }

    /**
     * Collapses repeated identical postings into one row each. Mainly a repair action
     * for databases written before de-duplication existed; new traffic is merged live.
     */
    fun compactDuplicates() {
        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                val removed = NotificationApp.repository.compactDuplicates()
                withContext(Dispatchers.Main) {
                    _compactResult.value = removed
                    loadNotifications(currentSort)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error compacting log: ${e.message}"
                }
            }
        }
    }

    fun consumeCompactResult() {
        _compactResult.value = null
    }

    /**
     * Deletes every row matching the active filter. Uses the same LIKE pattern as the
     * search queries, so the rows removed are exactly those the list was showing.
     */
    fun deleteMatching(query: String) {
        if (query.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO + handler) {
            try {
                val removed = NotificationApp.repository.deleteMatching(likePattern(query))
                withContext(Dispatchers.Main) {
                    _bulkDeleted.value = removed
                    loadNotifications(currentSort)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting notifications: ${e.message}"
                }
            }
        }
    }

    fun consumeBulkDeleted() {
        _bulkDeleted.value = null
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

enum class SortType {
    DATE,
    APP
}
