package com.shrey.task1sample.recording.helper

fun SharedPreferencesHelper.saveDirectoryUri(directoryUri: String?) {
    saveString("directoryUri", directoryUri)
}

fun SharedPreferencesHelper.getDirectoryUri(): String? {
    return getString("directoryUri", null)
}

fun SharedPreferencesHelper.saveVideoFPS(videoFPS : Int) {
    saveInt("videoFPS", videoFPS)
}

fun SharedPreferencesHelper.getVideoFPS(): Int {
    return getInt("videoFPS", 30)
}

fun SharedPreferencesHelper.saveVideoQuality(videoQuality : String) {
    saveString("videoQuality", videoQuality)
}

fun SharedPreferencesHelper.getVideoQuality(): String? {
    return getString("videoQuality", "HD")
}

fun SharedPreferencesHelper.saveRecordAudio(recordAudio : Boolean) {
    saveBoolean("recordAudio", recordAudio)
}

fun SharedPreferencesHelper.getRecordAudio(): Boolean {
    return getBoolean("recordAudio", false)
}