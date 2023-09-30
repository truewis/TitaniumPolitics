package com.capsulezero.game.core
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    var _alertLevel = 0;
    var places = hashMapOf<String, Place>()
    var characters = hashMapOf<String, Character>()
    var nonPlayerAgents = hashMapOf<String, NonPlayerAgent>()
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


}