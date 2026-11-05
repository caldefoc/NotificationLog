package com.notificationlog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Whether the system has actually bound our [NotificationListener].
 *
 * The `enabled_notification_listeners` setting only records that the user granted
 * access. It stays true even when the bind itself is refused — MIUI/HyperOS does
 * exactly that through AutoStartManagerService after an install, and the app then
 * claims to be capturing while nothing is written. Reporting the bind state instead
 * of the setting is what makes that failure visible.
 *
 * Written from the service (a background thread) and read on the main thread, hence
 * postValue rather than value.
 */
object ListenerConnection {

    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> = _connected

    val isConnected: Boolean
        get() = _connected.value == true

    fun setConnected(connected: Boolean) {
        if (_connected.value != connected) {
            _connected.postValue(connected)
        }
    }
}
