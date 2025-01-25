package com.dqs.clock

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {
    companion object {
        private const val CHANNEL_ID = "ClockAppNotifications"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }
    
    private var notificationId = 1
    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createNotificationChannel()
        requestNotificationPermission()
        
        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setMediaPlaybackRequiresUserGesture(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                }
            }
            
            addJavascriptInterface(WebAppInterface(), "Android")
        }
        
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Clock App Notifications"
            val descriptionText = "Notifications for Timer, Alarm, and Stopwatch"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun showNotification(title: String, message: String) {
            handler.post {
                val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(longArrayOf(0, 500, 250, 500))

                NotificationManagerCompat.from(this@MainActivity).apply {
                    try {
                        notify(notificationId++, builder.build())
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        @JavascriptInterface
        fun getCurrentTime(): String {
            return System.currentTimeMillis().toString()
        }
    }

    override fun onBackPressed() {
        val webView = findViewById<WebView>(android.R.id.content).getChildAt(0) as WebView
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}