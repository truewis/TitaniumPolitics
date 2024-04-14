package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.quests.Quest1
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Serializable
class GameState
{
    private var _time = 0

    var idlePop = 0
    var time: Int
        get() = _time
        set(value)
        {
            val old = _time
            _time = value
            timeChanged.forEach { it(old, _time) }
        }
    val hour: Int
        get() = _time % ReadOnlyJsons.getConst("lengthOfDay").toInt() / 2
    val day: Int
        get() = _time / ReadOnlyJsons.getConst("lengthOfDay").toInt()


    @Transient
            /*Old time is the time before the change. New time is the time after the change.*/
    var timeChanged =
        arrayListOf<(Int, Int) -> Unit>()
    val pop: Int
        get() = parties.values.sumOf { it.members.size + it.anonymousMembers } + idlePop
    val pickRandomParty: Party
        get()
        {
            //random party picker
            return parties.values.random()
        }

    @Transient
    var popChanged = arrayListOf<(Int, Int) -> Unit>()

    @Transient
    var updateUI = arrayListOf<(GameState) -> Unit>()

    //This is a list of functions that will be called when the game starts.
    @Transient
    var onStart = arrayListOf<() -> Unit>()
    var _alertLevel = 0
    var places = hashMapOf<String, Place>()
    var characters = hashMapOf<String, Character>()
    var nonPlayerAgents = hashMapOf<String, NonPlayerAgent>()
    var playerName = ""

    @Transient
    val player = characters[playerName]!!
    var log = Log()
    var parties = hashMapOf<String, Party>()
    var commands = hashMapOf<String, Command>()

    @Serializable
    private var _mutuality = hashMapOf<String, HashMap<String, Double>>()

    @Serializable
    private var _partyMutuality = hashMapOf<String, HashMap<String, Double>>()
    var scheduledMeetings = hashMapOf<String, Meeting>()
    var scheduledConferences = hashMapOf<String, Meeting>()
    var ongoingMeetings = hashMapOf<String, Meeting>()
    var ongoingConferences = hashMapOf<String, Meeting>()
    var budget = hashMapOf<String, Int>()//Party name to budget
    var isBudgetProposed = false
    var isBudgetResolved = false
    var informations = hashMapOf<String, Information>()
    var floatingResources = hashMapOf<String, Int>()
    var marketResources = hashMapOf<String, Int>()
    var questSystem = QuestSystem()
    var eventSystem = EventSystem()

    fun initialize()
    {
        println("Initializing game state...")
        characters.forEach { char ->
            //Create home for each character.
            places["home_" + char.key] = Place().apply {
                responsibleParty = ""
                //Connect the new home to the place specified in the character.
                connectedPlaces.add(this@GameState.characters[char.key]!!.livingBy)
            }
            places[this@GameState.characters[char.key]!!.livingBy]!!.connectedPlaces.add("home_" + char.key)
            if (places.none { it.value.characters.contains(char.key) })
                places["home_" + char.key]!!.characters.add(char.key)
        }
        questSystem.add(Quest1())
        injectDependency()
        println("Game state initialized successfully.")
    }

    fun getMutuality(a: String, b: String): Double
    {
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Getting mutuality $a -> $b invalid.")
        if (!_mutuality.containsKey(a))
            _mutuality[a] = hashMapOf()
        if (!_mutuality[a]!!.containsKey(b))
            _mutuality[a]!![b] = 50.0
        return _mutuality[a]!![b]!!
    }

    fun setMutuality(a: String, b: String, delta: Double)
    {
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Setting mutuality $a -> $b invalid.")
        if (!_mutuality.containsKey(a))
            _mutuality[a] = hashMapOf()
        _mutuality[a]!![b] = getMutuality(a, b) + delta
        if (getMutuality(a, b) > 100) _mutuality[a]!![b] = 100.0
        if (getMutuality(a, b) < 0) _mutuality[a]!![b] = 0.0
    }

    fun getPartyMutuality(a: String, b: String): Double
    {
        if (!parties.containsKey(a) || !parties.containsKey(b)) throw Exception("Getting party mutuality $a -> $b invalid.")
        if (!_partyMutuality.containsKey(a))
            _partyMutuality[a] = hashMapOf()
        if (!_partyMutuality[a]!!.containsKey(b))
            _partyMutuality[a]!![b] = 50.0
        return _partyMutuality[a]!![b]!!
    }

    fun setPartyMutuality(a: String, b: String, delta: Double)
    {
        if (!parties.containsKey(a) || !parties.containsKey(b)) throw Exception("Setting party mutuality $a -> $b invalid.")
        if (!_partyMutuality.containsKey(a))
            _partyMutuality[a] = hashMapOf()
        _partyMutuality[a]!![b] = getPartyMutuality(a, b) + delta
        if (getPartyMutuality(a, b) > 100) _partyMutuality[a]!![b] = 100.0
        if (getPartyMutuality(a, b) < 0) _partyMutuality[a]!![b] = 0.0
    }

    //Injects the parent gameState to all elements in the gameState. This function should be called exactly once after the gameState is created.
    private fun injectDependency()
    {
        log.injectParent(this)
        places.forEach { it.value.injectParent(this) }
        characters.forEach { it.value.injectParent(this) }
        parties.forEach { it.value.injectParent(this) }
        nonPlayerAgents.forEach { it.value.injectParent(this) }
        questSystem.injectParent(this)
        eventSystem.injectParent(this)
        println("GameState injected successfully.")
    }


    @OptIn(ExperimentalSerializationApi::class)
    fun dump()
    {

        val prettyJson = Json { // this returns the JsonBuilder
            prettyPrint = true
            allowSpecialFloatingPointValues = true
            // optional: specify indent
            prettyPrintIndent = " "
        }

        val file = File("save${Calendar.getInstance().time.toString("YYYYMMdd_HHmmss")}.json")
        file.writeText(prettyJson.encodeToString(this))
        println("Save File Dumped.")
    }

    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String
    {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }


}