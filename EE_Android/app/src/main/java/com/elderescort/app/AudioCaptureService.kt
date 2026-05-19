package com.elderescort.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjectionManager
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioCaptureService : Service() {

    private var audioRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null
    @Volatile private var isRecording = false
    private var model: Model? = null
    private var recognizer: Recognizer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        log("Service onCreate, 前台服务已启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }
        if (resultCode == Int.MIN_VALUE || data == null) {
            log("ERROR: resultCode=$resultCode 或 data 为空")
            stopSelf()
            return START_NOT_STICKY
        }
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(resultCode, data)
        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
        val projection = mediaProjection ?: return
        val sampleRate = 16000

        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_ASSISTANT)
            .addMatchingUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(captureConfig)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
        } else {
            null
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            log("ERROR: AudioRecord 初始化失败")
            stopSelf()
            return
        }

        log("AudioRecord 初始化成功 (buffer=${bufferSize}B), 加载模型…")

        val initStart = System.currentTimeMillis()
        if (!initRecognizer()) {
            log("ERROR: Vosk 模型加载失败")
            stopSelf()
            return
        }
        log("Vosk 模型加载完成 (${System.currentTimeMillis() - initStart}ms)")

        audioRecord?.startRecording()
        isRecording = true
        log("录音已启动")

        Thread {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    processAudio(buffer, read)
                } else if (read == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "AudioRecord ERROR_INVALID_OPERATION")
                }
            }
        }.start()
    }

    private fun initRecognizer(): Boolean {
        val modelDir = File(filesDir, "model")
        if (!modelDir.exists() || modelDir.list()?.isEmpty() == true) {
            log("首次运行，正在解压模型文件 (约42MB)…")
            val extractStart = System.currentTimeMillis()
            extractModelFromAssets(modelDir)
            log("模型解压完成 (${System.currentTimeMillis() - extractStart}ms)")
        } else {
            log("模型已存在: ${modelDir.absolutePath}")
        }

        return try {
            model = Model(modelDir.absolutePath)
            recognizer = Recognizer(model, 16000.0f)
            true
        } catch (e: IOException) {
            Log.e(TAG, "Failed to init Vosk recognizer", e)
            log("ERROR: ${e.message}")
            false
        }
    }

    private fun extractModelFromAssets(targetDir: File) {
        targetDir.mkdirs()
        copyAssetsDir("model", targetDir)
    }

    private fun copyAssetsDir(path: String, target: File) {
        try {
            val list = assets.list(path) ?: return
            for (name in list) {
                val childPath = "$path/$name"
                val childTarget = File(target, name)
                val subList = assets.list(childPath)
                if (subList != null && subList.isNotEmpty()) {
                    copyAssetsDir(childPath, childTarget)
                } else {
                    childTarget.parentFile?.mkdirs()
                    assets.open(childPath).use { input ->
                        FileOutputStream(childTarget).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset: $path", e)
        }
    }

    private var audioBytesRead = 0L
    private var lastAudioLogTime = 0L

    private fun processAudio(buffer: ByteArray, length: Int) {
        val rec = recognizer ?: return
        audioBytesRead += length
        val now = System.currentTimeMillis()
        if (now - lastAudioLogTime > 3000) {
            // 计算 RMS 音量，判断是否真的捕获到了声音
            var sumSq = 0L
            for (i in 0 until length step 2) {
                val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toInt()
                sumSq += sample * sample
            }
            val rms = Math.sqrt(sumSq.toDouble() / (length / 2))
            log("音频收集中… (已接收 ${audioBytesRead / 16000 / 2}s, RMS=${rms.toInt()})")
            lastAudioLogTime = now
        }
        if (rec.acceptWaveForm(buffer, length)) {
            val text = JSONObject(rec.result).optString("text", "").trim()
            if (text.isNotBlank()) {
                log("识别结果: $text")
                TextDispatcher.callback?.invoke(text)
            }
        } else {
            val partial = JSONObject(rec.partialResult).optString("partial", "").trim()
            if (partial.isNotBlank()) {
                log("部分识别: $partial")
                TextDispatcher.callback?.invoke(partial)
            }
        }
    }

    override fun onDestroy() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
        recognizer?.close()
        recognizer = null
        model?.close()
        model = null
        log("Service onDestroy")
        super.onDestroy()
    }

    private fun log(msg: String) {
        Log.i(TAG, msg)
        TextDispatcher.callback?.invoke("[LOG]$msg")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_more)
            .build()
    }

    companion object {
        private const val TAG = "AudioCaptureService"
        private const val CHANNEL_ID = "audio_capture_channel"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
    }
}
