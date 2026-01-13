package com.notificationlog

import android.app.Application
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import android.provider.Settings  // ✅ Correct
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
    
    init {
        checkNotificationServiceStatus()
    }
    
    fun checkNotificationServiceStatus() {
        viewModelScope.launch {
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
    
    fun clearNotifications() {
        _notificationCount.value = 0
        // TODO: Clear from database when implemented
    }
    
    fun incrementNotificationCount() {
        _notificationCount.value = (_notificationCount.value ?: 0) + 1
    }
    
    fun setError(message: String?) {
        _errorMessage.value = message
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}