package com.titaniumPolitics.game.core

import com.badlogic.gdx.math.MathUtils.clamp
import com.titaniumPolitics.game.core.Party.Role
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.ReadOnly.constInt
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.debugTools.Logger
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
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Serializable
class GameState {
    var workingDirectory = ""

    @Transient
    lateinit var gdh:
        GameDataHandler
    private var _time = 0
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

    @Transient
    val onMeetingAction =
        arrayListOf<(GameAction) -> Unit>() //Called for every action performed by a character who is currently in an ongoing meeting.
    val pop: Int
        get() = places.values.sumOf { it.currentTotalPop }
    val totalAnonPop: Int
        get() = characters.values.filter { it.type == Character.Type.ANON }.sumOf { it.reliant }
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
    val pickRandomParty: Party
        get() {
            //random party picker
            return parties.values.random()
        }

    /**
     * Randomly pick a real character that is alive.
     */
    val pickRandomRealCharacter: Character
        get() {
            //random party picker
            return activeCharacters.values.filter { it.type != Character.Type.ANON }.random()
        }

    @Transient
    val popChanged = arrayListOf<() -> Unit>()

    @Transient
    val updateUI = arrayListOf<(GameState) -> Unit>()

    //This is a list of functions that will be called when the game starts.
    @Transient
    val onStart = arrayListOf<() -> Unit>()
    @Transient
    val missedMeetingAlerts = arrayListOf<String>()
    var _alertLevel = 0
    var places = hashMapOf<String, Place>()
    val publicPlaces get() = places.filter { it.value.whoseHome == null }
    var characters = hashMapOf<String, Character>()
    var generatedCharacterNames = hashMapOf<String, String>()
    private var characterIndexCache =
        hashMapOf<String, Int>() //Cache for character indices to speed up mutuality calculations.

    val activeCharacters get() = characters.filter { it.value.alive && it.value.health > 0.0 }

    /*
    Characters that are alive but unconscious (health <= 0).
    Unconscious characters have chances to die each time unit until they are healed.
    They cannot perform normal actions, and need to be rescued.
     */
    val unconsciousCharacters get() = characters.filter { it.value.alive && it.value.health <= 0.0 }
    var nonPlayerAgents = hashMapOf<String, Agent>()
    var playerName = ""

    /**
     * Characters that the player knows about, including themselves.
     * This is used to filter information on UI.
     * Never intended to be used in core logic.
     */
    val knownCharactersToPlayer = hashSetOf<String>() //

    /**
     * Set of place names known to the player.
     * Most places are initially unknown. Adjacent places are revealed when the player enters a place.
     */
    val knownPlacesToPlayer = hashSetOf<String>()

    /**
     * Set of unlocked progression identifiers for the player.
     * Controls which Actions, Agendas, and Requests are available.
     * Grows as the player completes questlines.
     */
    var progression = hashSetOf<String>()
    @Transient
    var debugDisableProgressionCheck = false

    val player get() = characters[playerName]!!
    var log = Log()
    val parties = hashMapOf<String, Party>()
    val requests = hashMapOf<String, Request>()
    fun removeRequest(key: String) {
        if (!requests.containsKey(key)) throw Exception("Request with key $key does not exist.")
        requests.remove(key)
        characters.forEach {
            it.value.executedRequests.remove(key)
        }
    }

    private var _scheduledMeetings = hashMapOf<String, Meeting>()
    val scheduledMeetings: Map<String, Meeting> = Collections.unmodifiableMap(_scheduledMeetings)

    @Transient
    val onAddScheduledMeeting: ArrayList<(Meeting) -> Unit> = arrayListOf()
    fun addScheduledMeeting(
        meeting: Meeting
    ) {
        if (_scheduledMeetings.containsValue(meeting)) throw Exception("Scheduled meeting $meeting already exists.")
        if (meeting.involvedParty != null)
            if (_scheduledMeetings.any {
                    it.value.involvedParty == meeting.involvedParty && abs(
                        it.value.time - meeting.time
                    ) < IDTH
                }) {
                throw Exception("Scheduled meeting ${meeting.type} with party ${meeting.involvedParty} at time ${meeting.time} conflicts with existing meeting.")
            }
        _scheduledMeetings[meeting.ID] = meeting
        onAddScheduledMeeting.forEach { it(meeting) }
    }

    fun removeScheduledMeeting(
        key: String
    ) {
        if (!_scheduledMeetings.containsKey(key)) throw Exception("Scheduled meeting with key $key does not exist.")
        _scheduledMeetings.remove(key)
    }

    fun meetingName(mt: Meeting): String {
        return mt.ID
    }

    private var _ongoingMeetings = hashMapOf<String, Meeting>()
    val ongoingMeetings: Map<String, Meeting> = Collections.unmodifiableMap(_ongoingMeetings)

