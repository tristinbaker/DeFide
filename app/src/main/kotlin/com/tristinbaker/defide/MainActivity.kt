package com.tristinbaker.defide

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.tristinbaker.defide.data.applock.AppLockManager
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import com.tristinbaker.defide.ui.DeFideApp
import com.tristinbaker.defide.ui.applock.AppLockScreen
import com.tristinbaker.defide.ui.theme.DeFideTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// FragmentActivity (not ComponentActivity) is required by androidx.biometric's BiometricPrompt.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var prefsRepository: UserPreferencesRepository
    @Inject lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            )
        )
        setContent {
            val prefs by prefsRepository.preferences.collectAsState(initial = com.tristinbaker.defide.data.preferences.UserPreferences())
            if (prefs.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (prefs.fullScreenMode) {
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            LaunchedEffect(prefs.appLockTimeout) {
                appLockManager.timeout = prefs.appLockTimeout
            }
            val isLocked by appLockManager.isLocked.collectAsState()
            DeFideTheme(theme = prefs.theme, font = prefs.appFont) {
                if (prefs.appLockEnabled && isLocked) {
                    AppLockScreen(onUnlocked = { appLockManager.unlock() })
                } else {
                    DeFideApp()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLockManager.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) appLockManager.onAppBackgrounded()
    }
}
