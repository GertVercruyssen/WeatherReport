package com.example.weatherreport
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadCastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == "com.example.weatherreport") {
            val length = intent.getIntExtra("ShowWeatherLength",10)
            val minutes = intent.getIntExtra("ShowWeatherHour",9)
            val hour = intent.getIntExtra("ShowWeatherMinutes",20)
            val newintent =Intent(context,WeatherActivity::class.java)
            newintent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            newintent.putExtra("ShowWeatherLength",length)
            newintent.putExtra("ShowWeatherHour",hour)
            newintent.putExtra("ShowWeatherMinutes",minutes)
            context?.startActivity(newintent)
        }
    }
}