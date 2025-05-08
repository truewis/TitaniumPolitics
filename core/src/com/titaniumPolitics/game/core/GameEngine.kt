package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.dt
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.LogUI
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log
import kotlin.math.min
import kotlin.random.Random
import kotlin.system.exitProcess

/*
* GameEngine is a loop that runs the game. Each loop is a turn. Each turn, each character performs an action.
*
*
* */
class GameEngine(val gameState: GameState)
{

    //Let UI or other monitors to come in and read gamestate.
    var onObserverCall = arrayListOf<(GameState) -> Unit>()

    init
    {
        Logger.gState = gameState
    }

    fun startGame()
    {
        //Start the game.

        onObserverCall.forEach { it(gameState) }
        println("Game started. Time: ${gameState.time}. Starting main loop.")
        //Main loop


        while (true)
        {
            gameLoop()
        }
    }

    fun gameLoop()
    {
        gameState.characters.values.sortedByDescending { if (it == gameState.player) 1 else 0 }.forEach {
            if (it.alive)
            {
                if (it.frozen > 0)
                {
                    it.frozen--
                    if (!it.trait.contains("robot"))
                    {//Robots don't need to eat.
                        it.health -= dt / const("HealthConsumptionTau") * const("HealthMax")
                        it.hunger += dt / const("HungerConsumptionTau") * const("HungerMax")
                        it.thirst += dt / const("ThirstConsumptionTau") * const("ThirstMax")
                        if (it.hunger > const("hungerThreshold")) it.health -= dt / const("HealthConsumptionTau") * const(
                            "HealthMax"
                        )
                        if (it.thirst > const("thirstThreshold")) it.health -= dt / const("HealthConsumptionTau") * const(
                            "HealthMax"
                        )

                    }
                }
                while (it.frozen == 0)
                {
                    performAction(it)
                    //if the action took any amount of time, exit the loop.
                }
            }
        }
        progression()

        if (gameState.time % (const("lengthOfDay") / 24).toInt() == 0)//Every hour
        {
            hourlyProgression()
        }

        if (gameState.time % const("lengthOfDay").toInt() == 0)//Every day
        {
            dailyProgression()
        }
    }

    private fun hourlyProgression()
    {
        distributeResourcesHourly()
        workApparatusesHourly()
        ageInformationHourly()
        spreadPublicInfo()
        checkMarketResourcesHourly(gameState)
    }

    private fun dailyProgression()
    {
        partySizeAdjust()
        scheduleDailyConferences()
    }


    //This function is called at the end of each turn, after all the characters have performed their actions.
    fun progression()
    {
        gameState.time += 1
        println("[${gameState.formatTime()}]")
        gameState.places.forEach {
            it.value.distributeWorkers()
        }

        diffuseGas()
        calculateMutuality()

        conditionCheck()



        gameState.ongoingMeetings.forEach {
            it.value.onTimeChange(gameState)
        }
        gameState.requests.forEach {
            it.value.refresh(gameState)
        }


        //println("Time: ${gameState.time}")
        //println("My approval:${gameState.characters[gameState.playerAgent]!!.approval}")
        onObserverCall.forEach { it(gameState) }

    }

