package com.example.weatherreport

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //get settings from sharedprefs
        sharedPref = getPreferences(Context.MODE_PRIVATE)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.timepicker.setIs24HourView(true)
        binding.timepicker.hour = sharedPref.getInt("hours", 9)
        binding.timepicker.minute = sharedPref.getInt("minutes", 20)
        binding.lengthvalue.minValue=1
        binding.lengthvalue.maxValue=30
        binding.lengthvalue.value = sharedPref.getInt("length", 10)

        binding.okButton.setOnClickListener{
            saveSettings(binding.timepicker.hour,binding.timepicker.minute,binding.lengthvalue.value)
            setAlarm(binding.timepicker.hour,binding.timepicker.minute,binding.lengthvalue.value)
        }
        binding.showButton.setOnClickListener{
            val intent = Intent(applicationContext, WeatherActivity::class.java)
            intent.putExtra("ShowWeatherLength",binding.lengthvalue.value)
            this.startActivity(intent)
        }
    }

    private fun saveSettings(hour: Int, minute: Int, value: Int) {
        val editor = sharedPref.edit()
        editor.putInt("hours",hour)
        editor.putInt("minutes",minute)
        editor.putInt("length",value)
        editor.apply()
    }

    private fun setAlarm(hour: Int, minutes: Int, length: Int) {
        //Set alarm for tomorrow
        AlarmManager.RTC_WAKEUP
        val am= applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        //tomorrow morning
        val tomorrowmorning = GregorianCalendar().apply {
            //add(Calendar.DATE, 1)
            set(Calendar.HOUR_OF_DAY,hour)
            set(Calendar.MINUTE, minutes)
        }.timeInMillis

        //set the intent
        val intent = Intent(applicationContext, BroadCastReceiver::class.java)
        intent.action = "com.example.weatherreport"
        intent.putExtra("ShowWeatherLength",length)
        intent.putExtra("ShowWeatherHour",hour)
        intent.putExtra("ShowWeatherMinutes",minutes)
        val pintent= PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,tomorrowmorning, pintent)
        //am.setRepeating(AlarmManager.RTC_WAKEUP,tomorrowmorning,AlarmManager.INTERVAL_DAY, pintent)
    }
}