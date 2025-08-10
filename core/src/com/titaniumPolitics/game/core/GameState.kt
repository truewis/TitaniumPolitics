package com.titaniumPolitics.game.core

import com.badlogic.gdx.math.MathUtils.clamp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

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
    val timeInDay
        get() = _time % ReadOnly.constInt("lengthOfDay") //Time in the current day.
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

    val aliveCharacters get() = characters.filter { it.value.alive }

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

    fun meetingName(mt: Meeting): String {
        return ongoingMeetings.filter { it.value == mt }.keys.firstOrNull()
            ?: scheduledMeetings.filter { it.value == mt }.keys.firstOrNull()
            ?: throw Exception("Meeting $mt not found in ongoing or scheduled meetings.")
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

    //This function must be called after adding all knownTo characters, because it triggers the onAddInfo events.
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

    fun getWorkplace(character: String): Place? {
        //Returns the workplace of the character, if it exists.
        return places.values.find { character in (it.workplaceParty?.members ?: return@find false) }
    }

    fun initialize() {
        println("Initializing game state...")
        injectDependency()

        //Create workplace party for each workplace.
        places.forEach { place ->
            parties["workplace_${place.key}"] = Party().apply {
                injectParent(this@GameState)
                leader = place.value.manager
                leader?.let { members.add(it) }
                type = "workplace"
                home = place.key
            }

        }


        //Generate lower level managers for each workplace.
        parties.filter { it.value.type == "workplace" }.forEach { party ->
            listOf("administrator", "overseer", "logistician").forEach {
                val name = "${it}_${party.key}"
                characters[name] = Character().apply {
                    this.injectParent(this@GameState)
                    this.livingBy = Place.publicPlaces.random()

                    this.health = 100.0
                }
                nonPlayerAgents[name] = NonPlayerAgent().also {
                    it.injectParent(this)
                }
                places[party.value.home]?.responsibleDivision?.let { div ->
                    parties[div]!!.members += name//Add the lower level manager to the division party. These people have two parties at least.
                }
                when (it) {
                    "administrator" -> {
                        party.value.administrator = name
                    }

                    "overseer" -> {
                        party.value.overseer = name
                    }

                    "logistician" -> {
                        party.value.treasurer = name
                    }
                }
            }


        }
        places.forEach { place ->
            parties["workplace_${place.key}"]?.apply {
                members.addAll(characters.keys.filter { it.contains("workplace_${place.key}") }
                    .toHashSet() //Add all characters that are lower level managers of this workplace.
                )
            }

        }


        //Gain division anonymous member size from work place requirements.
        parties.forEach {
            if (it.value.type != "division") return@forEach //
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
                party.members.add(name)

            }

        }
        createIdleAnonAgent()

        characters.forEach { char ->
            //Create home for each character.
            places["home_" + char.key] = Place().apply {
                this.injectParent(this@GameState)
                responsibleDivision = null //Homes are not responsible for any division.
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
        characters.forEach {
            it.value.randomizeTraitAndStats()
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

    //Return value [-1, 1].
    fun getMutNorm(a: String?, b: String? = a) = if (a == null || b == null) .0 else normMut(getMutuality(a, b))

    private fun normMut(mutuality: Double) =
        (2 * mutuality - (ReadOnly.const("mutualityMax") + ReadOnly.const("mutualityMin"))) / (ReadOnly.const("mutualityMax") - ReadOnly.const(
            "mutualityMin"
        ))

    fun setMutuality(a: String, b: String = a, delta: Double) {
        if (delta.absoluteValue > 50f) throw Exception("Setting mutuality $a -> $b with delta $delta is too high. Use smaller values.")
        if (!delta.isFinite()) throw Exception("Setting mutuality $a -> $b with delta $delta is not finite.")
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Setting mutuality $a -> $b invalid.")
        if (!_mutuality.containsKey(a))
            _mutuality[a] = hashMapOf()
        _mutuality[a]!![b] = getMutuality(a, b) + delta
        if (getMutuality(a, b) > ReadOnly.const("mutualityMax")) _mutuality[a]!![b] =
            ReadOnly.const("mutualityMax")
        if (getMutuality(a, b) < ReadOnly.const("mutualityMin")) _mutuality[a]!![b] =
            ReadOnly.const("mutualityMin")
    }

    fun setMutuality(a: Collection<String>, b: Collection<String> = a, delta: Double) {
        a.forEach { a1 ->
            b.forEach { b1 ->
                setMutuality(a1, b1, delta)
            }
        }
    }

    //Return value [-1, 1].
    fun getPartyMutNorm(a: String?, b: String? = a) =
        if (a == null || b == null) .0 else normMut(getPartyMutuality(a, b))

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
                    throw Exception("Getting mutuality $memberA -> $memberB invalid.")
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
                if (memberA == memberB) continue //Skip self-mutuality.
                setMutuality(memberA, memberB, delta)
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

    fun formatDate(): String {
        val year = day / 90 + 27 // Assuming 90 days per year
        val month = (day % 90) / 30 // Assuming 15 days per month
        return "Megaros $year. ${month + 1}. ${day % 15 + 1}"
    }

    fun formatClock(): String {
        return formatClock(time)
    }


    //Market price per Kg, units of mutuality
    fun getMarketPrice(item: String): Double {

        val totalMarketSupplyEstimateWeekly = places.values.sumOf { it.marketSupplyEstimateWeekly[item] }

        val totalMarketBaseDemandEstimateWeekly =
            pop / 1000.0 * (ReadOnly.resJson[item]?.jsonObject?.get("demandPopElasticity(g/day/person)")?.jsonPrimitive?.double
                ?: .0) //This is base demand before elasticity is applied.

        var elasticityModifier = clamp(
            1 + (totalMarketBaseDemandEstimateWeekly / totalMarketSupplyEstimateWeekly - 1) / (ReadOnly.resJson[item]?.jsonObject?.get(
                "demandPriceElasticity"
            )?.jsonPrimitive?.double
                ?: 1.0), 0.3, 3.0
        )//Clamp the elasticity modifier to prevent extreme values and infinite values.

        if (elasticityModifier.isNaN())
            elasticityModifier = 3.0 // If elasticityModifier is NaN, set it to 3.0 to avoid issues.


        return (ReadOnly.resJson[item]?.jsonObject?.get("baseEGP(g/g)")?.jsonPrimitive?.double
            ?: .0) * 1000 /* Convert to Kg */ *
                (ReadOnly.const("mutualityMax") * 1e-3 /*1000 egP = 100 mutuality*/) * elasticityModifier
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