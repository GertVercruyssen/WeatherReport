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
    private lateinit var currentSettings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmMgr = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        //get settings from sharedprefs
        sharedPref = getPreferences(Context.MODE_PRIVATE)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val saved = sharedPref.getString("settings", "")
        currentSettings = Settings(saved)

        binding.checkBox.isChecked = currentSettings.repeat
        binding.editTextNumber.setText(currentSettings.repeathours.toString())
        binding.alarmtypeBox.isChecked = currentSettings.repeattype
        binding.timeoutNumber.setText(currentSettings.length.toString())
        binding.timeoutType1Box.isChecked = currentSettings.lengthscreen
        binding.timeoutType2Box.isChecked = currentSettings.lengthlock
        binding.timeoutType3Box.isChecked = currentSettings.lengthclose
        binding.delayNumber.setText(currentSettings.delay.toString())

        binding.okButton.setOnClickListener{

            saveSettings()
            alarmIntent = CreateWeatherPendingIntent()

            // Set the alarm to start now + delay
            val calendar: Calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                if(currentSettings.delay == 0)
                    add(Calendar.SECOND, 10)
                else
                    set(Calendar.MINUTE, currentSettings.delay)
            }

            if(currentSettings.repeattype)
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,alarmIntent)
            else {
                // setRepeating() lets you specify a precise custom interval--in this case, interval
                alarmMgr.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    1000 * 60 * 60 * currentSettings.repeathours.toLong(),
                    alarmIntent
                )
            }

            val convertedTime = (calendar.timeInMillis/1000).toDate()
            MyLog.appendLog(LogLevel.Info, "Next time set: $convertedTime")
        }

        binding.clearButton.setOnClickListener {

            saveSettings()

            if (alarmIntent != null) {
                alarmMgr.cancel(alarmIntent)
            } else {
                val  pendingIntent = CreateWeatherPendingIntent()
                alarmMgr.cancel(pendingIntent)
            }
        }
        binding.showButton.setOnClickListener {

            saveSettings()

            val intent = Intent(applicationContext, WeatherActivity::class.java)
            intent.putExtra("settings", currentSettings.toString())
            this.startActivity(intent)
        }
    }

    private fun CreateWeatherPendingIntent() : PendingIntent{
        val intent = Intent(applicationContext, BroadCastReceiver::class.java)
        intent.putExtra("settings", currentSettings.toString())
        intent.action = "SHOW_WEATHER"
        return PendingIntent.getBroadcast(applicationContext,0,intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun saveSettings() {
        currentSettings.repeat = binding.checkBox.isChecked
        currentSettings.repeathours =  binding.editTextNumber.text.toString().toInt()
        currentSettings.repeattype = binding.alarmtypeBox.isChecked
        currentSettings.length = binding.timeoutNumber.text.toString().toInt()
        currentSettings.lengthscreen = binding.timeoutType1Box.isChecked
        currentSettings.lengthlock = binding.timeoutType2Box.isChecked
        currentSettings.lengthclose = binding.timeoutType3Box.isChecked
        currentSettings.delay = binding.delayNumber.text.toString().toInt()

        val editor = sharedPref.edit()
        editor.putString("settings",currentSettings.toString())
        editor.apply()
    }
}