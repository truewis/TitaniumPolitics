package com.titaniumPolitics.game.core

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
    private var _idlePop = 0

    var idlePop: Int
        get() = _idlePop
        set(value) {
            _idlePop = value
            characters["Anon-idle"]!!.reliant = value
        }
    val laborValuePerHour
        get() =
            ReadOnly.const("mutualityMax") * 1e-2 * (pop - idlePop) / pop//TODO: must scale with cost of living

    var time: Int
        get() = _time
        set(value) {
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
        get() = ReadOnly.toHours(_time)
    val day: Int
        get() = ReadOnly.toDays(_time)


    @Transient
            /*Old time is the time before the change. New time is the time after the change.*/
    val timeChanged =
        arrayListOf<(Int, Int) -> Unit>()

    @Transient
    val onPlayerAction = arrayListOf<() -> Unit>() //This is called when the player ends their turn.
    val pop: Int
        get() = places.values.sumOf { it.currentTotalPop }
    val totalAnonPop: Int
        get() = characters.values.filter { it.name.contains("Anon") }.sumOf { it.reliant }
    val pickRandomParty: Party
        get() {
            //random party picker
            return parties.values.random()
        }
    val pickRandomCharacter: Character
        get() {
            //random party picker
            return characters.values.filter { it.alive }.random()
        }

    @Transient
    val popChanged = arrayListOf<() -> Unit>()

    @Transient
    val updateUI = arrayListOf<(GameState) -> Unit>()

    //This is a list of functions that will be called when the game starts.
    @Transient
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

    private var _scheduledMeetings = hashMapOf<String, Meeting>()
    val scheduledMeetings: Map<String, Meeting> = Collections.unmodifiableMap(_scheduledMeetings)

    @Transient
    val onAddScheduledMeeting: ArrayList<(Meeting) -> Unit> = arrayListOf()
    fun addScheduledMeeting(
        meeting: Meeting
    ) {
        if (_scheduledMeetings.containsValue(meeting)) throw Exception("Scheduled meeting $meeting already exists.")
        _scheduledMeetings["conference-${meeting.place}-${meeting.time}"] = meeting
        onAddScheduledMeeting.forEach { it(meeting) }
    }

    fun removeScheduledMeeting(
        key: String
    ) {
        if (!_scheduledMeetings.containsKey(key)) throw Exception("Scheduled meeting with key $key does not exist.")
        _scheduledMeetings.remove(key)
    }

    private var _ongoingMeetings = hashMapOf<String, Meeting>()
    val ongoingMeetings: Map<String, Meeting> = Collections.unmodifiableMap(_ongoingMeetings)

    @Transient
    val onAddOngoingMeeting: ArrayList<(Meeting) -> Unit> = arrayListOf()
    fun addOngoingMeeting(
        meeting: Meeting
    ) {
        if (_ongoingMeetings.containsValue(meeting)) throw Exception("Ongoing meeting $meeting already exists.")
        _ongoingMeetings["conference-${meeting.place}-${meeting.time}"] = meeting
        onAddOngoingMeeting.forEach { it(meeting) }
    }

    fun removeOngoingMeeting(
        key: String
    ) {
        if (!_ongoingMeetings.containsKey(key)) throw Exception("Ongoing meeting with key $key does not exist.")
        _ongoingMeetings.remove(key)
    }

    var budget = hashMapOf<String, Double>()//Party name to budget
    var isBudgetProposed = false
    var isBudgetResolved = false
    private var _informations = hashMapOf<String, Information>()
    val informations: Map<String, Information> = Collections.unmodifiableMap<String, Information>(_informations)

    @Transient
    val onAddInfo: ArrayList<(Information) -> Unit> = arrayListOf()
    fun addInformation(
        info: Information
    ) {
        if (_informations.containsValue(info)) throw Exception("Information $info already exists.")
        _informations[info.generateName()] = info
        onAddInfo.forEach { it(info) }
    }

    fun removeInformation(
        key: String
    ) {
        if (!_informations.containsKey(key)) throw Exception("Information with key $key does not exist.")
        _informations.remove(key)
    }

    var eventSystem = EventSystem()
    val realCharList get() = characters.keys.filter { !it.contains("Anon") && characters[it]!!.alive }
    val existingResourceList get() = places.values.map { it.resources.keys }.flatten().toHashSet()
    val existingGasList get() = places.values.map { it.gasResources.keys }.flatten().toHashSet()
    fun getApparatus(apparatusID: String): Apparatus {
        places.values.forEach { it.apparatuses.find { it.ID == apparatusID }?.apply { return this } }
        throw Exception(apparatusID)
    }

    fun getApparatusPlace(apparatusID: String): Place {
        return places.values.find { it.apparatuses.any { it.ID == apparatusID } }!!
    }

    fun createIdleAnonAgent() {
        val name = "Anon-idle"
        characters[name] =
            Character().apply {
                this.injectParent(this@GameState)
                this.livingBy = Place.publicPlaces.random()
                this.health = 100.0
                this.reliant = idlePop
            } //TODO: anonymous characters get resource from market.
        nonPlayerAgents[name] = AnonAgent().also {
            it.injectParent(this@GameState)
            it.workPlace = Place.publicPlaces.random()
        }
    }

    fun initialize() {
        println("Initializing game state...")
        injectDependency()
        //Gain party anonymous member size from work place requirements.
        parties.forEach {
            val party = it.value
            it.value.places.sumOf { it.apparatuses.sumOf { it.idealWorker } }

            //Create anonymous characters if the party is big enough.
            //TODO: maybe assign more then one anon agent per place.
            party.places.forEach { place ->

                val name = party.name + "-Anon-" + place.name
                characters[name] =
                    Character().apply {
                        //They live by one of their work places.
                        this.injectParent(this@GameState)
                        try {

                            this.livingBy = places.filter { it.value.responsibleDivision == party.name }.keys.random()

                        } catch (e: Exception) {
                            this.livingBy = Place.publicPlaces.random()
                        }

                        this.health = 100.0
                        this.reliant = place.plannedWorker
                    }
                nonPlayerAgents[name] = AnonAgent().also {
                    it.injectParent(this@GameState)
                    it.workPlace = place.name
                }
                //TODO: Give traits to the anonymous characters.
                party.members.add(name)

            }

        }
        createIdleAnonAgent()

        characters.forEach { char ->
            //Create home for each character.
            places["home_" + char.key] = Place().apply {
                this.injectParent(this@GameState)
                responsibleDivision = ""
                //Connect the new home to the place specified in the character.
                val liveBy = this@GameState.characters[char.key]!!.livingBy
                connectedPlaces.add(liveBy)
                coordinates = this@GameState.places[liveBy]!!.coordinates
            }
            places[this@GameState.characters[char.key]!!.livingBy]!!.connectedPlaces.add("home_" + char.key)
            if (places.none { it.value.characters.contains(char.key) })
                places["home_" + char.key]!!.characters.add(char.key)

            //Set Will to 50 for all characters.
            setMutuality(char.key, char.key, 50.0)

            char.value.resources =
                Resources("ration" to 100.0 * char.value.reliant, "water" to 100.0 * char.value.reliant)
        }
        randomize()
        eventSystem.newGame()
        println("Game state initialized successfully.")
    }

    fun randomize() {
        //randomize all mutualities by a certain range.
        characters.keys.forEach { a ->
            characters.keys.forEach { b ->
                if (a != b) {
                    setMutuality(a, b, (Math.random() * 50 - 25))
                }
            }
        }
        val randomTraits = listOf("gourmand", "old", "young", "psychopath", "charismatic", "shy")
        // Assign random traits to characters
        characters.forEach { (_, character) ->
            if (Math.random() < 0.2) // 20% chance to get a random trait
            {
                val trait = randomTraits.random()
                character.trait.add(trait)
            }
        }
    }

    fun getMutuality(a: String, b: String = a): Double {
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Getting mutuality $a -> $b invalid.")
        if (!_mutuality.containsKey(a))
            _mutuality[a] = hashMapOf()
        if (!_mutuality[a]!!.containsKey(b))
            _mutuality[a]!![b] = ReadOnly.const("mutualityDefault")
        return _mutuality[a]!![b]!!
    }

    fun setMutuality(a: String, b: String = a, delta: Double) {
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Setting mutuality $a -> $b invalid.")
        if (!_mutuality.containsKey(a))
            _mutuality[a] = hashMapOf()
        _mutuality[a]!![b] = getMutuality(a, b) + delta
        if (getMutuality(a, b) > ReadOnly.const("mutualityMax")) _mutuality[a]!![b] =
            ReadOnly.const("mutualityMax")
        if (getMutuality(a, b) < ReadOnly.const("mutualityMin")) _mutuality[a]!![b] =
            ReadOnly.const("mutualityMin")
    }

    fun getPartyMutuality(a: String, b: String = a): Double {
        if (!parties.containsKey(a) || !parties.containsKey(b)) throw Exception("Getting party mutuality $a -> $b invalid.")
        var totalMutuality = 0.0
        val count = parties[a]!!.size * parties[b]!!.size

        val membersA = parties[a]?.members ?: emptyList()
        val membersB = parties[b]?.members ?: emptyList()

        for (memberA in membersA) {
            for (memberB in membersB) {
                try {
                    val mutuality = getMutuality(memberA, memberB)
                    totalMutuality += mutuality * parties[a]!!.getMultiplier(memberA) * parties[b]!!.getMultiplier(
                        memberB
                    )
                } catch (e: Exception) {
                    // Handle cases where mutuality cannot be retrieved, e.g., one of the members does not exist.
                    throw Exception("Getting party mutuality $memberA -> $memberB invalid.")
                }
            }
        }

        return if (count > 0) totalMutuality / count else 0.0
    }

    fun setPartyMutuality(a: String, b: String = a, delta: Double) {
        if (!parties.containsKey(a) || !parties.containsKey(b)) throw Exception("Setting party mutuality $a -> $b invalid.")
        val membersA = parties[a]?.members ?: emptyList()
        val membersB = parties[b]?.members ?: emptyList()
        for (memberA in membersA) {
            for (memberB in membersB) {
                try {
                    setMutuality(memberA, memberB, delta)
                } catch (e: Exception) {
                    // Handle cases where mutuality cannot be set, e.g., one of the members does not exist.
                    throw Exception("Setting party mutuality $memberA -> $memberB invalid.")
                }
            }
        }
    }

    fun publicity(
        infoKey: String,
        party: String
    ): Int { //Number of people knowing this info in the party, based on anonymous people

        with(informations[infoKey]!!)
        {
            return knownTo.filter { it in parties[party]!!.members }.sumOf {
                parties[party]!!.getMultiplier(it)
            }
        }
    }

    //Injects the parent gameState to all elements in the gameState. This function should be called exactly once after the gameState is created.
    fun injectDependency() {
        log.injectParent(this)
        places.forEach { it.value.injectParent(this) }
        characters.forEach { it.value.injectParent(this) }
        parties.forEach { it.value.injectParent(this) }
        nonPlayerAgents.forEach { it.value.injectParent(this) }
        eventSystem.injectParent(this)
        println("GameState injected successfully.")
    }


    fun dump(): String {
        val fName = "save${Calendar.getInstance().time.toString("YYYYMMdd_HHmmss")}_${System.currentTimeMillis()}.json"
        dump(fName)
        return fName
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun dump(fileName: String) {
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

    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun formatTime(): String {
        return formatTime(time)
    }

    fun formatClock(): String {
        return formatClock(time)
    }

    companion object {
        fun formatTime(time: Int): String {
            val hour = (time % ReadOnly.constInt("lengthOfDay") / (ReadOnly.const("lengthOfDay") / 24.0)).toInt()
            val day = time / ReadOnly.constInt("lengthOfDay")
            val mm =
                ((time % ReadOnly.constInt("lengthOfDay") - hour * (ReadOnly.const("lengthOfDay") / 24.0)) / (ReadOnly.const(
                    "lengthOfDay"
                ) / (24.0 * 60))).toInt()
            return "$day:${hour.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}"
        }

        fun formatClock(time: Int): String {
            val hour = (time % ReadOnly.constInt("lengthOfDay") / (ReadOnly.const("lengthOfDay") / 24.0)).toInt()
            time / ReadOnly.constInt("lengthOfDay")
            val mm =
                ((time % ReadOnly.constInt("lengthOfDay") - hour * (ReadOnly.const("lengthOfDay") / 24.0)) / (ReadOnly.const(
                    "lengthOfDay"
                ) / (24.0 * 60))).toInt()
            return "${hour.toString().padStart(2, '0')}:${mm.toString().padStart(2, '0')}"
        }
    }


}