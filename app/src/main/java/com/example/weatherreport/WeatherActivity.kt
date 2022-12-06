package com.example.weatherreport

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Intent
import android.os.*
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherreport.databinding.ActivityWeatherBinding
import java.util.*


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding
    private lateinit var fullscreenContent: RelativeLayout
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler(Looper.myLooper()!!)

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = binding.fullscreenContent
        fullscreenContent.setOnClickListener { toggle() }
        fullscreenContentControls = binding.fullscreenContentControls
        hide()
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        binding.dummyButton.setOnTouchListener(delayHideTouchListener)

        val length = intent.getIntExtra("ShowWeatherLength",10)
        val minutes = intent.getIntExtra("ShowWeatherHour",9)
        val hour = intent.getIntExtra("ShowWeatherMinutes",20)
        setAlarm(hour,minutes, length)
        displayWeather(length)
    }

    private fun setAlarm(hour: Int, minutes: Int, length: Int) {
        //Set alarm for tomorrow
        AlarmManager.RTC_WAKEUP
        val am= applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        //tomorrow morning
        val tomorrowmorning = GregorianCalendar().apply {
            add(Calendar.DATE, 1)
            set(Calendar.HOUR_OF_DAY,hour)
            set(Calendar.MINUTE, minutes)
        }.timeInMillis

        //set the intent
//        val intent = Intent(applicationContext, WeatherActivity::class.java)
//        intent.action = "com.example.weatherreport"
//        intent.putExtra("ShowWeatherLength",length)
//        intent.putExtra("ShowWeatherHour",hour)
//        intent.putExtra("ShowWeatherMinutes",minutes)
//        val pintent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val intent = Intent(applicationContext, BroadCastReceiver::class.java)
        intent.action = "com.example.weatherreport"
        intent.putExtra("ShowWeatherLength",length)
        intent.putExtra("ShowWeatherHour",hour)
        intent.putExtra("ShowWeatherMinutes",minutes)
        val pintent= PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,tomorrowmorning, pintent)
        //am.setRepeating(AlarmManager.RTC_WAKEUP,tomorrowmorning,AlarmManager.INTERVAL_DAY, pintent)
    }

    private fun displayWeather(length: Int) {
        //Get the weatherData (Async)

        //wakeUpAndUnlock(length)

        //Get the weatherData (finish)

        //Set the UI and fill in with weatherdata
    }

    private fun wakeUpAndUnlock(length: Int) {

        //copy-pasted from stock android alarm
        val win: Window = window
        win.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        win.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        //wake up the screen
        //WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "WeatherReport:wakeTAG"
        )
        wakeLock.acquire(length * 60 * 1000L /*1 minutes*/)

        val keyguardManager =
            applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val keyguardLock = keyguardManager.newKeyguardLock("WeatherReport:unlockTAG")
        keyguardLock.disableKeyguard()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}