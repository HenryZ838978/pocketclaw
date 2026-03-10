package com.crabagent.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.crabagent.app.cloud.CloudClient
import com.crabagent.app.service.CrabForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var cloudStatus: TextView
    private val cloudClient = CloudClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        cloudStatus = findViewById(R.id.cloud_status)
        val btnPermission = findViewById<Button>(R.id.btn_notification_permission)
        val btnStart = findViewById<Button>(R.id.btn_start_service)

        btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnStart.setOnClickListener {
            startForegroundService(Intent(this, CrabForegroundService::class.java))
            updateStatus()
        }

        updateStatus()
        checkCloudConnection()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = isNotificationListenerEnabled()
        statusText.text = if (enabled) {
            "🦀 Notification access: GRANTED\nYour butler is listening."
        } else {
            "⚠️ Notification access: NOT GRANTED\nTap the button below to enable."
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, "com.crabagent.app.service.CrabNotificationListener")
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }

    private fun checkCloudConnection() {
        CoroutineScope(Dispatchers.Main).launch {
            val connected = withContext(Dispatchers.IO) { cloudClient.healthCheck() }
            cloudStatus.text = if (connected) {
                "☁️ Cloud Brain: CONNECTED"
            } else {
                "☁️ Cloud Brain: OFFLINE (start server first)"
            }
        }
    }
}