    fun performAction(char: Character)
    {
        var action: GameAction
        val actionList = availableActions(
            this.gameState, gameState.places.values.find { it.characters.contains(char.name) }!!.name,
            char.name
        )
        if (gameState.nonPlayerAgents[char.name] == null && char.name == gameState.playerName)//If the player character agent exists, the game does not wait for player input.
        //This is for automatic progression and test purposes.
        {
            do
            {
                action = acquire("Action", hashMapOf("actionList" to actionList))
                action.injectParent(gameState)
                if (action.isValid())
                    break
                else
                    println(
                        "Invalid action: ${action.javaClass.simpleName} by ${char.name} at ${
                            gameState.places.values.find {
                                it.characters.contains(
                                    char.name
                                )
                            }!!.name
                        }"
                    )
            } while (true)


        } else
        {
            action = gameState.nonPlayerAgents[char.name]?.chooseAction()
                ?: throw Exception("Non player character ${char.name} does not have a nonPlayerAgent.")
            action.injectParent(gameState)
            if (action.javaClass.simpleName !in actionList)
                Logger.warning(
                    "Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.formatTime()}, which is not in the action list. This may be a bug."
                )
            if (!action.isValid())
            {
                Logger.warning(
                    "Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.formatTime()}, which is not valid. This may be a bug."
                )
                println(Json.encodeToString(GameAction.serializer(), action))
                throw Exception("Non player character ${char.name} is performing an invalid action.")
            }
            if (action.sbjCharacter != char.name)
            {
                Logger.warning(
                    "Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.formatTime()}, which is not targeting itself. This may be a bug."
                )
                println(Json.encodeToString(GameAction.serializer(), action))
            }
            if (action.tgtPlace != char.place.name)
            {
                Logger.warning(
                    "Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.formatTime()}, which is not targeting its own place. This may be a bug."
                )
                println(Json.encodeToString(GameAction.serializer(), action))
            }

        }
        char.history.add(action.javaClass.simpleName)
        val place = gameState.places.values.find {
            it.characters.contains(
                char.name
            )
        }!!.name
        //Unless the information is wait and move. I think wait and move info is useless. It adds a lot of overhead, and the info that a character saw someone can be obtained by talking to them instead.
        if (action !is Wait && action !is Move)
        {
            //Add information to the character so that they can report back.
            Information(
                char.name,
                creationTime = gameState.time,
                type = InformationType.ACTION,
                tgtTime = gameState.time,
                tgtPlace = place,
                tgtCharacter = char.name,
                action = action
            ).also {
                it.knownTo.addAll(char.place.characters)//All characters from the same place know about the action.
                gameState.informations[it.generateName()] = it
            }
        }
        action.execute()
        gameState.setMutuality(char.name, delta = action.deltaWill())

    }

    private fun calculateMutuality()
    {
        //Wealth display effect: opportunists mutuality to more wealthy character increases. to less wealthy character decreases.

        //If there are meetings where some characters are missing, all the characters in the meeting lose mutuality toward the missing characters.
        gameState.ongoingMeetings.forEach { meeting ->
            meeting.value.scheduledCharacters.forEach { char ->
                if (!meeting.value.currentCharacters.contains(char))
                    meeting.value.currentCharacters.forEach { char2 ->
                        gameState.setMutuality(
                            char,
                            char2,
                            -dt / const("MutualityReinforcementTau") * ReadOnly.mutualityScale
                        )
                    }
            }
        }
    }

    // information does not affect the approval after some time.
    private fun ageInformationHourly()
    {
        val removed = arrayListOf<String>()
        gameState.informations.forEach {
            it.value.life -= dt
            if (it.value.life <= 0 && it.value.rememberedBy.isEmpty())
                removed.add(it.key)
        }
        removed.forEach { gameState.informations.remove(it) }
    }

    //TODO: optimize this function.
    private fun spreadPublicInfo()
    {
        gameState.parties.forEach { party ->
            //bad news affect the approval. casualty, stolen resource, TODO: low water ration oxygen, high wealth, crimes
            gameState.informations.filter { it.value.type == InformationType.CASUALTY }.forEach {
                var factor = 1.0
                if (it.value.author == "") factor *= 2.0//rumors affect the approval negatively.
                if (it.value.auxParty == party.key) factor *= 2.0//If the casualty is in our party, approval of the responsible party drops even more.
                //If casualty is not localized, does not affect mutualities.
                if (it.value.tgtPlace == "everywhere" || gameState.places[it.value.tgtPlace]!!.responsibleDivision == "")
                {
                    //Do nothing
                } else
                    gameState.setPartyMutuality(
                        party.key,
                        gameState.places[it.value.tgtPlace]!!.responsibleDivision,
                        -it.value.amount * gameState.publicity(
                            it.key,
                            party.key
                        ) / party.value.size * factor * dt / const("MutualityFromInfoTau") * ReadOnly.mutualityScale
                    )
                //if our party is responsible, integrity drops.

            }
            gameState.informations.filter { it.value.type == InformationType.ACTION && it.value.action!!.javaClass.simpleName == "unofficialResourceTransfer" }
                .forEach {
                    var factor = 1
                    if (it.value.author == "") factor = 2//rumors affect the approval negatively.

                    //party loses mutuality toward the responsible party. TODO: consider affecting the individual mutuality toward the perpetrator.
                    //TODO: item value must be put into consideration
                    gameState.setPartyMutuality(
                        party.key, gameState.places[it.value.tgtPlace]!!.responsibleDivision, -log(
                            it.value.amount.toDouble() + 1, 2.0
                        ) * gameState.publicity(
                            it.key,
                            party.key
                        ) / party.value.size * factor * dt / const("MutualityFromInfoTau") * ReadOnly.mutualityScale
                    )
                }
            //The fact that resource is low itself does not affect the mutuality.--------------------------------------------------------------------
            //TODO: Why?
//            gameState.informations.filter { it.value.type == "resources" && it.value.tgtPlace== "everywhere" && it.value.tgtResource in listOf("water", "oxygen", "ration") }
//                .forEach {
//                    var factor = 1
//                    if (it.value.author == "") factor = 2//rumors affect the approval negatively.
//                    var consumption = when(it.value.tgtResource){
//                        "water"->4
//                        "ration"->2
//                        "oxygen"->1
//                        else -> 0
//                    }
//                    if(it.value.amount==0)//If the resource is empty, approval of everyone except the robots drops at the maximum rate.
//                        gameState.characters.values.forEach{char->
//                            if(!char.trait.contains("robot"))
//                                char.approval-= consumption  * factor * 1
//                        }
//                    else//If the resource is less than 12 hours worth left, approval of everyone except the robots drops at the rate INVERSELY proportional to the amount of resource left.
//                        gameState.characters.values.forEach{char->
//                        if(!char.trait.contains("robot"))
//                            char.approval-= min(consumption  * factor * gameState.pop / it.value.amount, consumption  * factor)
//                    }
//
//                }
            //-----------------------------------------------------------------------------------------------------------------------------------------
        }

    }

    //Party size is adjusted every day.
    //Each department is also its own party. At this time, the head of the party is the head of the department.
    //In this case, the number of people in the party is equal to the number of employees in the department. As the number of employees decreases, more unnamed people leave the party, and as the number of employees increases, more unnamed people come in.
    //However, if the total number of people at the station is insufficient, unnamed people may not come in, and fewer people than the target number may come to work.
    fun partySizeAdjust()
    {
//        gameState.parties.values.filter { it.type == "division" }.forEach {
//            val targetSize = it.plannedWorker
//            if (it.size < targetSize)
//            {
//                if (gameState.idlePop >= targetSize - it.anonymousMembers)
//                {
//                    //unnamed people join the party.
//                    it.setAnonMembers(targetSize)
//                    gameState.idlePop -= targetSize - it.anonymousMembers
//                } else
//                {
//                    //unnamed people join the party.
//                    it.setAnonMembers(it.anonymousMembers + gameState.idlePop)
//                    gameState.idlePop = 0
//                }
//            } else if (it.size > targetSize)
//            {
//                //unnamed people leave the party.
//                it.setAnonMembers(targetSize)
//                gameState.idlePop += it.size - targetSize
//            }
//        }

    }

    fun scheduleDailyConferences()
    {
        //Each division has a conference every day. The conference is attended by the head of the division and the members of the division.
        gameState.parties.values.filter { it.type == "division" }.forEach { party ->
            if (party.leader != "")
            {
                val conference = Meeting(
                    gameState.time + 9 * 3600 / dt /*9 in the morning*/,
                    "divisionDailyConference",
                    place = party.home,
                    scheduledCharacters = party.members
                ).also { it.involvedParty = party.name }

                gameState.scheduledMeetings["conference-${party.home}-${party.name}-${gameState.time}"] = conference
            } else
            {
                //If the division leader is not assigned, the conference for electing the division leader is scheduled.
                val conference = Meeting(
                    gameState.time + 9 * 3600 / dt /*9 in the morning*/,
                    "divisionLeaderElection",
                    place = party.home,
                    scheduledCharacters = (setOf("ctrler") + party.members).toHashSet()
                ).also { it.involvedParty = party.name }
                gameState.scheduledMeetings["conference-${party.home}-${party.name}-${gameState.time}"] = conference
            }
        }

        //Cabinet has a conference every day. The conference is attended by the division leaders

        val conference = Meeting(
            gameState.time + 12 * 3600 / dt /*12 in the afternoon*/,
            "cabinetDailyConference",
            place = gameState.parties["cabinet"]!!.home,
            scheduledCharacters = gameState.parties["cabinet"]!!.members
        ).also { it.involvedParty = "cabinet" }

        gameState.scheduledMeetings["conference-${gameState.parties["cabinet"]!!.home}-cabinet-${gameState.time}"] =
            conference


        val conference2 = Meeting(
            gameState.time + 15 * 3600 / dt /*3 in the afternoon*/,
            "triumvirateDailyConference",
            place = gameState.parties["triumvirate"]!!.home,
            scheduledCharacters = gameState.parties["triumvirate"]!!.members
        ).also { it.involvedParty = "triumvirate" }

        gameState.scheduledMeetings["conference-${gameState.parties["triumvirate"]!!.home}-triumvirate-${gameState.time}"] =
            conference2

    }

    fun distributeResourcesHourly()
    {
        val dth = 3600
        //Some resources are scheduled to be distributed to other places. Other resources are distributed manually.
        //Distribute energy. Each energy storage value slowly moves to the average of all energy storage values.
        val energyDistributionTau = 10000 //[s]
        val energyStorage =
            gameState.places.values.filter { place -> place.apparatuses.any { it.name == "energyStorage" } }.sumOf {
                it.resources["energy"]
            }
        val energyStorageCount =
            gameState.places.values.sumOf { place -> place.apparatuses.filter { it.name == "energyStorage" }.size }
        gameState.places.values.filter { place -> place.apparatuses.any { it.name == "energyStorage" } }
            .forEach { place ->
                place.resources["energy"] = (place.resources["energy"]
                        ) + (energyStorage / energyStorageCount * place.apparatuses.filter { it.name == "energyStorage" }.size - (place.resources["energy"]
                        )) / energyDistributionTau * dth
            }
    }

    fun diffuseGas()
    {
        gameState.places.forEach {
            it.value.diffuseGasAndTemp()
        }
    }

    fun workApparatusesHourly()
    {
        gameState.places.forEach { it.value.workApparatusHourly() }
    }

    fun checkMarketResourcesHourly(tgtState: GameState)
    {
        val dth = 3600
        gameState.places.forEach { place ->
            place.value.gasResources.forEach { place.value.gasResources[it.key] = it.value * 0.999 }
        } //1/1000 of the floating resources is lost

        gameState.places.forEach { (placeName, place) ->
            if (place.gasResources["oxygen"] < place.currentTotalPop * const("MarketOxygenConsumptionRate") * 86400)//TODO: Migrate to gas system.
                println("Less than 24 hours of oxygen out in $placeName")
            val consumptionOxygen = (place.currentTotalPop * const("MarketOxygenConsumptionRate") * dth)
            if (place.gasResources["oxygen"] > consumptionOxygen)
            {
                place.gasResources["oxygen"] -= consumptionOxygen //0.5kg/day consumption.
                place.gasResources["carbonDioxide"] += consumptionOxygen * 96 / 64 //Oxygen is converted to carbonDioxide.
            }
            if (place.gasPressure("oxygen") < const("CriticalOxygenPressure") || place.gasPressure("carbonDioxide") / place.gasPressure(
                    "oxygen"
                ) > const("CriticalCarbonDioxideRatio")
            )
            {
                if (place.responsibleDivision == "") return//TODO: currently oxygen deaths don't happen in places without responsibleParty.
                val death =
                    place.currentTotalPop / 100 + 1//TODO: adjust deaths. Also, productivity starts to drop when oxygen is low. Use the gas system.
                place.apply {
                    gameState.parties[responsibleDivision]!!.causeDeaths(death)
                    println("Casualties: at most $death, due to suffocation at $placeName. Pop left: ${gameState.pop}")
                    createRumor(tgtState).apply {
                        type = InformationType.CASUALTY
                        tgtPlace = placeName
                        amount = death
                        auxParty = responsibleDivision
                    }
                }
            }

        }


        //Total redistribution of resources among anonymous people every hour.
        val marketResources = Resources()
        var anonPeople = 0
        gameState.characters.filter { it.key.contains("Anon") }.forEach {
            marketResources += it.value.resources
            anonPeople += it.value.reliant
        }

        gameState.characters.filter { it.key.contains("Anon") }
            .forEach { it.value.resources = marketResources * (it.value.reliant * 1.0 / anonPeople) }

    }

    fun createRumor(tgtState: GameState) = Information(
        author = "",
        creationTime = tgtState.time
    ).also { /*spread rumor*/
        tgtState.informations[it.generateName()] = it //cpy.publicity = 5
        it.knownTo += tgtState.pickRandomCharacter.name
    }


    //TODO: Check for win/lose/interrupt conditions
    fun conditionCheck()
    {
        gameState.characters.forEach { entry ->
            //If air is not breathable, take damage.
            if (entry.value.place.gasPressure("oxygen") < const("CriticalOxygenPressure") || entry.value.place.gasPressure(
                    "carbonDioxide"
                ) / entry.value.place.gasPressure(
                    "oxygen"
                ) > const("CriticalCarbonDioxideRatio")
            )
            {
                entry.value.health -= dt / const("SuffocationTau") * const("HealthMax")
                //If in a workplace, party integrity decreases, if I am not the leader
                entry.value.division?.also {
                    if (entry.value.place.responsibleDivision == it.name && it.leader != entry.key)
                        gameState.setPartyMutuality(
                            it.name,
                            delta = -dt / const("SuffocationTau") * const("mutualityMax")
                        )
                }

            }
            //If temperature is extreme, take damage.
            if (entry.value.place.temperature - 300 /*[K]*/ !in -const("TemperatureDifferenceTolerance")..const("TemperatureDifferenceTolerance")
            )
            {
//                entry.value.health -= dt / const("TemperatureDamageTau") * abs(entry.value.place.temperature / 300 /*[K]*/ - 1) * const(
//                    "HealthMax"
//                )//TODO: balance this
                //If in a workplace, party integrity decreases, if I am not the leader
                entry.value.division?.also {
                    if (entry.value.place.responsibleDivision == it.name && it.leader != entry.key)
                        gameState.setPartyMutuality(
                            it.name,
                            delta = -dt / const("TemperatureDamageTau") * const("mutualityMax")
                        )
                }
            }
            if (entry.value.alive && entry.value.health <= 0)
            {
                println("${entry.key} died.")
                gameState.places.values.find { it.characters.contains(entry.key) }!!.characters -= entry.key
                entry.value.alive = false
            }
        }
        val l = gameState.characters.filter { it.value.alive && !it.value.trait.contains("robot") }
        if (!l.contains(gameState.playerName))
        {

            println("You died. Game over.")
            gameState.dump()
            exitProcess(0)

        } else if (l.size == 1)
        {
            println("You are the last survivor.")
            gameState.dump()
            exitProcess(0)
        }

        if (gameState.time % (ReadOnly.constInt("lengthOfDay") * 15) == 0)
        { //Every 15 days, reset the budget.
            gameState.isBudgetProposed = false
            gameState.isBudgetResolved = false
            //Since the party is division, it pays out the salary of the members.
            gameState.parties.values.filter { it.type == "division" }.forEach { party ->
                party.isSalaryPaid = false
            }
        }
        if (gameState.time % ReadOnly.constInt("lengthOfDay") == 0)
        { //Every day, we used to inform the infrastructure minister about total resource.
//            val infraName = gameState.parties.values.find { it.name == "infrastructure" }!!.leader
//            if (infraName != "")
//            {
//                Information(
//                    infraName,
//                    creationTime = gameState.time,
//                    tgtTime = gameState.time,
//                    type = "resource",
//                    tgtResource = "water",
//                    tgtPlace = "everywhere",
//                    amount = gameState.places.values.sumOf {
//                        it.resources["water"]
//                    }).also { it.knownTo.add(infraName);gameState.informations[it.generateName()] = it }
//                Information(
//                    infraName,
//                    creationTime = gameState.time,
//                    tgtTime = gameState.time,
//                    type = "resource",
//                    tgtResource = "oxygen",
//                    tgtPlace = "everywhere",
//                    amount = gameState.places.values.sumOf {
//                        it.resources["oxygen"]
//                    }).also { it.knownTo.add(infraName);gameState.informations[it.generateName()] = it }
//                Information(
//                    infraName,
//                    creationTime = gameState.time,
//                    tgtTime = gameState.time,
//                    type = "resource",
//                    tgtResource = "ration",
//                    tgtPlace = "everywhere",
//                    amount = gameState.places.values.sumOf {
//                        it.resources["ration"]
//                    }).also { it.knownTo.add(infraName);gameState.informations[it.generateName()] = it }
//            }
        }

    }

    companion object
    {
        var acquireCallback: (Any) -> Unit = {}
        var acquireEvent = arrayListOf<(AcquireParams) -> Unit>()
        val onAccident = ArrayList<(String, Int) -> Unit>()//Place and Casualty

        class AcquireParams(val type: String, val variables: HashMap<String, Any>)


        fun acquire(choices: List<String>): String = runBlocking {
            val logUI = LogUI.instance
            logUI.appendText("Acquire: ${choices.toString().replace("[", "").replace("]", "")}")
            logUI.numberMode = true
            logUI.isInputEnabled = true
            var wanted = -1
            suspendCoroutine { continuation ->
                Gdx.app.postRunnable {
                    logUI.numberModeCallback = { x -> wanted = x; logUI.numberMode = false; continuation.resume(Unit) }
                }
            }
            return@runBlocking choices[wanted]
        }

        inline fun <reified T> acquire(dataType: String, params: HashMap<String, Any>): T = runBlocking {
            var wanted: T? = null

            // Use coroutine to suspend until the acquisition is complete
            suspendCoroutine { continuation ->
                Gdx.app.postRunnable {
                    acquireEvent.forEach { it(AcquireParams(dataType, params)) }
                    acquireCallback = { x ->
                        try
                        {
                            wanted = x as T
                        } catch (e: Exception)
                        {
                            Logger.warning("Acquire failed: Wanted type: ${T::class}, Acquired type: ${x::class}")
                            throw e
                        }
                        // Resume the coroutine to signal completion
                        acquireCallback = {}
                        continuation.resume(Unit)
                    }
                }
            }


            // Return the acquired value
            return@runBlocking wanted as T
        }

        //When someone else requests an action, request class will check isValid function of the action, not this function.
        fun availableActions(gameState: GameState, place: String, character: String): ArrayList<String>
        {
            val actions = arrayListOf<String>()
            val placeObj = gameState.places[place]!!
            if (gameState.ongoingMeetings.any { it.value.currentCharacters.contains(character) })
            {
                val conf = gameState.ongoingMeetings.filter {
                    it.value.currentCharacters.contains(
                        character
                    )
                }.values.first()
                if (character == gameState.playerName)
                {
                    println("You are in a meeting.")
                    println(
                        "Attendees: ${
                            conf.currentCharacters
                        }"
                    )
                }
                val subject = conf.type
                if (subject == "")
                {//If there is no subject, i.e. casual talk
                } else
                    if (character == gameState.parties[conf.involvedParty]!!.leader)//Only the leader can do below actions.
                    {
                        actions.add("Resign") //Only leaders can resign right now. Resign is one of the few actions that can be done without an agenda.
                        if (subject == "divisionDailyConference")
                        {
                            if (!gameState.parties[conf.involvedParty]!!.isSalaryPaid)
                                actions.add("Salary") //Salary is distributed in a divisionDailyConference.
                        }
                    }
                //When not the leader, you can only do below actions.
                //There is no command anymore.
//                if (gameState.parties[conf.involvedParty]?.leader == character)
//                {
//                    actions.add("Command")
//                }
                if (conf.currentSpeaker == character)
                {
                    actions.add("NewAgenda")
                    actions.add("AddInfo")
                    actions.add("EndMeeting")
                    actions.add("EndSpeech")
                } else
                {
                    actions.add("Wait")
                    actions.add("Intercept")

                    //Takeover/Refuse as a separate action is not useful. Once you are nominated to speak, you can either speak or end the speech.
                }
                actions.add("LeaveMeeting")
                return actions
            }
            ////////////////////////////////////////////////////MEETING ACTIONS//////////////////////////////////////////////////////////
            if (placeObj.characters.count() > 1)
                actions.add("Talk")
            if (placeObj.isAccidentScene)
            {
                if (placeObj.responsibleDivision != "" && gameState.parties[placeObj.responsibleDivision]!!.members.contains(
                        character
                    )
                )//Only the responsible party members can clear the accident scene.
                    actions.add("ClearAccidentScene")
                actions.add("InvestigateAccidentScene")
            }
            actions.add("Move")
            actions.add("Examine")
            //actions.add("radio")
            actions.add("Wait")
            if (placeObj.manager == character)
            {
                actions.add("SetWorkers")
                actions.add("SetWorkHours")
            }
            if (place.contains("home"))
            {
                actions.add("Sleep")
                actions.add("Eat")
                actions.add("PrepareInfo")
            }

            if (place == "mainControlRoom" || place == "market" || place == "squareNorth" || place == "squareSouth")
            {
                //actions.add("InfoAnnounce") Only the leader of the internal division can announce.
            }
            if (placeObj.responsibleDivision != "" && gameState.parties[placeObj.responsibleDivision]!!.members.contains(
                    character
                )
            )
            {
                actions.add("UnofficialResourceTransfer")//can only steal from their own division.
                actions.add("OfficialResourceTransfer")//can only move resources from their own division.
            }
            if (place == "home_$character")
            {
                actions.add("UnofficialResourceTransfer")//can only move resources from their home.
            }
            val availableMeetings =
                gameState.scheduledMeetings.filter {
                    it.value.time - gameState.time in -ReadOnly.constInt("MeetingStartTolerance")..ReadOnly.constInt(
                        "MeetingStartTolerance"
                    ) && it.value.place == place
                }
                    .filter { !gameState.ongoingMeetings.containsKey(it.key) }
                    .filter { it.value.scheduledCharacters.contains(character) }
            if (availableMeetings.isNotEmpty())
                actions.add("StartMeeting")
            val meetingsToJoin = gameState.ongoingMeetings.filter {
                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(character) && it.value.place == place
            }
            if (meetingsToJoin.isNotEmpty())
            {
                val subject = gameState.ongoingMeetings.firstNotNullOf { entry ->
                    entry.value.type.takeIf {
                        entry.value.scheduledCharacters.contains(character) && !entry.value.currentCharacters.contains(
                            character
                        ) && entry.value.place == place
                    }
                }
                if (gameState.playerName == character)
                {
                    when (subject)
                    {
                        "Talk" -> println("Someone wants to talk to you.")//TODO: there must be a way to know NPC's intention to talk
                    }
                }
                actions.add("JoinMeeting")
            }
            if (gameState.characters[character]!!.trait.contains("technician") && !place.contains("home"))
            {
                actions.add("Repair")
            }
            return actions
        }

        val random = Random(System.currentTimeMillis())


    }
}