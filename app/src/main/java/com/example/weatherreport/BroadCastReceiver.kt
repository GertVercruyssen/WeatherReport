package com.example.weatherreport
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class BroadCastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        MyLog.appendLog(LogLevel.Info, "Broadcast triggered")
        if(intent?.action == "SHOW_WEATHER") {
            val settings = Settings(intent.getStringExtra("settings"))
            if(settings.repeattype)
                setNextAlarm(context, settings)

            val newintent =Intent(context,WeatherActivity::class.java)
            newintent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            newintent.putExtra("settings", settings.toString())
            context.startActivity(newintent)
        }
    }

    private fun setNextAlarm(
        context: Context,
        settings: Settings
    ) {
        val am = context.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.MINUTE, (settings.delay * -1))
            add(Calendar.HOUR_OF_DAY, settings.repeathours)
            set(Calendar.MINUTE, settings.delay)
        }

        val intent = Intent(context, BroadCastReceiver::class.java)
        intent.putExtra("settings", settings.toString())
        intent.action = "SHOW_WEATHER"
        val pintent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pintent)
        val convertedTime = (calendar.timeInMillis / 1000).toDate()
        MyLog.appendLog(LogLevel.Info, "Next time set: $convertedTime")
    }
}