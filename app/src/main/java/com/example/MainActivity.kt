package com.example

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.ui.BatteryViewModel
import com.example.ui.BatteryViewModelFactory
import com.example.ui.components.BatteryMonitorScreen
import com.example.ui.theme.MyApplicationTheme
import android.content.IntentFilter
import com.example.receiver.BatteryUpdateReceiver

class MainActivity : ComponentActivity() {

    private lateinit var batteryUpdateReceiver: BatteryUpdateReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        batteryUpdateReceiver = BatteryUpdateReceiver()
        registerReceiver(batteryUpdateReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val app = application as BatteryMonitorApplication
        val viewModel: BatteryViewModel by viewModels {
            BatteryViewModelFactory(app.repository, app.estimator)
        }
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(Unit) {
                        checkUsageStatsPermission()
                    }
                    BatteryMonitorScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkUsageStatsPermission() {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryUpdateReceiver)
    }
}
