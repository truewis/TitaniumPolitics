package com.capsulezero.game.core
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Serializable
class GameState {
    private var _time = 0
    var time: Int
        get() = _time
        set(value) {
            val old = _time
            _time = value
            timeChanged.forEach { it(old, _time) }
        }
    val hour:Int
        get() = _time%48/2
    val day:Int
        get() = _time/48
    @Transient
    var timeChanged = arrayListOf<(Int, Int)->Unit>()
    private var _pop = 0
    var pop: Int
        get() = _pop
        set(value) {
            val old = _pop
            _pop = value
            popChanged.forEach { it(old, _pop) }
        }
    @Transient
    var popChanged = arrayListOf<(Int, Int)->Unit>()

    @Transient
    var updateUI = arrayListOf<(GameState)->Unit>()
    var _alertLevel = 0;
    var places = hashMapOf<String, Place>()
    var characters = hashMapOf<String, Character>()
    var nonPlayerAgents = hashMapOf<String, NonPlayerAgent>()
    var playerAgent = ""
    var log = Log()
    var scheduledMeetings = hashMapOf<String, Meeting>()
    var scheduledConferences = hashMapOf<String, Meeting>()
    var ongoingMeetings = hashMapOf<String, Meeting>()
    var ongoingConferences = hashMapOf<String, Meeting>()
    var budget = hashMapOf<String, Int>()
    var isBudgetProposed = false
    var isBudgetResolved = false
    var informations = hashMapOf<String, Information>()
    var marketWater = 0
    var recyclableWater = 0
    var marketOxygen = 0
    var marketRation = 0

    fun injectDependency(){
        log.injectParent(this)
        places.forEach { it.value.injectParent(this) }
    }


    @OptIn(ExperimentalSerializationApi::class)
    fun dump()
    {

        val prettyJson = Json { // this returns the JsonBuilder
            prettyPrint = true
            // optional: specify indent
            prettyPrintIndent = " "
        }

        val file = File("save${Calendar.getInstance().time.toString("YYYYMMdd_HHmmss")}.json")
        file.writeText(prettyJson.encodeToString(this))
        println("Save File Dumped.")
    }
    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }


}