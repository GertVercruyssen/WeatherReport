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

        binding.screenvalue.minValue=1
        binding.screenvalue.maxValue=60
        binding.screenvalue.value = sharedPref.getInt("screen", 10)
        binding.delayvalue.minValue=0
        binding.delayvalue.maxValue=180
        binding.delayvalue.value = sharedPref.getInt("delay", 10)

        binding.okButton.setOnClickListener{
            saveSettings(binding.screenvalue.value, binding.delayvalue.value)
            val intent = Intent(applicationContext, WeatherActivity::class.java)
            intent.putExtra("ShowScreenLength",binding.screenvalue.value)
            intent.putExtra("ShowDelay",binding.delayvalue.value)
            this.startActivity(intent)
        }
    }

    private fun saveSettings(screen: Int, delay: Int) {
        val editor = sharedPref.edit()
        editor.putInt("screen",screen)
        editor.putInt("delay",delay)
        editor.apply()
    }
}