    @Transient
    val onAddOngoingMeeting: ArrayList<(Meeting) -> Unit> = arrayListOf()
    fun addOngoingMeeting(
        meeting: Meeting
    ) {
        if (_ongoingMeetings.containsValue(meeting)) throw Exception("Ongoing meeting $meeting already exists.")
        _ongoingMeetings[meeting.ID] = meeting
        onAddOngoingMeeting.forEach { it(meeting) }
    }

    fun removeOngoingMeeting(
        key: String
    ) {
        if (!_ongoingMeetings.containsKey(key)) throw Exception("Ongoing meeting with key $key does not exist.")
        _ongoingMeetings.remove(key)
    }

    var budget = hashMapOf<String, Double>()//Party name to budget
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
        requests.values.forEach {
            it.onNewInfo(info)
        }
        onAddInfo.forEach { it(info) }
    }

    fun removeInformation(
        key: String
    ) {
        if (!_informations.containsKey(key)) throw Exception("Information with key $key does not exist.")
        _informations.remove(key)
        //Remove information from requests' proofOfExecutionInfos
        requests.values.forEach {
            it.proofOfExecutionInfos.remove(key)
        }
        //Remove information from places' accidentInformationKeys
        places.values.forEach {
            it.accidentInformationKeys.remove(key)
        }
    }

    var eventSystem = EventSystem()
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
                injectParent(this@GameState)
                type = Character.Type.ANON
                livingBy = Place.publicPlaces.random()
                health = 100.0
                reliant = idlePop
            }
        nonPlayerAgents[name] = NonPlayerAgent().also {
            it.injectParent(this@GameState)
        }
    }

    fun getWorkplace(character: String): Place? {
        if (character == "ctrler")
            return places["mainControlRoom"]
        if (character == "observer")
            return places["observatory"]
        //Returns the workplace of the character, if it exists.
        return places.values.find { character in (it.workplaceParty?.members ?: return@find false) }
    }

    /**
     * This function is only called once when new game starts. Not called when loading existing games.
     * */
    fun initialize() {
        Logger.write("Initializing game state...", Logger.LogLevel.INFO)
        Logger.writer.flush()
        injectDependency()

        //Create NonPlayerAgents for predefined characters.
        characters.forEach { char ->
            if (char.key == playerName || char.value.type == Character.Type.ANON) return@forEach //Skip player character and anon characters.
            nonPlayerAgents[char.key] = NonPlayerAgent().also {
                it.injectParent(this)
            }
        }

        //Create workplace party for each workplace.
        places.forEach { place ->
            //If a party already exists for the workplace, skip it. This is for loading existing games, where workplaces and parties are already created.
            if (!parties.containsKey("workplace_${place.key}")) {
                parties["workplace_${place.key}"] = Party().apply {
                    injectParent(this@GameState)
                    place.value.responsibleDivision?.run {
                        if (place.key.contains("Headquarters")) {
                            val division = parties[place.value.responsibleDivision]!!
                            addMember(division.leader!!, Role.NONE)
                            changeLeader(division.leader!!)
                        } else {
                            val randomDirector =
                                (parties[this]!!.directorMembers - playerName).random()//Player is not a director of any workplace when starting a new game. Usually they must be assigned by events.
                            addMember(randomDirector, Role.NONE)
                            changeLeader(randomDirector)
                        }
                    }
                    type = Party.Type.WORKPLACE
                    home = place.key
                }
            }
        }


        //Generate lower level managers for each workplace.
        parties.filter { it.value.type == Party.Type.WORKPLACE }.forEach { party ->
            listOf(Role.ADMINISTRATOR, Role.TREASURER, Role.OVERSEER).forEach { role ->
                // If the role is already filled, skip it. This is for loading existing games, where some lower level managers may already be created.
                if (party.value.members.none { party.value.getRole(it) == role }) {
                    val name = CharacterGenerator.generateName(listOf(true, false).random())
                    generatedCharacterNames[name] = ReadOnly.charProp(name)
                    characters[name] = Character().apply {
                        this.injectParent(this@GameState)
                        type = Character.Type.EMPLOYEE
                        this.livingBy = Place.publicPlaces.random()

                        this.health = 100.0
                    }
                    nonPlayerAgents[name] = NonPlayerAgent().also {
                        it.injectParent(this)
                    }
                    places[party.value.home]?.responsibleDivision?.let { div ->
                        parties[div]!!.addMember(
                            name,
                            Role.NONE
                        )//Add the lower level manager to the division party. These people have two parties at least.
                    }
                    party.value.addMember(name, role)
                }
            }


        }


        //Gain division anonymous member size from work place requirements.
        parties.forEach {
            if (it.value.type != Party.Type.DIVISION) return@forEach //
            val division = it.value

            //Create anonymous characters if the party is big enough.
            //TODO: maybe assign more then one anon agent per place.
            division.divisionPlaces.forEach { place ->

                val name = division.name + "-Anon-" + place.name
                characters[name] =
                    Character().apply {
                        //They live by one of their work places.
                        this.injectParent(this@GameState)
                        type = Character.Type.ANON
                        try {

                            this.livingBy =
                                places.filter { it.value.responsibleDivision == division.name }.keys.random()

                        } catch (e: Exception) {
                            this.livingBy = Place.publicPlaces.random()
                        }

                        this.health = 100.0
                        this.reliant = place.plannedWorker
                    }
                nonPlayerAgents[name] = NonPlayerAgent().also {
                    it.injectParent(this@GameState)
                }
                place.workplaceParty?.addMember(name, Role.NONE)
                division.addMember(name, Role.NONE)

            }

        }
        createIdleAnonAgent()

        characters.forEach { char ->
            //Create home for each character.
            val liveBy = this@GameState.characters[char.key]!!.livingBy
            places["home_" + char.key] = Place().apply {
                this.injectParent(this@GameState)
                responsibleDivision = null //Homes are not responsible for any division.
                authorizedCharacters += (char.key) //Only the character can enter their home.
                //Connect the new home to the place specified in the character.
                isBuildingIn = liveBy
                connectedPlaces.add(liveBy)
                coordinates = this@GameState.places[liveBy]!!.coordinates
                volume =
                    100.0f * (char.value.reliant + 1) //Set a default volume for the home. There are virtual anon agents with 0 reliant, so we add 1 to avoid division by zero.
                gasResources = Resources(
                    "oxygen" to 300.0,
                    "carbonDioxide" to 1.5,
                    "nitrogen" to 900.0
                ) * (char.value.reliant).toDouble()
            }
            places[liveBy]!!.connectedPlaces.add("home_" + char.key)
            if (places.none { it.value.characters.contains(char.key) })
                places["home_" + char.key]!!.characters.add(char.key)

            char.value.resources["ration"] = 10.0 * char.value.reliant
            char.value.resources["water"] = 10.0 * char.value.reliant
        }

        //After all characters are created, create mutuality matrix.
        _mutuality = Array(characters.size) { DoubleArray(characters.size) { ReadOnly.const("mutualityDefault") } }
        _mutualityReasons =
            Array(characters.size) { ArrayList(characters.map { "" }) }
        characters.keys.forEachIndexed { index, name ->
            characterIndexCache[name] = index //Cache the index of the character for faster access.
        }
        characters.forEach {
            if (it.value.type == Character.Type.DIRECTOR || (it.value.type == Character.Type.EMPLOYEE &&
                    it.value.division == player.division) //Add all directors and above. Only add employees that are in the same division as the player.
            )
                knownCharactersToPlayer += it.key //Add characters to the known characters of the player.
        }
        createCorridors()
        initializeKnownPlaces()
        randomize()
        addFactions()
        eventSystem.newGame()
        Logger.write("Game state initialized successfully.", Logger.LogLevel.INFO)
    }

    /**
     * Reveal a place and all of its adjacent places to the player.
     */
    fun discoverPlacesAdjacentTo(placeName: String) {
        knownPlacesToPlayer.add(placeName)
        places[placeName]?.connectedPlaces?.forEach { knownPlacesToPlayer.add(it) }
    }

    /**
     * Initialize the set of places known to the player at game start.
     * The three outer barriers and techSchool are known. Their adjacent places are also revealed.
     */
    fun initializeKnownPlaces() {
        listOf("outerBarrierEast", "outerBarrierWest", "outerBarrierCenter", "techSchool").forEach {
            discoverPlacesAdjacentTo(it)
        }
        // Also reveal neighbors of the player's starting place.
        discoverPlacesAdjacentTo(player.place.name)
    }

    fun createCorridors() {
        //Create corridors for each connection between places.
        val corridorInitialDurability = 95.0
        val corridorVolumeBase = 20000f
        val processedConnections = mutableSetOf<Pair<String, String>>()

        // Pre-compute connectivity score for each non-corridor place (for manway tier selection).
        val connectionCount = hashMapOf<String, Int>()
        places.keys.filter { !it.contains("corridor") }.forEach { name ->
            connectionCount[name] = places[name]!!.connectedPlaces.count { !it.contains("corridor") }
        }

        // Helper: collect which gas/liquid resources a place's apparatus array produces or needs.
        fun Place.producedGasLiquidResources(): Set<String> {
            val result = mutableSetOf<String>()
            apparatuses.forEach { app ->
                val appData = ReadOnly.appJson[app.name]?.jsonObject ?: return@forEach
                appData["gasGeneration"]?.jsonObject?.keys?.forEach { result.add(it) }
                appData["idealProduction"]?.jsonObject?.keys
                    ?.filter { it in ReadOnly.gasJson.keys || it in Apparatus.LIQUID_RESOURCE_KEYS }
                    ?.forEach { result.add(it) }
            }
            return result
        }

        fun Place.consumedGasLiquidResources(): Set<String> {
            val result = mutableSetOf<String>()
            apparatuses.forEach { app ->
                val appData = ReadOnly.appJson[app.name]?.jsonObject ?: return@forEach
                appData["idealAbsorption"]?.jsonObject?.keys?.forEach { result.add(it) }
                appData["idealConsumption"]?.jsonObject?.keys
                    ?.filter { it in ReadOnly.gasJson.keys || it in Apparatus.LIQUID_RESOURCE_KEYS }
                    ?.forEach { result.add(it) }
            }
            return result
        }

        places.keys.toList().forEach { aName ->
            places[aName]!!.connectedPlaces.toList().forEach { bName ->
                val key = if (aName < bName) aName to bName else bName to aName
                if (key in processedConnections) return@forEach
                //We do not create corridors for connections that already have corridors, to avoid duplication.
                if (aName.contains("corridor") || bName.contains("corridor")) return@forEach
                //If one place is building in another, we do not create corridor between them.
                if (places[aName]!!.isBuildingIn == bName || places[bName]!!.isBuildingIn == aName) return@forEach
                processedConnections.add(key)

                val (sortedA, sortedB) = key
                val aCoords = places[sortedA]!!.coordinates
                val bCoords = places[sortedB]!!.coordinates
                val diff = bCoords - aCoords
                val t1Coords = aCoords + (diff * 2.0) / 10
                val t2Coords = aCoords + (diff * 8.0) / 10
                val t1Name = "corridor_${sortedA}_${sortedB}"
                val t2Name = "corridor_${sortedB}_${sortedA}"

                // ---- Manway tier selection ----
                val isElevatorConnection =
                    (sortedA == "outerBarrierEast" && sortedB == "techSchool") ||
                        (sortedA == "techSchool" && sortedB == "outerBarrierEast")
                if (isElevatorConnection) {
                    // Keep legacy elevator apparatus for this special connection.
                    places[sortedA]!!.apparatuses.add(Apparatus().apply {
                        name = "elevator"; durability = corridorInitialDurability; ID = "elevator_$t1Name"
                    })
                    places[sortedB]!!.apparatuses.add(Apparatus().apply {
                        name = "elevator"; durability = corridorInitialDurability; ID = "elevator_$t2Name"
                    })
                } else {
                    val maxConn = maxOf(
                        connectionCount[sortedA] ?: 1,
                        connectionCount[sortedB] ?: 1
                    )
                    val roll = Math.random()
                    val manwayTier = when {
                        maxConn >= 6 -> when {
                            roll < 0.30 -> "manwayI"
                            roll < 0.85 -> "manwayII"
                            else -> "manwayIII"
                        }

                        maxConn >= 4 -> when {
                            roll < 0.60 -> "manwayI"
                            roll < 0.95 -> "manwayII"
                            else -> "manwayIII"
                        }

                        else -> when {
                            roll < 0.90 -> "manwayI"
                            roll < 0.99 -> "manwayII"
                            else -> "manwayIII"
                        }
                    }
                    places[sortedA]!!.apparatuses.add(Apparatus().apply {
                        name = manwayTier; durability = corridorInitialDurability; ID = "${manwayTier}_$t1Name"
                    })
                    places[sortedB]!!.apparatuses.add(Apparatus().apply {
                        name = manwayTier; durability = corridorInitialDurability; ID = "${manwayTier}_$t2Name"
                    })
                }

                val corridorVolume = corridorVolumeBase
                val volumeRatio = corridorVolume / 1e4f
                val gasRes = {
                    Resources(
                        "oxygen" to 3000.0 * volumeRatio,
                        "carbonDioxide" to 15.0 * volumeRatio,
                        "nitrogen" to 9000.0 * volumeRatio
                    ).apply { positive = true }
                }

                places[t1Name] = Place().apply {
                    this.injectParent(this@GameState)
                    coordinates = t1Coords
                    volume = corridorVolume
                    connectedPlaces.add(sortedA)
                    connectedPlaces.add(t2Name)
                    gasResources = gasRes()
                }
                places[t2Name] = Place().apply {
                    this.injectParent(this@GameState)
                    coordinates = t2Coords
                    volume = corridorVolume
                    connectedPlaces.add(t1Name)
                    connectedPlaces.add(sortedB)
                    gasResources = gasRes()
                }

                places[sortedA]!!.connectedPlaces.remove(sortedB)
                places[sortedA]!!.connectedPlaces.add(t1Name)
                places[sortedB]!!.connectedPlaces.remove(sortedA)
                places[sortedB]!!.connectedPlaces.add(t2Name)

                // ---- Infrastructure seeding for non-elevator corridors ----
                if (!isElevatorConnection) {
                    // Determine chosen manway tier's radius for space budget checks.
                    val manwayApp = places[sortedA]!!.apparatuses
                        .find { it.ID.endsWith("_$t1Name") && it.name in Apparatus.MANWAY_NAMES }
                    val radius = manwayApp?.corridorRadius ?: 2.0
                    val totalArea = kotlin.math.PI * radius * radius

                    // Slope (rise/run) of this connection.
                    val diff3 = bCoords - aCoords
                    val horizontalDist = sqrt(diff3.x * diff3.x + diff3.y * diff3.y)
                    val slope = if (horizontalDist < 0.001) Double.MAX_VALUE
                    else kotlin.math.abs(diff3.z) / horizontalDist

                    var usedSpace = 0.0

                    fun addInfraToCorridors(infraName: String, resourceFilter: String? = null) {
                        val app1 = Apparatus().apply {
                            name = infraName
                            durability = corridorInitialDurability
                            ID = "${infraName}_$t1Name"
                            if (resourceFilter != null) parameters["resourceFilter"] = resourceFilter
                        }
                        val app2 = Apparatus().apply {
                            name = infraName
                            durability = corridorInitialDurability
                            ID = "${infraName}_$t2Name"
                            if (resourceFilter != null) parameters["resourceFilter"] = resourceFilter
                        }
                        places[t1Name]!!.apparatuses.add(app1)
                        places[t2Name]!!.apparatuses.add(app2)
                        usedSpace += app1.spaceConsumption
                    }

                    // Railway: available in manwayII+ if slope is within limit.
                    val railwayMaxSlope = 0.15
                    var hasRailway = false
                    if (radius >= 4.0 && slope <= railwayMaxSlope && usedSpace + 3.0 <= totalArea) {
                        if (Math.random() < 0.40) {
                            addInfraToCorridors("railway")
                            // Railway always comes with a power line for its own energy.
                            if (usedSpace + 0.2 <= totalArea) addInfraToCorridors("powerLineII")
                            hasRailway = true
                        }
                    }

                    // Cart path: in manwayII+ without railway, within slope limit.
                    val cartPathMaxSlope = 0.20
                    if (!hasRailway && radius >= 4.0 && slope <= cartPathMaxSlope && usedSpace + 1.0 <= totalArea) {
                        if (Math.random() < 0.20) addInfraToCorridors("cartPath")
                    }

                    // Power line: if no railway power line already added.
                    if (!hasRailway) {
                        if (radius >= 4.0 && usedSpace + 0.2 <= totalArea && Math.random() < 0.30) {
                            addInfraToCorridors("powerLineI")
                        }
                    }

                    // Pipes: seed if endpoints have matching production/consumption.
                    val aProduced = places[sortedA]!!.producedGasLiquidResources()
                    val bConsumed = places[sortedB]!!.consumedGasLiquidResources()
                    val bProduced = places[sortedB]!!.producedGasLiquidResources()
                    val aConsumed = places[sortedA]!!.consumedGasLiquidResources()

                    val pipeResources = (aProduced intersect bConsumed) union (bProduced intersect aConsumed)
                    pipeResources.forEach { res ->
                        val isGas = res in ReadOnly.gasJson.keys
                        val pipeName = if (isGas) "gasPipeI" else "liquidPipeI"
                        val pipeSpace = if (isGas) 0.2 else 0.2
                        if (usedSpace + pipeSpace <= totalArea && Math.random() < 0.60) {
                            addInfraToCorridors(pipeName, res)
                        }
                    }
                }
            }
        }

        // Generate fake corridors (always manwayI, smallest).
        val fakeCorrIdxCounter = hashMapOf<String, Int>()
        val corridorVolume = corridorVolumeBase
        val volumeRatio = corridorVolume / 1e4f
        places.keys.toList().forEach { sourceName ->
            val r = Math.random()
            val fakeCorrCount = when {
                r < 0.73 -> 0
                r < 0.97 -> 1
                else -> 2
            }
            repeat(fakeCorrCount) {
                val idx = fakeCorrIdxCounter.getOrDefault(sourceName, 0)
                fakeCorrIdxCounter[sourceName] = idx + 1
                val fakeName = "corridor_fake_${sourceName}_$idx"
                val sourceCoords = places[sourceName]!!.coordinates
                val radius = 2 + Math.random() * 3
                val theta = Math.random() * 2 * Math.PI
                val fakeCoords = sourceCoords + Coordinate3D(
                    radius * cos(theta),
                    0.0,
                    radius * sin(theta)
                )
                places[fakeName] = Place().apply {
                    this.injectParent(this@GameState)
                    coordinates = fakeCoords
                    volume = corridorVolume
                    connectedPlaces.add(sourceName)
                    gasResources = Resources(
                        "oxygen" to 3000.0 * volumeRatio,
                        "carbonDioxide" to 15.0 * volumeRatio,
                        "nitrogen" to 9000.0 * volumeRatio
                    ).apply { positive = true }
                }
                places[sourceName]!!.connectedPlaces.add(fakeName)
                places[sourceName]!!.apparatuses.add(Apparatus().apply {
                    name = "manwayI"; durability = corridorInitialDurability; ID = "manwayI_$fakeName"
                })
            }
        }
    }

    fun randomize() {
        //randomize all mutualities by a certain range.
        characters.keys.forEach { a ->
            characters.keys.forEach { b ->
                if (a != b) {
                    setMutuality(a, b, (Math.random() * 30 - 15), "Randomize")
                } else
                    setMutuality(
                        a,
                        b,
                        50.0,
                        "Randomize"
                    ) //Will for self is always initialized at 50 for predictability.
            }
        }
        setHardcodedMutuality()
        characters.forEach {
            it.value.randomizeTraitAndStats()
        }

        //Randomize durabilities.
        places.forEach {
            it.value.apparatuses.forEach {
                it.durability = (Math.random() * 50 + 50)
            }
        }
    }

    fun addFactions() {
        //Add religious factions: atheist, spiritualist, artificialist
        val religion = listOf("atheist", "spiritualist", "artificialist")
        religion.forEach {
            parties[it] = Party().apply {
                injectParent(this@GameState)
                type = Party.Type.OTHER
                home = "market"
                characters.filter { char -> it in char.value.trait }.keys.forEach {
                    addMember(it, Role.NONE)
                }
            }
        }
        //Add qualification factions: engineer, soldier, administrator, miner
        val qualification = listOf("engineer", "soldier", "administrator", "miner")
        qualification.forEach {
            parties[it] = Party().apply {
                injectParent(this@GameState)
                type = Party.Type.QUALIFICATION
                home = "market"
                characters.filter { char -> it in char.value.trait }.keys.forEach {
                    addMember(it, Role.NONE)
                }
            }
        }
    }

    @Serializable
    private var _mutuality = Array(1) { DoubleArray(1) }

    @Transient
    val onNewMutualityReason = arrayListOf<(String, String, Double, String?) -> Unit>()
    fun addMutualityReason(indexA: Int, indexB: Int, newDelta: Double, newReasonKey: String) {
        _mutualityReasons[indexA][indexB] += "$time:$newDelta:$newReasonKey\n"
        onNewMutualityReason.forEach {
            it(
                characters.keys.elementAt(indexA),
                characters.keys.elementAt(indexB),
                newDelta,
                newReasonKey
            )
        }
    }

    private var _mutualityReasons =
        Array(1) { ArrayList<String>() } //Reasons for mutuality changes, indexed by character index.

    fun setHardcodedMutuality() {
        //Set hardcoded mutualities for some characters.
        //Check if the characters exist before setting mutuality, to avoid errors when loading existing games.
        if (!characters.containsKey("Rui") || !characters.containsKey("Yuhoa")) return
        setMutuality("Rui", "Yuhoa", 30.0, "Hardcoded")
        setMutuality("Yuhoa", "Rui", 30.0, "Hardcoded")

        if (!characters.containsKey("Rui") || !characters.containsKey("Alina")) return
        setMutuality("Alina", "Rui", 30.0, "Hardcoded")
        setMutuality("Rui", "Alina", 30.0, "Hardcoded")

        if (!characters.containsKey("Krailin") || !characters.containsKey("Alina")) return
        setMutuality("Alina", "Krailin", -15.0, "Hardcoded")
        setMutuality("Krailin", "Alina", -15.0, "Hardcoded")
        if (!characters.containsKey("Rui") || !characters.containsKey("Vaeme")) return
        setMutuality("Rui", "Vaeme", -15.0, "Hardcoded")

        if (!characters.containsKey("Rui") || !characters.containsKey("Eugene")) return
        setMutuality("Eugene", "Rui", 20.0, "Hardcoded")
        setMutuality("Rui", "Eugene", 20.0, "Hardcoded")
    }

    fun getMutuality(a: String, b: String = a): Double {
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Getting mutuality $a -> $b invalid.")
        val indexA = characterIndexCache[a]!!
        val indexB = characterIndexCache[b]!!
        return _mutuality[indexA][indexB]
    }

    fun getCharToPartyMutuality(char: String, party: String): Double {
        if (!characters.containsKey(char) || !parties.containsKey(party)) throw Exception("Getting mutuality $char -> $party invalid.")
        val members = parties[party]!!.members
        return members.map { getMutuality(char, it) }.average()
    }

    /**Return value [-1, 1].*/
    fun getMutNorm(a: String?, b: String? = a) = if (a == null || b == null) .0 else normMut(getMutuality(a, b))

    private fun normMut(mutuality: Double) =
        (2 * mutuality - (ReadOnly.const("mutualityMax") + ReadOnly.const("mutualityMin"))) / (ReadOnly.const("mutualityMax") - ReadOnly.const(
            "mutualityMin"
        ))

    fun setMutuality(a: String, b: String = a, delta: Double, reasonKey: String? = null) {
        if (abs(delta) < 1e-2) return //No change in mutuality, do nothing.
        if (delta.absoluteValue > 100f) throw Exception("Setting mutuality $a -> $b with delta $delta, $reasonKey is too high. Use smaller values.")
        if (!delta.isFinite()) throw Exception("Setting mutuality $a -> $b with delta $delta is not finite.")
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Setting mutuality $a -> $b invalid.")
        if (a == b && "robot" in characters[a]!!.trait)
            return //Do not change will for robots, they are not affected by will.
        val indexA = characterIndexCache[a]!!
        val indexB = characterIndexCache[b]!!
        _mutuality[indexA][indexB] = getMutuality(a, b) + delta
        if (getMutuality(a, b) > ReadOnly.const("mutualityMax")) _mutuality[indexA][indexB] =
            ReadOnly.const("mutualityMax")
        if (getMutuality(a, b) < ReadOnly.const("mutualityMin")) _mutuality[indexA][indexB] =
            ReadOnly.const("mutualityMin")
        addMutualityReason(indexA, indexB, delta, reasonKey ?: "Unknown")

        //Generate information if the mutuality change was in the meeting.
        //The impression is felt by all characters in the meeting.
        ongoingMeetings.values.find { a in it.currentCharacters }?.let { meeting ->
            Information(
                author = null,
                creationTime = time,
                type = InformationType.MUTUALITY,
                tgtTime = time,
                tgtCharacter = a,
                auxCharacter = b,
                amount = getMutuality(a, b)
                    .toInt() - (0..7).random(),
                variables = hashMapOf(
                    "delta" to delta
                    //TODO: reason
                )
            ).also {
                it.knownTo.addAll(meeting.currentCharacters)
                addInformation(it)
            }
        }

    }

    fun getSignificantMutualityReasons(a: String, b: String): List<Pair<Double, String>> {
        if (!characters.containsKey(a) || !characters.containsKey(b)) throw Exception("Getting mutuality reasons $a -> $b invalid.")
        val indexA = characterIndexCache[a]!!
        val indexB = characterIndexCache[b]!!
        val reason = _mutualityReasons[indexA][indexB].split('\n')//Last element is always empty.
        //Pick three most significant reasons, i.e. those with the highest absolute delta. It should be weighted with time since the delta.
        return reason.take(reason.size - 1).map { it.split(':') }
            //Take sum of deltas with the same reason key.
            .groupBy { it[2]/*reason key*/ }
            .mapValues { it.value.sumOf { (moment, delta) -> delta.toDouble() / (sqrt(time - moment.toInt() + 1.0)) /*Time weighted*/ } }
            .filter { it.value.absoluteValue > 1e-2 } //Filter out insignificant reasons.
            .toList()
            .sortedByDescending { abs(it.second) } //Sort by absolute delta.
            .take(3) //Take three most significant reasons.
            .map { Pair(it.second, it.first) } //Format the reason.
    }

    fun setMutuality(a: Collection<String>, b: Collection<String> = a, delta: Double, reasonKey: String? = null) {
        a.forEach { a1 ->
            b.forEach { b1 ->
                setMutuality(a1, b1, delta, reasonKey)
            }
        }
    }

    /**Return value [-1, 1].*/
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


    /**
    Sets mutuality for all members of the party a to all members of the party b.
    This is weighted by the size of the party a, so that larger parties have less influence on individual mutualities.
     */
    fun setPartyMutuality(a: String, b: String = a, weightedDelta: Double, reasonKey: String? = null) {
        if (!parties.containsKey(a) || !parties.containsKey(b)) throw Exception("Setting party mutuality $a -> $b invalid.")
        val membersA = (parties[a]?.members ?: emptyList()) - (parties[a]?.directorMembers ?: emptyList())
        val membersB = parties[b]?.members ?: emptyList()
        for (memberA in membersA) {
            for (memberB in membersB) {
                if (memberA == memberB) continue //Skip self-mutuality.
                setMutuality(memberA, memberB, weightedDelta / parties[a]!!.size, reasonKey)
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
        gdh = GameDataHandler(workingDirectory)
        gdh.initializeColumns()
        timeChanged += { old, new ->
            if (new % IDTH == 0) {
                gdh.writeEveryTurn(this)
            }
        }
        log.injectParent(this)
        places.forEach { it.value.injectParent(this) }
        characters.forEach { it.value.injectParent(this) }
        generatedCharacterNames.forEach { (id, displayName) ->
            ReadOnly.setCharacterProp(id, displayName)
        }
        parties.forEach { it.value.injectParent(this) }
        nonPlayerAgents.forEach { it.value.injectParent(this) }
        eventSystem.injectParent(this)
        Logger.write("GameState injected successfully.", Logger.LogLevel.INFO)
    }


    fun dump(): String {
        val fName =
            if (workingDirectory != "") (workingDirectory + "/save${time}_${System.currentTimeMillis()}.json") else "save${
                Calendar.getInstance().time.toString("YYYYMMdd_HHmmss")
            }_${time}_${System.currentTimeMillis()}.json"
        dump(fName)
        return fName
    }

    fun dumpTemp(): String {
        if (workingDirectory != "") {
            val fName =
                (workingDirectory + "/lastSave.json")
            dump(fName)
            return fName
        } else
            throw Exception("Cannot dump temp save without working directory.")
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

    fun formatDate(type: String = "full", addTime: Int = 0): String {
        val tmpDay = ReadOnly.toDays(_time + addTime)
        val year = tmpDay / (constInt("quarterInDays") * 4) + 27 // 60 days per year
        val quarter =
            (tmpDay % (constInt("quarterInDays") * 4)) / constInt("quarterInDays") // 15 days per quarter
        val watchmaker = characters.values.find { it.trait.contains("watchmaker") }!!.name
        when (type) {
            "full" -> return "${ReadOnly.charProp(watchmaker)} $year. ${quarter + 1}. ${tmpDay % constInt("quarterInDays") + 1}"
            "year" -> return "${ReadOnly.charProp(watchmaker)} $year."
            "month" -> return "${quarter + 1}"
            "monthDate" -> return "${quarter + 1}. ${tmpDay % 15 + 1}"
            else -> return "${ReadOnly.charProp(watchmaker)} $year. ${quarter + 1}. ${tmpDay % constInt("quarterInDays") + 1}"
        }
    }

    fun formatClock(): String {
        return formatClock(time)
    }


    /**Market price per Kg, units of mutuality*/
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

    fun destroy() {
        //Destroy the game state, clear all data.
        gdh.close()
        Logger.close()
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

    /**
     * Finds the optimal transport route for [resourceKey] from place [from] to place [to].
     *
     * Uses a modified Dijkstra where the edge cost is 1.0 / throughput (lower = better).
     * Falls back to raw manpower throughput on any segment with no infrastructure.
     *
     * If [from] has a warehouse apparatus with assigned workers, its throughput is applied
     * as a floor for every segment that lacks other infrastructure.
     *
     * @return A [TransportRoute] with the best (maximum bottleneck) path, or null if [to] is unreachable.
     */
    fun findOptimalTransportRoute(from: String, to: String, resourceKey: String): TransportRoute? {
        if (from == to) return TransportRoute(emptyList(), Double.MAX_VALUE)

        // Warehouse throughput from the source: applies as a floor on every segment.
        val warehouseThroughput = places[from]?.apparatuses
            ?.filter { it.transportType == "warehouse" && it.durability > 0 }
            ?.sumOf { it.actualThroughput } ?: 0.0

        // Dijkstra — cost = sum of (1/throughput) for each hop. Lower = faster.
        data class State(val place: String, val cost: Double)

        val cost = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
        val prev = mutableMapOf<String, String?>()
        val prevThroughput = mutableMapOf<String, Double>()
        val prevMethod = mutableMapOf<String, String>()
        val visited = mutableSetOf<String>()
        val queue = PriorityQueue<State>(compareBy { it.cost })

        cost[from] = 0.0
        queue.add(State(from, 0.0))

        while (queue.isNotEmpty()) {
            val (current, currentCost) = queue.poll()
            if (current in visited) continue
            visited.add(current)
            if (current == to) break

            val currentPlace = places[current] ?: continue
            for (neighbor in currentPlace.connectedPlaces) {
                if (neighbor in visited) continue
                val neighborPlace = places[neighbor] ?: continue

                // Throughput on this hop = what the corridor/neighbor can move for this resource.
                val segThroughput = neighborPlace.throughputForResource(resourceKey, warehouseThroughput)
                val segCost = if (segThroughput <= 0.0) Double.MAX_VALUE / 2 else 1.0 / segThroughput

                val newCost = if (currentCost >= Double.MAX_VALUE / 2) Double.MAX_VALUE / 2
                else currentCost + segCost

                if (newCost < cost.getValue(neighbor)) {
                    cost[neighbor] = newCost
                    prev[neighbor] = current
                    prevThroughput[neighbor] = segThroughput
                    val bestMethod = neighborPlace.transportInfrastructureForResource(resourceKey)
                        .maxByOrNull { it.actualThroughput }?.name ?: "manual"
                    prevMethod[neighbor] = bestMethod
                    queue.add(State(neighbor, newCost))
                }
            }
        }

        if (cost.getValue(to) >= Double.MAX_VALUE / 2) return null

        // Reconstruct path.
        val segments = mutableListOf<TransportSegment>()
        var current: String? = to
        while (current != null && current != from) {
            val p = prev[current] ?: break
            segments.add(
                0, TransportSegment(
                    fromPlace = p,
                    toPlace = current,
                    methodName = prevMethod[current] ?: "manual",
                    throughput = prevThroughput[current] ?: 1.0
                )
            )
            current = p
        }

        val bottleneck = segments.minOfOrNull { it.throughput } ?: 1.0
        return TransportRoute(segments, bottleneck)
    }

}
