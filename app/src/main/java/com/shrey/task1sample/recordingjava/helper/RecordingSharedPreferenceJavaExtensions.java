package com.shrey.task1sample.recordingjava.helper;

public class RecordingSharedPreferenceJavaExtensions {
    public static void saveDirectoryUri(SharedPreferencesJavaHelper sharedPreferencesHelper, String directoryUri) {
        sharedPreferencesHelper.saveString("directoryUri", directoryUri);
    }

    public static String getDirectoryUri(SharedPreferencesJavaHelper sharedPreferencesHelper) {
        return sharedPreferencesHelper.getString("directoryUri", null);
    }

    public static void saveVideoFPS(SharedPreferencesJavaHelper sharedPreferencesHelper, int videoFPS) {
        sharedPreferencesHelper.saveInt("videoFPS", videoFPS);
    }

    public static int getVideoFPS(SharedPreferencesJavaHelper sharedPreferencesHelper) {
        return sharedPreferencesHelper.getInt("videoFPS", 30);
    }

    public static void saveVideoQuality(SharedPreferencesJavaHelper sharedPreferencesHelper, String videoQuality) {
        sharedPreferencesHelper.saveString("videoQuality", videoQuality);
    }

    public static String getVideoQuality(SharedPreferencesJavaHelper sharedPreferencesHelper) {
        return sharedPreferencesHelper.getString("videoQuality", "HD");
    }

    public static void saveRecordAudio(SharedPreferencesJavaHelper sharedPreferencesHelper, boolean recordAudio) {
        sharedPreferencesHelper.saveBoolean("recordAudio", recordAudio);
    }

    public static boolean getRecordAudio(SharedPreferencesJavaHelper sharedPreferencesHelper) {
        return sharedPreferencesHelper.getBoolean("recordAudio", false);
    }
}
