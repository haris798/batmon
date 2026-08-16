package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.BatteryMonitorApplication
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PowerConnectionReceiver : BroadcastReceiver() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_POWER_CONNECTED || action == Intent.ACTION_POWER_DISCONNECTED) {
            val app = context.applicationContext as BatteryMonitorApplication
            val pendingResult = goAsync()
            GlobalScope.launch {
                try {
                    app.repository.recordSample()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
