package com.shrey.task1sample.recordingjava;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.shrey.task1sample.MainActivity;
import com.shrey.task1sample.R;
import com.shrey.task1sample.recording.ScreenRecordingActivity;
import com.shrey.task1sample.recording.ScreenRecordingService;
import com.shrey.task1sample.recordingjava.helper.RecordingSharedPreferenceJavaExtensions;
import com.shrey.task1sample.recordingjava.helper.SharedPreferencesJavaHelper;
import com.shrey.task1sample.recordingjava.utils.ASFUriUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScreenRecordingJavaActivity extends AppCompatActivity {

    private Spinner fpsSpinner;
    private Spinner qualitySpinner;
    private SwitchMaterial recordAudioSwitch;
    private MaterialButton startButton;
    private MaterialButton pauseButton;
    private MaterialButton stopButton;
    private LinearLayout recordingControlsLayout;
    private TextView pathTextView;
    private MaterialButton clearButton;

    private ScreenRecordingJavaService recordingService;
    private boolean bound = false;
    private MediaProjection mediaProjection;
    private SharedPreferencesJavaHelper sharedPreferencesHelper;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            ScreenRecordingJavaService.ServiceBinder serviceBinder = (ScreenRecordingJavaService.ServiceBinder) binder;
            recordingService = serviceBinder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_recording);
        sharedPreferencesHelper = new SharedPreferencesJavaHelper(this);
        initView();
        bindEvents();
        handleDataUI();

        // Bind to the recording service
        Intent intent = new Intent(this, ScreenRecordingService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initView() {
        fpsSpinner = findViewById(R.id.fpsSpinner);
        qualitySpinner = findViewById(R.id.qualitySpinner);
        recordAudioSwitch = findViewById(R.id.recordAudioSwitch);
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        stopButton = findViewById(R.id.stopButton);
        recordingControlsLayout = findViewById(R.id.recordingControlsLayout);
        pathTextView = findViewById(R.id.pathTextView);
        clearButton = findViewById(R.id.clearButton);
    }

    private void bindEvents() {
        clearButton.setOnClickListener(v -> clearSettings());
        // Start Button
        startButton.setOnClickListener(v -> startRecording());

        // Pause Button
        pauseButton.setOnClickListener(v -> pauseRecording());
        // Stop Button
        stopButton.setOnClickListener(v -> stopRecording());

        // Record Audio Switch
        recordAudioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkPermissionsRecording();
            }
        });
    }

    private void handleDataUI() {
        createSpinnerFPS();
        createSpinnerVideoQuality();
        handlePathSave();
        handleEnableMicrophone();
    }

    private void clearSettings() {
        RecordingSharedPreferenceJavaExtensions.saveDirectoryUri(sharedPreferencesHelper, "");
        RecordingSharedPreferenceJavaExtensions.saveVideoQuality(sharedPreferencesHelper, "HD");
        RecordingSharedPreferenceJavaExtensions.saveVideoFPS(sharedPreferencesHelper, 30);
        RecordingSharedPreferenceJavaExtensions.saveRecordAudio(sharedPreferencesHelper, false);
        handleDataUI();
    }

    private void handleEnableMicrophone() {
        boolean isRecordAudio = RecordingSharedPreferenceJavaExtensions.getRecordAudio(sharedPreferencesHelper);
        recordAudioSwitch.setChecked(isRecordAudio);
        recordAudioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkPermissionsRecording();
            } else {
                RecordingSharedPreferenceJavaExtensions.saveRecordAudio(sharedPreferencesHelper, false);
            }
        });
    }

    private void handlePathSave() {
        String path = RecordingSharedPreferenceJavaExtensions.getDirectoryUri(sharedPreferencesHelper);
        if (path != null && !path.isEmpty()) {
            pathTextView.setText(path);
        } else {
            pathTextView.setText("/storage/emulated/0/Movies/ScreenRecordings");
        }
        pathTextView.setOnClickListener(v -> openDirectoryPicker());
    }

    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        directoryPickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> directoryPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Uri directoryUri = data != null ? data.getData() : null;
                    if (directoryUri != null) {
                        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(
                                directoryUri,
                                DocumentsContract.getTreeDocumentId(directoryUri)
                        );
                        String path = ASFUriUtils.getPath(this, docUri);
                        pathTextView.setText(path);
                        RecordingSharedPreferenceJavaExtensions.saveDirectoryUri(sharedPreferencesHelper, path);
                    }
                }
            });

    private void createSpinnerFPS() {
        int step = 5;
        int minValue = 5;
        int maxValue = 60;
        List<String> fpsValues = new ArrayList<>();
        for (int i = minValue; i <= maxValue; i += step) {
            fpsValues.add(String.valueOf(i));
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fpsValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fpsSpinner.setAdapter(adapter);

        // Set default selection to 30
        int defaultPosition = (RecordingSharedPreferenceJavaExtensions.getVideoFPS(sharedPreferencesHelper) - minValue) / step;
        fpsSpinner.setSelection(defaultPosition);

        // Set up listener for spinner selection
        fpsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedValueStr = (String) parent.getItemAtPosition(position);
                Integer selectedValue = Integer.parseInt(selectedValueStr);
                if (selectedValue != null) {
                    Log.e("value", selectedValue + " selected");
                    RecordingSharedPreferenceJavaExtensions.saveVideoFPS(sharedPreferencesHelper, selectedValue);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }


    private void createSpinnerVideoQuality() {
        List<String> videoQualityValues = Arrays.asList("SD", "HD", "FULLHD", "2K", "4K");
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, videoQualityValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qualitySpinner.setAdapter(adapter);

        // Set default selection to HD
        String defaultQuality = RecordingSharedPreferenceJavaExtensions.getVideoQuality(sharedPreferencesHelper);
        int defaultPosition = videoQualityValues.indexOf(defaultQuality);
        qualitySpinner.setSelection(defaultPosition);

        qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedValueStr = (String) parent.getItemAtPosition(position);
                Log.e("value", selectedValueStr + " selected");
                RecordingSharedPreferenceJavaExtensions.saveVideoQuality(sharedPreferencesHelper, selectedValueStr);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }


    private void checkPermissionsRecording() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_REQUEST_CODE
            );
        } else {
            RecordingSharedPreferenceJavaExtensions.saveRecordAudio(sharedPreferencesHelper, true);
        }

    }

    private void startRecording() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        requestRecordingLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
    }

    private final ActivityResultLauncher<Intent> requestRecordingLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    MediaProjectionManager mediaProjectionManager =
                            (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    mediaProjection = data != null ?
                            mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data) : null;

                    if (mediaProjection != null && bound) {
                        int fps = Integer.parseInt(fpsSpinner.getSelectedItem().toString());
                        String selectedVideoQuality = qualitySpinner.getSelectedItem().toString();
                        boolean recordAudio = recordAudioSwitch.isChecked();
                        Log.e("GGG", "recordAudio " + recordAudio);
                        DisplayMetrics displayMetrics = getDisplayMetrics();
                        recordingService.startRecording(
                                fps,
                                selectedVideoQuality,
                                recordAudio,
                                mediaProjection,
                                displayMetrics
                        );
                        startButton.setVisibility(View.GONE);
                        clearButton.setVisibility(View.GONE);
                        recordingControlsLayout.setVisibility(View.VISIBLE);
                    } else {
                        // Handle the case where mediaProjection is null or service is not bound
                        Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );


    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }


    private void pauseRecording() {
        if (bound) {
            if (recordingService.isPaused) {
                recordingService.resumeRecording();
                pauseButton.setText("Pause");
            } else {
                recordingService.pauseRecording();
                pauseButton.setText("Resume");
            }
        }
    }

    private void stopRecording() {
        if (bound) {
            recordingService.stopRecording();
            pauseButton.setText("Pause");
            startButton.setVisibility(View.VISIBLE);
            recordingControlsLayout.setVisibility(View.GONE);
            clearButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_REQUEST_CODE) {
            recordAudioSwitch.setChecked(grantResults[0] == PackageManager.PERMISSION_GRANTED);
            RecordingSharedPreferenceJavaExtensions.saveRecordAudio(sharedPreferencesHelper, grantResults[0] == PackageManager.PERMISSION_GRANTED);
        }
    }


    public static final int RECORD_AUDIO_REQUEST_CODE = 10;

    public static Intent newIntent(Context context) {
        return new Intent(context, ScreenRecordingActivity.class);
    }
}
