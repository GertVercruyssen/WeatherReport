package com.example.weatherreport

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardLock
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.*
import android.os.PowerManager.WakeLock
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.weatherreport.databinding.ActivityWeatherBinding
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import java.lang.Runnable
import java.lang.Thread.sleep
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread
import kotlin.system.exitProcess


sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

data class DisplayData(
    val todayWeatherDate: Long,
    val todayWeatherType: weatherData,
    val todayTemperature: Float,
    val todayHumid: Float,
    val todayPressure: Float,
    val todayFeelTemperature: Float,
    val todayRainchances: List<Pair<Float, Long>>,
    val nextDays: List<ForecastData>
)data class RequestData (
    val list: List<ForecastData>
)
data class mainData(
    val temp: Float,
    @SerializedName(value = "feels_like")
    val feelslike: Float,
    @SerializedName(value = "grnd_level")
    val grndlvl: Float,
    val humidity: Float
)
data class weatherData(
    val id: String,
    val main: String,
    val description: String,
    val icon: String
)
data class windData(
    val speed: Float
)
data class ForecastData(
    val dt: Long,
    val main: mainData,
    val weather: List<weatherData>,
    val wind: windData,
    val pop: Float
)

//Takes timestamp in seconds NOT MILLIS
fun Long.toDate(format: String = "yyyy-MM-dd HH:mm:ss"): String {
    val sdf = java.text.SimpleDateFormat(format)
    val time: Date = Date(this*1000)
    return sdf.format(time)
}

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class WeatherActivity : AppCompatActivity() {

    private lateinit var keyguardLock: KeyguardLock
    private var wakeLock: WakeLock? = null
    private lateinit var binding: ActivityWeatherBinding
    private lateinit var fullscreenContent: ConstraintLayout
    private lateinit var fullscreenContentControls: LinearLayout
    private lateinit var settings: Settings
    private val hideHandler = Handler(Looper.myLooper()!!)
    private var playingNoise = false
    private var media: MediaPlayer? = null

    //35.681765, 139.664546 thuis
    private val urldaily = "https://api.openweathermap.org/data/2.5/weather?lat=35.681&lon=139.664&appid=2ddcbe80f116f1a66b67526c132f6322&units=metric"
    private val url5days = "https://api.openweathermap.org/data/2.5/forecast?lat=35.681&lon=139.664&appid=2ddcbe80f116f1a66b67526c132f6322&units=metric"

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
            //MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MyLog.appendLog(LogLevel.Info, "Startup WeatherActivity")
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
        media = MediaPlayer.create(applicationContext,R.raw.noise)
        media?.isLooping = true
        binding.dummyButton.setOnTouchListener(delayHideTouchListener)
        binding.dummyButton.setOnClickListener {
            if(playingNoise) {
                media?.pause()
            } else {
                media?.start()
            }
            playingNoise = !playingNoise

        }

        settings = Settings(intent.getStringExtra("settings"))
        displayWeather()
        autoTurnOff()
    }

    private fun autoTurnOff() {
        if (settings.lengthscreen)
            wakeLock?.release()
//        thread(start = true) {
//            sleep(settings.length.toLong() * 60 * 1000);
//            if (settings.lengthscreen) {
//                if (wakeLock == null) {
//                    val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
//                    val wakeLock = pm.newWakeLock(
//                        PowerManager.PARTIAL_WAKE_LOCK,
//                        "WeatherReport:screenoffTAG"
//                    )
//                }
//                wakeLock?.release()
//            }
//            if (settings.lengthlock)
//                keyguardLock.reenableKeyguard();
//            if (settings.lengthclose)
//                exitProcess(0);
//        }
    }

    override fun onResume() {
        super.onResume()
        displayWeather()
        autoTurnOff()
    }

    private fun displayWeather() = runBlocking{
        val job = async { getWeatherData() }

        wakeUpAndUnlock(settings.length)

        val result = job.await()

        setupDisplay(result)
    }

    private fun setupDisplay(weather: Result<DisplayData>) {
        when(weather) {
            is Result.Error -> {
                binding.outputText.text = weather.exception.message
            }
            is Result.Success -> {
                showWeatherData(weather.data)
            }
        }
    }

    private fun showWeatherData(data: DisplayData) {
        binding.outputText.isVisible=false //hide error output

        binding.dateofweather.text = data.todayWeatherDate.toDate("yyyy-MM-dd HH:mm:ss")

        val icon = when(data.todayWeatherType.icon.replace('n','d')) {
            "01d" -> R.drawable.c1d
            "01n" -> R.drawable.c1d
            "02d" -> R.drawable.c2d
            "02n" -> R.drawable.c2d
            "03d" -> R.drawable.c3d
            "03n" -> R.drawable.c3d
            "04d" -> R.drawable.c4d
            "04n" -> R.drawable.c4d
            "09d" -> R.drawable.c9d
            "09n" -> R.drawable.c9d
            "10d" -> R.drawable.c10d
            "10n" -> R.drawable.c10d
            "11d" -> R.drawable.c11d
            "11n" -> R.drawable.c11d
            "13d" -> R.drawable.c13d
            "13n" -> R.drawable.c13d
            "50d" -> R.drawable.c50d
            "50n" -> R.drawable.c50d
            else-> R.drawable.c50d
        }
        binding.icon.setImageResource(icon)

        binding.temp.text = String.format("%.1f", data.todayTemperature)+"°C"

        binding.chart1.input = data.todayRainchances
        binding.chart1.invalidate() //force redraw

        binding.feeltemp.text = String.format("%.1f", data.todayFeelTemperature)+"°C"

        binding.description.text = data.todayWeatherType.description

        binding.tempgraph.input = data.nextDays.map { it.main.temp }
        binding.tempgraph.firsttimestamp = data.nextDays[0].dt

        binding.humid.text = "Humidity: "+ String.format("%.1f", data.todayHumid)+"%"
        binding.pressure.text = "Pressure: "+ String.format("%.1f", data.todayPressure)
    }

    private suspend fun getWeatherData(): Result<DisplayData> {
        return try {
            Result.Success(
                withContext(Dispatchers.IO) { getWeatherDatafromAPI() }
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun getWeatherDatafromAPI(): DisplayData {
        val url =
            URL(url5days)
        val connection = url.openConnection() as HttpsURLConnection

        if (connection.responseCode == 200) {
            val inputStream = connection.inputStream
            val inputStreamReader = inputStream.reader(charset("UTF-8"))
            val receivedData = Gson().fromJson(inputStreamReader, RequestData::class.java)
            inputStreamReader.close()
            inputStream.close()
            return processData(receivedData)
        } else {
            throw Exception("Could not connect to the API")
        }
    }

    private fun processData(requestData: RequestData): DisplayData {
        val rainchances = mutableListOf<Pair<Float, Long>>()
        for (i in 0..15) {
            rainchances.add(requestData.list[i].pop to requestData.list[i].dt)
        }
        val hour = requestData.list[0].dt.toDate("HH").toInt()
        val incommingWeather = when (hour<11) {
            true -> requestData.list[1]
            false -> requestData.list[0]
        }

        return DisplayData(
            incommingWeather.dt,
            incommingWeather.weather[0],
            incommingWeather.main.temp,
            incommingWeather.main.humidity,
            incommingWeather.main.grndlvl,
            incommingWeather.main.feelslike,
            rainchances.toList(),
            requestData.list
        )
    }

    private fun wakeUpAndUnlock(length: Int) {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON )
//        window.addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON  )

        //wake up the screen
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "WeatherReport:wakeTAG"
        )
        wakeLock?.acquire((length+1) * 60 * 1000L )

        val keyguardManager =
            applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardLock = keyguardManager.newKeyguardLock("WeatherReport:unlockTAG")
        keyguardLock.disableKeyguard()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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