package com.example.weatherreport
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadCastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action == "com.example.weatherreport") {
            val screen = intent.getIntExtra("ShowScreenLength",10)
            val delay = intent.getIntExtra("ShowDelay",5)
            val newintent =Intent(context,WeatherActivity::class.java)
            newintent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            newintent.putExtra("ShowScreenLength",screen)
            newintent.putExtra("ShowDelay",delay)
            context?.startActivity(newintent)
        }
    }
}