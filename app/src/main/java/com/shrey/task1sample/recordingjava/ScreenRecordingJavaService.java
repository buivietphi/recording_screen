package com.shrey.task1sample.recordingjava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.shrey.task1sample.R;
import com.shrey.task1sample.recordingjava.helper.RecordingSharedPreferenceJavaExtensions;
import com.shrey.task1sample.recordingjava.helper.SharedPreferencesJavaHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecordingJavaService extends Service {

    private static final int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "screen_recording_channel";

    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    public boolean isPaused = false;

    private final IBinder binder = new ServiceBinder();
    private SharedPreferencesJavaHelper sharedPreferencesHelper;

    public class ServiceBinder extends Binder {
        ScreenRecordingJavaService getService() {
            return ScreenRecordingJavaService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        sharedPreferencesHelper = new SharedPreferencesJavaHelper(getApplicationContext());
    }


    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Recording Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Recording Service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    public void startRecording(int fps, String videoQuality, boolean recordAudio, MediaProjection mediaProjection, DisplayMetrics displayMetrics) {
        Log.e("FPS", "FPS start recording " + fps);
        setupMediaRecorder(fps, videoQuality, recordAudio, mediaProjection, displayMetrics);
        mediaRecorder.start();
        isRecording = true;
    }

    private void setupMediaRecorder(int fps, String videoQuality, boolean recordAudio, MediaProjection mediaProjection, DisplayMetrics displayMetrics) {
        int density = displayMetrics.densityDpi;
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        int videoBitrate = getBitrateForVideoQuality(videoQuality);
        int videoEncoder = MediaRecorder.VideoEncoder.H264;

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        if (recordAudio) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setVideoEncoder(videoEncoder);
        if (recordAudio) {
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }
        mediaRecorder.setVideoEncodingBitRate(videoBitrate);
        mediaRecorder.setVideoFrameRate(fps);
        mediaRecorder.setOutputFile(getOutputFile().getAbsolutePath());
        mediaRecorder.setVideoSize(width, height);
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null,
                null
        );
    }

    public void pauseRecording() {
        if (isRecording && !isPaused) {
            mediaRecorder.pause();
            isPaused = true;
        }
    }

    public void resumeRecording() {
        if (isRecording && isPaused) {
            mediaRecorder.resume();
            isPaused = false;
        }
    }

    public void stopRecording() {
        if (isRecording) {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            isPaused = false;
            Toast.makeText(this, "Video save progressing...", Toast.LENGTH_SHORT).show();
            File videoFile = getOutputFile();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.getName());
            contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            contentValues.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());

            ContentResolver resolver = getContentResolver();
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            Toast.makeText(this, "Video saved successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private int getBitrateForVideoQuality(String quality) {
        final int multiplier = 1000000; // 1 Mbps = 1,000,000 bit
        double bitrate;
        switch (quality) {
            case "SD":
                bitrate = 2.5; // Mbps
                break;
            case "HD":
                bitrate = 5.0; // Mbps
                break;
            case "FULLHD":
                bitrate = 8.0; // Mbps
                break;
            case "2K":
                bitrate = 16.0; // Mbps
                break;
            case "4K":
                bitrate = 40.0; // Mbps
                break;
            default:
                bitrate = 5.0; // Default is HD quality
                break;
        }
        return (int) (bitrate * multiplier);
    }


    private File getOutputFile() {
        String directoryUriString = RecordingSharedPreferenceJavaExtensions.getDirectoryUri(sharedPreferencesHelper);

        String directoryPath;
        if (directoryUriString != null && !directoryUriString.isEmpty()) {
            File directory = new File(directoryUriString);
            if (directory.exists() && directory.isDirectory()) {
                directoryPath = directory.getAbsolutePath();
            } else {
                directory.mkdirs();
                directoryPath = directory.getAbsolutePath();
            }
        } else {
            File moviesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            File screenRecordingsDirectory = new File(moviesDirectory, "ScreenRecordings");
            screenRecordingsDirectory.mkdirs();
            directoryPath = screenRecordingsDirectory.getAbsolutePath();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "screen_recording_" + timeStamp + ".mp4";

        return new File(directoryPath, fileName);
    }
}
