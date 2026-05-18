package com.elderescort.app

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.elderescort.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        webView = binding.webView
        setContentView(binding.root)

        initWebView()
        TextDispatcher.callback = { text ->
            if (text.startsWith("[LOG]")) {
                sendStatus(text.removePrefix("[LOG]"))
            } else {
                sendTextToWeb(text)
            }
        }
        requestMediaProjection()
    }

    private fun initWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.addJavascriptInterface(AndroidBridge(), "AndroidApp")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                sendStatus("WebView 已就绪")
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
        // Load bundled frontend production build
        webView.loadUrl("file:///android_asset/index.html")
        // webView.loadUrl("http://localhost:5173")
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
    }
}

object TextDispatcher {
    @Volatile var callback: ((String) -> Unit)? = null

    fun dispatch(text: String) {
        callback?.invoke(text)
    }
}
