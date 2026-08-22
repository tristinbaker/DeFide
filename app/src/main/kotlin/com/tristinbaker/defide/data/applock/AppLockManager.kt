package com.tristinbaker.defide.data.applock

import android.os.SystemClock
import com.tristinbaker.defide.data.preferences.AppLockTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor() {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    /** Kept in sync with the preference by MainActivity's composition. */
    var timeout: AppLockTimeout = AppLockTimeout.IMMEDIATELY

    private var backgroundedAt: Long = 0L

    fun onAppBackgrounded() {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onAppForegrounded() {
        if (_isLocked.value || backgroundedAt == 0L) return
        if (SystemClock.elapsedRealtime() - backgroundedAt >= timeout.millis) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
    }
}
