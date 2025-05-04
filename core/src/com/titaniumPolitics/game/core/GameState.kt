package com.titaniumPolitics.game.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
class GameState
{
    private var _time = 0
    private var _idlePop = 0

    var idlePop: Int
        get() = _idlePop
        set(value)
        {
            _idlePop = value
            characters["Anon-idle"]!!.reliant = value
        }
    val laborValuePerHour
        get() =
            ReadOnly.const("mutualityMax") * 1e-2 * (pop - idlePop) / pop//TODO: must scale with cost of living

    var time: Int
        get() = _time
        set(value)
        {
            val old = _time
            _time = value
            (timeChanged.clone() as ArrayList<(Int, Int) -> Unit>).forEach {
                it(
                    old,
                    _time
                )
            } //Clone the list to prevent concurrent modification.
        }
    val hour: Int
        get() = (_time % ReadOnly.constInt("lengthOfDay") / (ReadOnly.const("lengthOfDay") / 24.0)).toInt()
    val day: Int
        get() = _time / ReadOnly.constInt("lengthOfDay")


    /*Old time is the time before the change. New time is the time after the change.*/
    val timeChanged =
        arrayListOf<(Int, Int) -> Unit>()
    val pop: Int
        get() = places.values.sumOf { it.currentTotalPop }
    val totalAnonPop: Int
        get() = characters.values.filter { it.name.contains("Anon") }.sumOf { it.reliant }
    val pickRandomParty: Party
        get()
        {
            //random party picker
            return parties.values.random()
        }
    val pickRandomCharacter: Character
        get()
        {
            //random party picker
            return characters.values.filter { it.alive }.random()
        }

    val popChanged = arrayListOf<() -> Unit>()

    val updateUI = arrayListOf<(GameState) -> Unit>()

    //This is a list of functions that will be called when the game starts.
    val onStart = arrayListOf<() -> Unit>()
    var _alertLevel = 0
    var places = hashMapOf<String, Place>()
    var characters = hashMapOf<String, Character>()
    var nonPlayerAgents = hashMapOf<String, Agent>()
    var playerName = ""

    val player get() = characters[playerName]!!
    var log = Log()
    var parties = hashMapOf<String, Party>()
    var requests = hashMapOf<String, Request>()

    @Serializable
    private var _mutuality = hashMapOf<String, HashMap<String, Double>>()

    var scheduledMeetings = hashMapOf<String, Meeting>()
    var ongoingMeetings = hashMapOf<String, Meeting>()
    var budget = hashMapOf<String, Double>()//Party name to budget
    var isBudgetProposed = false
    var isBudgetResolved = false
    var informations = hashMapOf<String, Information>()
    var eventSystem = EventSystem()
    val realCharList get() = characters.keys.filter { !it.contains("Anon") && characters[it]!!.alive }
    val existingResourceList get() = places.values.map { it.resources.keys }.flatten().toHashSet()
    val existingGasList get() = places.values.map { it.gasResources.keys }.flatten().toHashSet()
    fun getApparatus(apparatusID: String): Apparatus
    {
        places.values.forEach { it.apparatuses.find { it.ID == apparatusID }?.apply { return this } }
        throw Exception(apparatusID)
    }

    fun getApparatusPlace(apparatusID: String): Place
    {
        return places.values.find { it.apparatuses.any { it.ID == apparatusID } }!!
    }

    fun createIdleAnonAgent()
    {
        val name = "Anon-idle"
        characters[name] =
            Character().apply {
                this.livingBy = Place.publicPlaces.random()
                this.health = 100.0
                this.reliant = idlePop
            } //TODO: anonymous characters get resource from market.
        nonPlayerAgents[name] = AnonAgent().also { it.workPlace = Place.publicPlaces.random() }
    }

