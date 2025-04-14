package com.example.studytimerapp

import android.app.Dialog
import android.media.MediaPlayer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View

class SoundSettingsDialog(private val context: AppCompatActivity) {
    private lateinit var dialog: Dialog
    private lateinit var startSoundSpinner: Spinner
    private lateinit var playFinishSoundCheckbox: CheckBox
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var saveButton: Button

    // Sound options
    private val soundOptions = arrayOf(
        "Bird",
        "Chime",
        "Bird Nature",
        "Night Ambience"
    )

    // Preload sound files
    private val soundFiles = mapOf(
        "Bird" to R.raw.bird_sound,
        "Chime" to R.raw.rain_wind_chimes,
        "Bird Nature" to R.raw.birds_nature_sound,
        "Night Ambience" to R.raw.night_ambience
    )

    fun show() {
        // Create dialog
        dialog = Dialog(context)
        dialog.setContentView(R.layout.settings_dialog_layout)

        // Initialize UI components
        startSoundSpinner = dialog.findViewById(R.id.startSoundSpinner)
        playFinishSoundCheckbox = dialog.findViewById(R.id.playFinishSoundCheckbox)
        volumeSeekBar = dialog.findViewById(R.id.volumeSeekBar)
        saveButton = dialog.findViewById(R.id.saveChangesButton)

        // Setup spinner
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            soundOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        startSoundSpinner.adapter = adapter

        // Load previous settings
        loadSavedSettings()

        // Setup save button
        saveButton.setOnClickListener {
            saveSettings()
            dialog.dismiss()
        }

        // Setup sound preview when selecting sound
        startSoundSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                previewSound(soundOptions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        dialog.show()
    }

    private fun previewSound(soundName: String) {
        soundFiles[soundName]?.let { soundResId ->
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        }
    }

    private fun loadSavedSettings() {
        // Load saved preferences
        val prefs = context.getSharedPreferences("SoundSettings", AppCompatActivity.MODE_PRIVATE)

        // Restore selected sound
        val savedSoundIndex = prefs.getInt("startSoundIndex", 0)
        startSoundSpinner.setSelection(savedSoundIndex)

        // Restore finish sound checkbox
        val playFinishSound = prefs.getBoolean("playFinishSound", true)
        playFinishSoundCheckbox.isChecked = playFinishSound

        // Restore volume
        val savedVolume = prefs.getInt("volume", 50)
        volumeSeekBar.progress = savedVolume
    }

    private fun saveSettings() {
        // Save preferences
        val prefs = context.getSharedPreferences("SoundSettings", AppCompatActivity.MODE_PRIVATE)
        val editor = prefs.edit()

        // Save selected sound
        editor.putInt("startSoundIndex", startSoundSpinner.selectedItemPosition)

        // Save finish sound setting
        editor.putBoolean("playFinishSound", playFinishSoundCheckbox.isChecked)

        // Save volume
        editor.putInt("volume", volumeSeekBar.progress)

        editor.apply()
    }
}