package com.shrey.task1sample.recording

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.shrey.task1sample.R
import com.shrey.task1sample.recording.helper.SharedPreferencesHelper
import com.shrey.task1sample.recording.helper.getDirectoryUri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordingService : Service() {

    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    var isPaused = false

    private val binder = ServiceBinder()

    inner class ServiceBinder : Binder() {
        fun getService(): ScreenRecordingService = this@ScreenRecordingService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }


    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Recording Service")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    fun startRecording(
        fps: Int,
        videoQuality: String,
        recordAudio: Boolean,
        mediaProjection: MediaProjection,
        displayMetrics: DisplayMetrics
    ) {
        Log.e("FPS", "FPS start recording $fps")
        setupMediaRecorder(
            fps,
            videoQuality,
            recordAudio,
            mediaProjection,
            displayMetrics
        )
        mediaRecorder?.start()
        isRecording = true
    }

    private fun setupMediaRecorder(
        fps: Int,
        videoQuality: String,
        recordAudio: Boolean,
        mediaProjection: MediaProjection,
        displayMetrics: DisplayMetrics
    ) {
        val density = displayMetrics.densityDpi
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val videoBitrate = getBitrateForVideoQuality(videoQuality)
        val videoEncoder = MediaRecorder.VideoEncoder.H264
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(applicationContext)
        } else {
            MediaRecorder()
        }.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            if (recordAudio) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setVideoEncoder(videoEncoder)
            if (recordAudio) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }
            setVideoEncodingBitRate(videoBitrate)
            setVideoFrameRate(fps)
            setOutputFile(getOutputFile().absolutePath)
            setVideoSize(width, height)
            try {
                prepare()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenRecorder",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )
    }

    fun pauseRecording() {
        if (isRecording && !isPaused) {
            mediaRecorder?.pause()
            isPaused = true
        }
    }

    fun resumeRecording() {
        if (isRecording && isPaused) {
            mediaRecorder?.resume()
            isPaused = false
        }
    }

    fun stopRecording() {
        if (isRecording) {
            virtualDisplay?.release()
            virtualDisplay = null
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            virtualDisplay?.release()
            isRecording = false
            isPaused = false
            Toast.makeText(this, "Video save progressing...", Toast.LENGTH_SHORT).show()
            val videoFile = getOutputFile()
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATA, videoFile.absolutePath)
            }
            val resolver = contentResolver
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            Toast.makeText(this, "Video saved successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBitrateForVideoQuality(quality: String): Int {
        val multiplier = 1000000 // 1 Mbps = 1,000,000 bit
        val bitrate = when (quality) {
            "SD" -> 2.5 // Mbps
            "HD" -> 5.0 // Mbps
            "FULLHD" -> 8.0 // Mbps
            "2K" -> 16.0 // Mbps
            "4K" -> 40.0 // Mbps
            else -> 5.0 // Default is HD quality
        }
        return (bitrate * multiplier).toInt()
    }


    private fun getOutputFile(): File {
        val directoryUriString = SharedPreferencesHelper(this).getDirectoryUri()

        val directoryPath: String = if (!directoryUriString.isNullOrEmpty()) {
            val directory = File(directoryUriString)
            if (directory.exists() && directory.isDirectory) {
                directory.absolutePath
            } else {
                directory.mkdirs()
                directory.absolutePath
            }
        } else {
            val moviesDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val screenRecordingsDirectory = File(moviesDirectory, "ScreenRecordings")
            screenRecordingsDirectory.mkdirs()
            screenRecordingsDirectory.absolutePath
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "screen_recording_$timeStamp.mp4"

        return File(directoryPath, fileName)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "screen_recording_channel"
    }
}