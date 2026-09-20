package com.monnhuitne.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.monnhuitne.app.push.subscribeToFailureTopic
import com.monnhuitne.app.ui.MonNhuitNeApp
import com.monnhuitne.app.ui.theme.MonNhuitNeTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* pas d'action si refusé */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        subscribeToFailureTopic(this)

        setContent {
            MonNhuitNeTheme {
                MonNhuitNeApp()
            }
        }
    }
}
