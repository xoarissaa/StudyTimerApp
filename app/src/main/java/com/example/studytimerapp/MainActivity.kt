package com.example.studytimerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    private lateinit var timerDisplay: TextView
    private lateinit var startButton: Button
    private lateinit var resetButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var soundButton: ImageButton
    private lateinit var timerTabs: TabLayout
    private var mediaPlayer: MediaPlayer? = null

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 25 * 60 * 1000
    private var timerRunning = false
    private var isSoundEnabled = true

    private var pomodoroCount = 0
    private val POMODOROS_BEFORE_LONG_BREAK = 4
    private val POMODORO_DURATION = 25 * 60 * 1000L
    private val SHORT_BREAK_DURATION = 5 * 60 * 1000L
    private val LONG_BREAK_DURATION = 15 * 60 * 1000L

    private val CHANNEL_ID = "pomodoro_notification_channel"
    private val NOTIFICATION_ID = 1
    private var defaultTimeLeftInMillis: Long = POMODORO_DURATION

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timerDisplay = findViewById(R.id.timerDisplay)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.resetButton)
        settingsButton = findViewById(R.id.settingsButton)
        soundButton = findViewById(R.id.soundButton)
        timerTabs = findViewById(R.id.timerTabs)

        updateTimerDisplay()
        setupListeners()
        createNotificationChannel()
        loadSoundSettings()
    }

    private fun setupListeners() {
        timerTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> setTimerDuration(POMODORO_DURATION)
                    1 -> setTimerDuration(SHORT_BREAK_DURATION)
                    2 -> setTimerDuration(LONG_BREAK_DURATION)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        startButton.setOnClickListener {
            if (timerRunning) pauseTimer() else startTimer()
        }

        resetButton.setOnClickListener { resetTimer() }
        settingsButton.setOnClickListener { showSettingsDialog() }
        soundButton.setOnClickListener { toggleSound() }
    }

    private fun setTimerDuration(milliseconds: Long) {
        countDownTimer?.cancel()
        defaultTimeLeftInMillis = milliseconds
        timeLeftInMillis = defaultTimeLeftInMillis
        updateTimerDisplay()
        timerRunning = false
        startButton.text = getString(R.string.start)
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = defaultTimeLeftInMillis
        updateTimerDisplay()
        timerRunning = false
        startButton.text = getString(R.string.start)
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                timerRunning = false
                startButton.text = getString(R.string.start)
                handleTimerCompletion()
                if (isSoundEnabled) playFinishSound()
                showTimerCompletionNotification()
                vibrateDevice()
            }
        }.start()
        timerRunning = true
        startButton.text = getString(R.string.pause)
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        startButton.text = getString(R.string.start)
    }

    private fun updateTimerDisplay() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        timerDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun showSettingsDialog() {
        SoundSettingsDialog(this).show()
    }

    private fun toggleSound() {
        isSoundEnabled = !isSoundEnabled
        saveSoundToggleSetting()
        soundButton.setImageResource(if (isSoundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
    }

    private fun saveSoundToggleSetting() {
        val prefs = getSharedPreferences("SoundSettings", MODE_PRIVATE)
        prefs.edit().putBoolean("playFinishSound", isSoundEnabled).apply()
    }

    private fun playFinishSound() {
        val prefs = getSharedPreferences("SoundSettings", MODE_PRIVATE)
        val soundIndex = prefs.getInt("startSoundIndex", 0)
        val volume = prefs.getInt("volume", 50) / 100f
        val soundMap = mapOf(
            0 to R.raw.bird_sound,
            1 to R.raw.rain_wind_chimes,
            2 to R.raw.birds_nature_sound,
            3 to R.raw.night_ambience
        )
        val soundResId = soundMap[soundIndex] ?: R.raw.bird_sound
        mediaPlayer = MediaPlayer.create(this, soundResId)
        mediaPlayer?.apply {
            setVolume(volume, volume)
            start()
            setOnCompletionListener { it.release() }
        }
    }

    private fun handleTimerCompletion() {
        when (timerTabs.selectedTabPosition) {
            0 -> {
                pomodoroCount++
                if (pomodoroCount >= POMODOROS_BEFORE_LONG_BREAK) {
                    timerTabs.getTabAt(2)?.select()
                    pomodoroCount = 0
                } else {
                    timerTabs.getTabAt(1)?.select()
                }
            }
            1, 2 -> timerTabs.getTabAt(0)?.select()
        }
    }

    private fun showTimerCompletionNotification() {
        val title = when (timerTabs.selectedTabPosition) {
            0 -> "Pomodoro Complete!"
            1 -> "Short Break Over!"
            2 -> "Long Break Over!"
            else -> "Timer Done!"
        }
        val message = if (timerTabs.selectedTabPosition == 0) "Take a break!" else "Time to focus!"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reset)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Pomodoro Timer notifications"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun loadSoundSettings() {
        val prefs = getSharedPreferences("SoundSettings", MODE_PRIVATE)
        isSoundEnabled = prefs.getBoolean("playFinishSound", true)
        soundButton.setImageResource(if (isSoundEnabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
    }
}
