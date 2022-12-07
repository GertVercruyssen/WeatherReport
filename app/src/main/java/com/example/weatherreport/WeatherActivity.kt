package com.example.weatherreport

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Intent
import android.os.*
import android.text.format.DateFormat
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.example.weatherreport.databinding.ActivityWeatherBinding
import com.github.matteobattilana.weather.PrecipType
import com.github.matteobattilana.weather.WeatherView
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.lang.Runnable
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList


sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

data class DisplayData(
    val todayWeatherDate: Long,
    val todayWeatherType: weatherData,
    val todayTemperature: Float,
    val todayRainchances: List<Pair<Float, Long>>,
    val next5Days: List<weatherData>
)data class RequestData (
    val list: List<ForecastData>
)
data class mainData(
    val temp: Float,
    val feelslike: Float,
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

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding
    private lateinit var fullscreenContent: ConstraintLayout
    private lateinit var fullscreenContentControls: LinearLayout
    private lateinit var weatherView: WeatherView
    private val hideHandler = Handler(Looper.myLooper()!!)
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
        weatherView = binding.weatherView
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

    private fun displayWeather(length: Int) = runBlocking{
        val job = async { getWeatherData() }

        wakeUpAndUnlock(length)

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

        when(data.todayWeatherType.main) {
            "Rain"->weatherView.setWeatherData(PrecipType.RAIN)
            "Snow"->weatherView.setWeatherData(PrecipType.SNOW)
            "Clear"->weatherView.setWeatherData(PrecipType.CLEAR)
            else->weatherView.visibility = View.INVISIBLE
        }
        binding.dateofweather.text = getDate(data.todayWeatherDate)
        val icon = when(data.todayWeatherType.icon) {
            "01d" -> R.drawable._01d
            "01n" -> R.drawable._01n
            "02d" -> R.drawable._02d
            "02n" -> R.drawable._02n
            "03d" -> R.drawable._03d
            "03n" -> R.drawable._03n
            "04d" -> R.drawable._04d
            "04n" -> R.drawable._04n
            "09d" -> R.drawable._09d
            "09n" -> R.drawable._09n
            "10d" -> R.drawable._10d
            "10n" -> R.drawable._10n
            "11d" -> R.drawable._11d
            "11n" -> R.drawable._11n
            "13d" -> R.drawable._13d
            "13n" -> R.drawable._13n
            "50d" -> R.drawable._50d
            "50n" -> R.drawable._50n
            else-> R.drawable._50n
        }
        binding.icon.setImageResource(icon)

        binding.temp.text = String.format("%.1f", data.todayTemperature)+"°C"

        doBarChart(data.todayRainchances)
    }

    private fun doBarChart(inputdata: List<Pair<Float,Long>>) {
        val entries: ArrayList<BarEntry> = ArrayList()

        //input data
        for (i in inputdata.indices) {
            entries.add(BarEntry(inputdata[i].first, i))
        }
        val barDataSet = BarDataSet(entries, "rain chance")
        val allBarDataSets = ArrayList<IBarDataSet>()
        allBarDataSets.add(barDataSet)

        val data = BarData(listOf(), allBarDataSets)
        binding.chart1.data = data
        binding.chart1.invalidate()

        binding.chart1.xAxis.values = inputdata.map { getDate(it.second) }.toMutableList()
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
        for (i in 0..5) {
            rainchances.add(requestData.list[i].pop to requestData.list[i].dt)
        }
        val incommingWeather = when (requestData.list[0].dt < (System.currentTimeMillis()*1000)+5000) {
            true -> requestData.list[1]
            false -> requestData.list[0]
        }

        return DisplayData(
            incommingWeather.dt,
            incommingWeather.weather[0],
            incommingWeather.main.temp,
            rainchances.toList(),
            requestData.list.map { it.weather[0] }
        )
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

    //Takes timestamp in seconds NOT MILLIS
    fun getDate(timestamp: Long) :String {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = timestamp
        val date = DateFormat.format("HH:MM dd-MM-yyyy",calendar).toString()
        return date
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