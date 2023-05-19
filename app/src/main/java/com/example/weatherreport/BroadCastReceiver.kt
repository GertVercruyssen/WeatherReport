package com.example.weatherreport
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadCastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        MyLog.appendLog(LogLevel.Info, "Broadcast triggered")
        if(intent?.action == "SHOW_WEATHER") {
            val screen = intent.getIntExtra("length",10)
            val delay = intent.getIntExtra("delay",5)
            val interval = intent.getIntExtra("interval",3)
            val newintent =Intent(context,WeatherActivity::class.java)
            newintent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            newintent.putExtra("length",screen)
            newintent.putExtra("delay",delay)
            newintent.putExtra("interval",interval)
            context?.startActivity(newintent)
        }
    }
}