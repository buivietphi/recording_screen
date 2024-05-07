package com.shrey.task1sample.recording

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.provider.DocumentsContract
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shrey.task1sample.R
import com.shrey.task1sample.recording.utils.ASFUriUtils
import com.shrey.task1sample.recording.helper.SharedPreferencesHelper
import com.shrey.task1sample.recording.helper.getDirectoryUri
import com.shrey.task1sample.recording.helper.getRecordAudio
import com.shrey.task1sample.recording.helper.getVideoFPS
import com.shrey.task1sample.recording.helper.getVideoQuality
import com.shrey.task1sample.recording.helper.saveDirectoryUri
import com.shrey.task1sample.recording.helper.saveRecordAudio
import com.shrey.task1sample.recording.helper.saveVideoFPS
import com.shrey.task1sample.recording.helper.saveVideoQuality
import kotlinx.android.synthetic.main.activity_screen_recording.clearButton
import kotlinx.android.synthetic.main.activity_screen_recording.fpsSpinner
import kotlinx.android.synthetic.main.activity_screen_recording.pathTextView
import kotlinx.android.synthetic.main.activity_screen_recording.pauseButton
import kotlinx.android.synthetic.main.activity_screen_recording.qualitySpinner
import kotlinx.android.synthetic.main.activity_screen_recording.recordAudioSwitch
import kotlinx.android.synthetic.main.activity_screen_recording.recordingControlsLayout
import kotlinx.android.synthetic.main.activity_screen_recording.startButton
import kotlinx.android.synthetic.main.activity_screen_recording.stopButton


class ScreenRecordingActivity : AppCompatActivity() {

    private lateinit var recordingService: ScreenRecordingService
    private var bound = false
    private var mediaProjection: MediaProjection? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val serviceBinder = binder as ScreenRecordingService.ServiceBinder
            recordingService = serviceBinder.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_recording)
        handleDataUI()
        clearButton.setOnClickListener { clearSettings() }
        startButton.setOnClickListener { startRecording() }
        pauseButton.setOnClickListener { pauseRecording() }
        stopButton.setOnClickListener { stopRecording() }
        val intent = Intent(this, ScreenRecordingService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun handleDataUI() {
        createSpinnerFPS()
        createSpinnerVideoQuality()
        handlePathSave()
        handleEnableMicrophone()
    }

    private fun clearSettings() {
        SharedPreferencesHelper(this).saveDirectoryUri("")
        SharedPreferencesHelper(this).saveVideoQuality("HD")
        SharedPreferencesHelper(this).saveVideoFPS(30)
        SharedPreferencesHelper(this).saveRecordAudio(false)
        handleDataUI()
    }

    private fun handleEnableMicrophone() {
        recordAudioSwitch.isChecked = SharedPreferencesHelper(this).getRecordAudio()
        recordAudioSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                checkPermissionsRecording()
            } else {
                SharedPreferencesHelper(this).saveRecordAudio(false)
            }
        }
    }

    private fun handlePathSave() {
        val path = SharedPreferencesHelper(this).getDirectoryUri()
        if (!path.isNullOrEmpty()) {
            pathTextView.text = path
        } else {
            pathTextView.text = "/storage/emulated/0/Movies/ScreenRecordings"
        }
        pathTextView.setOnClickListener {
            openDirectoryPicker()
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        directoryPickerLauncher.launch(intent)
    }

    private val directoryPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val directoryUri = data?.data
                val docUri = DocumentsContract.buildDocumentUriUsingTree(
                    directoryUri,
                    DocumentsContract.getTreeDocumentId(directoryUri)
                )
                val path: String = ASFUriUtils.getPath(this, docUri)
                pathTextView.text = path
                SharedPreferencesHelper(this).saveDirectoryUri(path)
            }
        }


    private fun createSpinnerFPS() {
        val step = 5
        val minValue = 5
        val maxValue = 60
        val fpsValues = (minValue..maxValue step step).map { it.toString() }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fpsValues)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fpsSpinner.adapter = adapter

        // Set default selection to 30
        val defaultPosition = (SharedPreferencesHelper(this).getVideoFPS() - minValue) / step
        fpsSpinner.setSelection(defaultPosition)
        // Set up listener for spinner selection
        fpsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedValueStr = parent?.getItemAtPosition(position) as? String
                selectedValueStr?.let {
                    val selectedValue = it.toIntOrNull()
                    selectedValue?.let {
                        Log.e("value", "$it selected")
                        SharedPreferencesHelper(this@ScreenRecordingActivity).saveVideoFPS(it)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun createSpinnerVideoQuality() {
        val videoQualityValues = listOf("SD", "HD", "FULLHD", "2K", "4K")
        val adapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, videoQualityValues)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        qualitySpinner.adapter = adapter

        // Set default selection to HD
        val defaultQuality = SharedPreferencesHelper(this).getVideoQuality()
        val defaultPosition = videoQualityValues.indexOf(defaultQuality)
        qualitySpinner.setSelection(defaultPosition)

        qualitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedValueStr = parent?.getItemAtPosition(position) as? String
                selectedValueStr?.let {
                    Log.e("value", "$it selected")
                    SharedPreferencesHelper(this@ScreenRecordingActivity).saveVideoQuality(it)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun checkPermissionsRecording() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ),
                RECORD_AUDIO_REQUEST_CODE
            )
        } else {
            SharedPreferencesHelper(this).saveRecordAudio(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private fun startRecording() {
        val mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        requestRecordingLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private val requestRecordingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val mediaProjectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = data?.let {
                    mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, it)
                }

                if (mediaProjection != null && bound) {
                    val fps = fpsSpinner.selectedItem.toString().toIntOrNull() ?: 30
                    val selectedVideoQuality = qualitySpinner.selectedItem.toString()
                    val recordAudio = recordAudioSwitch.isChecked
                    Log.e("GGG", "recordAudio $recordAudio")
                    val displayMetrics = getDisplayMetrics()
                    recordingService.startRecording(
                        fps,
                        selectedVideoQuality,
                        recordAudio,
                        mediaProjection!!,
                        displayMetrics
                    )
                    startButton.visibility = View.GONE
                    recordingControlsLayout.visibility = View.VISIBLE
                    clearButton.visibility = View.GONE
                } else {
                    // Handle the case where mediaProjection is null or service is not bound
                    Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun getDisplayMetrics(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics
    }

    private fun pauseRecording() {
        if (bound) {
            if (recordingService.isPaused) {
                recordingService.resumeRecording()
                pauseButton.text = "Pause"
            } else {
                recordingService.pauseRecording()
                pauseButton.text = "Resume"
            }
        }
    }

    private fun stopRecording() {
        if (bound) {
            recordingService.stopRecording()
            pauseButton.text = "Pause"
            startButton.visibility = View.VISIBLE
            recordingControlsLayout.visibility = View.GONE
            clearButton.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            recordAudioSwitch.isChecked = grantResults[0] == PackageManager.PERMISSION_GRANTED
            SharedPreferencesHelper(this).saveRecordAudio(grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }

    companion object {
        const val RECORD_AUDIO_REQUEST_CODE = 10
        fun newIntent(context: Context) = Intent(context, ScreenRecordingActivity::class.java)
    }
}