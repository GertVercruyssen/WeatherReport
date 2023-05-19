package com.example.weatherreport

import android.content.res.Resources
import android.os.Environment
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel {
    Info, Error, Test, JS
}

object MyLog {
    private val filelimit: Int = 3
    private val filesize: Int = 1024
    private val appname: String = "WeatherReport"

    private var logcount: Long = 0
    private val downloadFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    private var currentLogFile: File? = null

    fun appendLog(level: LogLevel, text: String) {
        logcount++
        val currentTime = Calendar.getInstance().time
        val format1 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN)
        val currentTimeString = format1.format(currentTime)
        var logtext = "$currentTimeString $level: $text"

        //if you want to limit by amount of messages instead
        //        logs.add(text);
//        if(logs.size()> 100)
//            logs.remove(0);

//        if (!BuildConfig.DEBUG)
//            return

        //looking up the logfiles is expensive (storage access) so check once every 10 times
        if (logcount % 10 == 1L) currentLogFile = getCurrentLogFile()
        if (currentLogFile != null) {
            try {
                //BufferedWriter for performance, true to set append to file flag
                val buf = BufferedWriter(FileWriter(currentLogFile, true))
                buf.append(logtext)
                buf.newLine()
                buf.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    //    public String logsToJson() {
    //        ArrayList<String> logMessages = new ArrayList<String>();
    //        //logMessages = logs;
    //        try {
    //            File[] files = downloadFolder.listFiles();
    //            List<File> logfiles = new ArrayList<File>();
    //            if(files == null) { //Directory does not exist
    //                return "";
    //            }
    //            if(files.length == 0) {
    //                return "";
    //            }
    //            for (File file : files)
    //            {
    //                if(file.getName().startsWith("TicketMachineHelper"))
    //                    logfiles.add(file);
    //            }
    //            Collections.sort(logfiles);
    //            for (File file : logfiles)
    //            {
    //                if (file.exists()) {
    //                    InputStream is = new FileInputStream(file);
    //                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    //                    StringBuilder sb = new StringBuilder();
    //                    String line = null;
    //                    while ((line = reader.readLine()) != null) {
    //                        logMessages.add(line);
    //                    }
    //                    reader.close();
    //                }
    //            }
    //
    //        } catch (Exception e) {
    //            appendLog(LogLevel.Error, "Could not read logfiles: "+e.getMessage());
    //        }
    //
    //        String result = "{ \"MyStringArray\" : [";
    //        for(String line : logMessages) {
    //            result += "\""+line+"\",";
    //        }
    //        result = result.substring(0,result.length()-1); //remove trailing comma
    //        result += "] }";
    //        return result;
    //    }

    private fun getCurrentLogFile(): File? {
        val files = downloadFolder.listFiles()
        val logfiles: MutableList<File> = ArrayList()
        if (files == null) { //Directory does not exist
            return null
        }
        if (files.isEmpty()) {
            return makeNewLogFile()
        }
        for (file in files) {
            if (file.name.startsWith(appname)) logfiles.add(file)
        }
        logfiles.sort()
        if (logfiles.size == 0) {
            return makeNewLogFile()
        }
        val latestfilesize = logfiles[logfiles.size - 1].length() / filesize //in kB
        if (latestfilesize < filesize) { //1mb size limit
            return logfiles[logfiles.size - 1]
        }
        val logFile = makeNewLogFile()
        if (logfiles.size >= filelimit) { // max amount of files limit
            logfiles[logfiles.size - 1].delete()
        }
        return logFile
    }

    private fun makeNewLogFile(): File {
        return File(
            downloadFolder.path,
            appname +
                    Calendar.getInstance()[Calendar.YEAR] +
                    Calendar.getInstance()[Calendar.MONTH] +
                    Calendar.getInstance()[Calendar.DAY_OF_MONTH] + ".txt"
        )
    }
}