    fun initialize()
    {
        println("Initializing game state...")

        places.forEach { it.value.injectParent(this) }
        //Gain party anonymous member size from work place requirements.
        parties.forEach {
            val party = it.value
            party.injectParent(this)
            it.value.places.sumOf { it.apparatuses.sumOf { it.idealWorker } }

            //Create anonymous characters if the party is big enough.
            //TODO: maybe assign more then one anon agent per place.
            party.places.forEach { place ->

                val name = party.name + "-Anon-" + place.name
                characters[name] =
                    Character().apply {
                        //They live by one of their work places.
                        try
                        {

                            this.livingBy = places.filter { it.value.responsibleParty == party.name }.keys.random()

                        } catch (e: Exception)
                        {
                            this.livingBy = Place.publicPlaces.random()
                        }

                        this.health = 100.0
                        this.reliant = place.plannedWorker
                        this.resources = Resources("ration" to 100.0 * this.reliant, "water" to 100.0 * this.reliant)
                    }
                nonPlayerAgents[name] = AnonAgent().also { it.workPlace = place.name }
                //TODO: Give traits to the anonymous characters.
                party.members.add(name)

            }

        }
        createIdleAnonAgent()

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

            //Set Will to 50 for all characters.
            setMutuality(char.key, char.key, 50.0)
        }
        eventSystem.injectParent(this)
        //eventSystem.newGame()
        injectDependency()
        println("Game state initialized successfully.")
    }

    fun getMutuality(a: String, b: String = a): Double
    {
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Getting mutuality $a -> $b invalid.")
        if (!_mutuality.containsKey(a))
            _mutuality[a] = hashMapOf()
        if (!_mutuality[a]!!.containsKey(b))
            _mutuality[a]!![b] = ReadOnly.const("mutualityDefault")
        return _mutuality[a]!![b]!!
    }

    fun setMutuality(a: String, b: String = a, delta: Double)
    {
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Setting mutuality $a -> $b invalid.")
        if (!_mutuality.containsKey(a))
            _mutuality[a] = hashMapOf()
        _mutuality[a]!![b] = getMutuality(a, b) + delta
        if (getMutuality(a, b) > ReadOnly.const("mutualityMax")) _mutuality[a]!![b] =
            ReadOnly.const("mutualityMax")
        if (getMutuality(a, b) < ReadOnly.const("mutualityMin")) _mutuality[a]!![b] =
            ReadOnly.const("mutualityMin")
    }

    fun getPartyMutuality(a: String, b: String = a): Double
    {
        if (!parties.containsKey(a) || !parties.containsKey(b)) throw Exception("Getting party mutuality $a -> $b invalid.")
        var totalMutuality = 0.0
        val count = parties[a]!!.size * parties[b]!!.size

        val membersA = parties[a]?.members ?: emptyList()
        val membersB = parties[b]?.members ?: emptyList()

        for (memberA in membersA)
        {
            for (memberB in membersB)
            {
                try
                {
                    val mutuality = getMutuality(memberA, memberB)
                    totalMutuality += mutuality * parties[a]!!.getMultiplier(memberA) * parties[b]!!.getMultiplier(
                        memberB
                    )
                } catch (e: Exception)
                {
                    // Handle cases where mutuality cannot be retrieved, e.g., one of the members does not exist.
                    throw Exception("Getting party mutuality $memberA -> $memberB invalid.")
                }
            }
        }

        return if (count > 0) totalMutuality / count else 0.0
    }

    fun setPartyMutuality(a: String, b: String = a, delta: Double)
    {
        if (!parties.containsKey(a) || !parties.containsKey(b)) throw Exception("Setting party mutuality $a -> $b invalid.")
        val membersA = parties[a]?.members ?: emptyList()
        val membersB = parties[b]?.members ?: emptyList()
        for (memberA in membersA)
        {
            for (memberB in membersB)
            {
                try
                {
                    setMutuality(memberA, memberB, delta)
                } catch (e: Exception)
                {
                    // Handle cases where mutuality cannot be set, e.g., one of the members does not exist.
                    throw Exception("Setting party mutuality $memberA -> $memberB invalid.")
                }
            }
        }
    }

    fun publicity(infoKey: String, party: String): Int
    { //Number of people knowing this info in the party, based on anonymous people

        with(informations[infoKey]!!)
        {
            return knownTo.filter { it in parties[party]!!.members }.sumOf {
                parties[party]!!.getMultiplier(it)
            }
        }
    }

    //Injects the parent gameState to all elements in the gameState. This function should be called exactly once after the gameState is created.
    fun injectDependency()
    {
        log.injectParent(this)
        places.forEach { it.value.injectParent(this) }
        characters.forEach { it.value.injectParent(this) }
        parties.forEach { it.value.injectParent(this) }
        nonPlayerAgents.forEach { it.value.injectParent(this) }
        eventSystem.injectParent(this)
        println("GameState injected successfully.")
    }


    fun dump(): String
    {
        val fName = "save${Calendar.getInstance().time.toString("YYYYMMdd_HHmmss")}_${System.currentTimeMillis()}.json"
        dump(fName)
        return fName
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun dump(fileName: String)
    {
        val prettyJson = Json { // this returns the JsonBuilder
            prettyPrint = true
            allowSpecialFloatingPointValues = true
            // optional: specify indent
            prettyPrintIndent = " "
        }

        val file = File(fileName)
        file.writeText(prettyJson.encodeToString(this))
        println("Save File Dumped.")
    }

    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String
    {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun formatTime(): String
    {
        val mm =
            ((time % ReadOnly.constInt("lengthOfDay") - hour * (ReadOnly.const("lengthOfDay") / 24.0)) / (ReadOnly.const(
                "lengthOfDay"
            ) / (24.0 * 60))).toInt()
        return "${hour.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}"
    }


}