package com.example.studytimerapp

import android.app.Dialog
import android.media.MediaPlayer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SoundSettingsDialog(private val context: AppCompatActivity) {
    private lateinit var dialog: Dialog
    private lateinit var startSoundSpinner: Spinner
    private lateinit var playFinishSoundCheckbox: CheckBox
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var saveButton: Button

    private val soundOptions = arrayOf(
        "Bird",
        "Chime",
        "Bird Nature",
        "Night Ambience"
    )

    private val soundFiles = mapOf(
        "Bird" to R.raw.bird_sound,
        "Chime" to R.raw.rain_wind_chimes,
        "Bird Nature" to R.raw.birds_nature_sound,
        "Night Ambience" to R.raw.night_ambience
    )

    fun show() {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.settings_dialog_layout)

        // UI elements
        startSoundSpinner = dialog.findViewById(R.id.startSoundSpinner)
        playFinishSoundCheckbox = dialog.findViewById(R.id.playFinishSoundCheckbox)
        volumeSeekBar = dialog.findViewById(R.id.volumeSeekBar)
        saveButton = dialog.findViewById(R.id.saveChangesButton)

        // Spinner setup
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, soundOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        startSoundSpinner.adapter = adapter

        loadSavedSettings()

        // Save button click
        saveButton.setOnClickListener {
            saveSettings()
            dialog.dismiss()
        }

        // Live preview on selecting sound
        startSoundSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                previewSound(soundOptions[position])
                saveCurrentSoundIndex(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Live update volume as user slides
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val prefs = context.getSharedPreferences("SoundSettings", AppCompatActivity.MODE_PRIVATE)
                prefs.edit().putInt("volume", progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Save toggle immediately on click
        playFinishSoundCheckbox.setOnCheckedChangeListener { _, isChecked ->
            val prefs = context.getSharedPreferences("SoundSettings", AppCompatActivity.MODE_PRIVATE)
            prefs.edit().putBoolean("playFinishSound", isChecked).apply()
        }

        dialog.show()
    }

    private fun previewSound(soundName: String) {
        soundFiles[soundName]?.let { soundResId ->
            val mediaPlayer = MediaPlayer.create(context, soundResId)

            // Use saved volume
            val prefs = context.getSharedPreferences("SoundSettings", AppCompatActivity.MODE_PRIVATE)
            val volume = prefs.getInt("volume", 50) / 100f

            mediaPlayer.setVolume(volume, volume)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { it.release() }
        }
    }

    private fun saveCurrentSoundIndex(index: Int) {
        val prefs = context.getSharedPreferences("SoundSettings", AppCompatActivity.MODE_PRIVATE)
        prefs.edit().putInt("startSoundIndex", index).apply()
    }

    private fun loadSavedSettings() {
        val prefs = context.getSharedPreferences("SoundSettings", AppCompatActivity.MODE_PRIVATE)

        startSoundSpinner.setSelection(prefs.getInt("startSoundIndex", 0))
        playFinishSoundCheckbox.isChecked = prefs.getBoolean("playFinishSound", true)
        volumeSeekBar.progress = prefs.getInt("volume", 50)
    }

    private fun saveSettings() {
        // Nothing else needed here now since we save live
        dialog.dismiss()
    }
}
