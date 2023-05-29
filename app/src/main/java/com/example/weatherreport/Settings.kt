package com.example.weatherreport

data class Settings(var repeat: Boolean = true, var repeathours: Int = 3,
                    var repeattype: Boolean = false, var length: Int = 10,
                    var lengthscreen: Boolean =true, var lengthlock: Boolean =true,
                    var lengthclose: Boolean =false, var delay: Int = 10) {
    constructor(input: String?) : this() {
        if(input != null && input != "") {
            val inner = input.substring(9,input.length-1)
            val pairs = inner.split(", ")
            for(item in pairs) {
                val currentpair = item.split("=")
                when (currentpair[0]) {
                    "repeat" -> repeat = currentpair[1].toBoolean()
                    "repeathours" -> repeathours = currentpair[1].toInt()
                    "repeattype" -> repeattype = currentpair[1].toBoolean()
                    "length" -> length = currentpair[1].toInt()
                    "lengthscreen" -> lengthscreen = currentpair[1].toBoolean()
                    "lengthlock" -> lengthlock = currentpair[1].toBoolean()
                    "lengthclose" -> lengthclose = currentpair[1].toBoolean()
                    "delay" -> delay = currentpair[1].toInt()
                }
            }
        }
    }
}
