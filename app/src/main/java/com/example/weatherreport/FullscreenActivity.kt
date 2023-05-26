package com.example.weatherreport

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherreport.databinding.ActivityMainBinding
import java.util.*


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */

class FullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPref:SharedPreferences
    private lateinit var alarmMgr: AlarmManager
    private  var alarmIntent:  PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmMgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        //get settings from sharedprefs
        sharedPref = getPreferences(Context.MODE_PRIVATE)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.screenvalue.minValue=1
        binding.screenvalue.maxValue=60
        binding.screenvalue.value = sharedPref.getInt("screen", 10)
        binding.delayvalue.minValue=0
        binding.delayvalue.maxValue=180
        binding.delayvalue.value = sharedPref.getInt("delay", 5)
        binding.intervalvalue.minValue=1
        binding.intervalvalue.maxValue=24
        binding.intervalvalue.value = sharedPref.getInt("interval", 3)

        binding.okButton.setOnClickListener{
            saveSettings(binding.screenvalue.value, binding.delayvalue.value, binding.intervalvalue.value)
            alarmIntent = CreateWeatherPendingIntent(binding.screenvalue.value, binding.delayvalue.value, binding.intervalvalue.value)

            // Set the alarm to start now + delay
            val calendar: Calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.MINUTE, binding.delayvalue.value)
            }

            alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,alarmIntent)
            //alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC,calendar.timeInMillis,alarmIntent)
            // setRepeating() lets you specify a precise custom interval--in this case, interval
//            alarmMgr.setRepeating(
//                AlarmManager.RTC_WAKEUP,
//                calendar.timeInMillis,
//                1000 * 60 * 10, //debug: every 10 mins
////                1000 * 60 * 60 * binding.intervalvalue.value.toLong(),
//                alarmIntent
//            )

            val convertedTime = (calendar.timeInMillis/1000).toDate()
            MyLog.appendLog(LogLevel.Info, "Next time set: $convertedTime")
        }

        binding.clearButton.setOnClickListener {
            saveSettings(binding.screenvalue.value, binding.delayvalue.value, binding.intervalvalue.value)

            if (alarmIntent != null) {
                alarmMgr.cancel(alarmIntent)
            } else {
                val  pendingIntent = CreateWeatherPendingIntent(binding.screenvalue.value, binding.delayvalue.value, binding.intervalvalue.value)
                alarmMgr.cancel(pendingIntent)
            }
        }
        binding.showButton.setOnClickListener {
            saveSettings(binding.screenvalue.value, binding.delayvalue.value, binding.intervalvalue.value)
            val intent = Intent(applicationContext, WeatherActivity::class.java)
            intent.putExtra("length",binding.screenvalue.value)
            intent.putExtra("delay",binding.delayvalue.value)
            intent.putExtra("interval",binding.intervalvalue.value)
            this.startActivity(intent)
        }
    }

    private fun CreateWeatherPendingIntent(length: Int, delay: Int, interval: Int) : PendingIntent{
        val intent = Intent(applicationContext, BroadCastReceiver::class.java)
        intent.putExtra("length", length)
        intent.putExtra("delay", delay)
        intent.putExtra("interval", interval)
        intent.action = "SHOW_WEATHER"
        return PendingIntent.getBroadcast(applicationContext,0,intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun saveSettings(screen: Int, delay: Int, interval:Int) {
        val editor = sharedPref.edit()
        editor.putInt("screen",screen)
        editor.putInt("delay",delay)
        editor.putInt("interval",interval)
        editor.apply()
    }
}