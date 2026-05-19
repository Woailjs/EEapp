package com.elderescort.app

import android.util.Log
import android.webkit.JavascriptInterface

class AndroidBridge(
    private val onFraudDetected: (warningMessage: String, articleId: Int) -> Unit
) {
    @JavascriptInterface
    fun showFraudAlert(warningMessage: String, articleId: Int) {
        Log.i("FraudBridge", "JS调用 showFraudAlert: $warningMessage, articleId=$articleId")
        onFraudDetected(warningMessage, articleId)
    }

    @JavascriptInterface
    fun onAudioText(text: String) {}

    @JavascriptInterface
    fun getStatus(): String = "connected"
}
