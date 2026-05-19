package com.elderescort.app

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.elderescort.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingArticleId: Int? = null
    private var isWebViewReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        webView = binding.webView
        setContentView(binding.root)

        createNotificationChannel()
        requestNotificationPermission()
        initWebView()
        TextDispatcher.callback = { text ->
            if (text.startsWith("[LOG]")) {
                sendStatus(text.removePrefix("[LOG]"))
            } else {
                sendTextToWeb(text)
            }
        }
        requestMediaProjection()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val articleId = intent?.getIntExtra("articleId", -1) ?: -1
        if (articleId > 0) {
            if (isWebViewReady) {
                navigateToArticle(articleId)
            } else {
                pendingArticleId = articleId
            }
        }
    }

    private fun initWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.addJavascriptInterface(
            AndroidBridge { warningMessage, articleId ->
                showFraudNotification(warningMessage, articleId)
            },
            "AndroidApp"
        )
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isWebViewReady = true
                sendStatus("WebView 已就绪")
                pendingArticleId?.let { navigateToArticle(it) }
                pendingArticleId = null
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                sendStatus("WebView 错误($errorCode): $description URL: $failingUrl")
            }
        }
        webView.loadUrl("http://localhost:5173")
    }

    private fun navigateToArticle(articleId: Int) {
        mainHandler.post {
            webView.evaluateJavascript(
                "javascript:window.routerPush && window.routerPush('/article/$articleId')",
                null
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_FRAUD,
                getString(R.string.notification_channel_fraud),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "防诈警告通知"
                enableVibration(true)
                setBypassDnd(true)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showFraudNotification(warningMessage: String, articleId: Int) {
        Log.i("FraudNotif", "准备发送通知: $warningMessage, articleId=$articleId")
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("articleId", articleId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullText = "$warningMessage\n\n您可能正在面临诱导性推销或欺诈，请谨慎。点击此处查看科学处理方法。"
        val notification = NotificationCompat.Builder(this, CHANNEL_FRAUD)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ 防诈警告")
            .setContentText(fullText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, false)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID_FRAUD, notification)
    }

    private fun requestMediaProjection() {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_MEDIA_PROJECTION) return

        if (resultCode == Activity.RESULT_OK && data != null) {
            sendStatus("授权通过，启动音频捕获服务…")
            val intent = Intent(this, AudioCaptureService::class.java).apply {
                putExtra(AudioCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(AudioCaptureService.EXTRA_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            sendStatus("授权被拒绝 (resultCode=$resultCode)，请重新打开应用授权")
        }
    }

    fun sendTextToWeb(text: String) {
        val escaped = text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
        mainHandler.post {
            webView.evaluateJavascript(
                "javascript:window.receiveAudioText('$escaped')",
                null
            )
        }
    }

    fun sendStatus(msg: String) {
        val escaped = msg
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", " ")
            .replace("\r", "")
        mainHandler.post {
            webView.evaluateJavascript(
                "javascript:window.onAppStatus('$escaped')",
                null
            )
        }
    }

    override fun onDestroy() {
        TextDispatcher.callback = null
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 2001
        private const val CHANNEL_FRAUD = "fraud_alert_channel"
        private const val NOTIFICATION_ID_FRAUD = 2002
        private const val REQUEST_NOTIFICATION_PERMISSION = 2003
    }
}

object TextDispatcher {
    @Volatile var callback: ((String) -> Unit)? = null

    fun dispatch(text: String) {
        callback?.invoke(text)
    }
}
