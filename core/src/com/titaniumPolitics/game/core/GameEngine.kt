package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.Character.Type
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.DT
import com.titaniumPolitics.game.core.ReadOnly.S_PER_HR
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.LogUI
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.max
import kotlin.random.Random
import kotlin.system.exitProcess

/*
* GameEngine is a loop that runs the game. Each loop is a turn. Each turn, each character performs an action.
*
*
* */
class GameEngine(val gameState: GameState) {

    //Let UI or other monitors to come in and read gamestate.
    var onObserverCall = arrayListOf<(GameState) -> Unit>()


    init {
        Logger.gState = gameState
        Logger.init()
    }

    fun startGame() {
        //Start the game.

        onObserverCall.forEach { it(gameState) }
        Logger.write("Game started. Time: ${gameState.time}. Starting main loop.", Logger.LogLevel.INFO)
        //Main loop


        while (true) {
            gameLoop()
        }
    }

    fun gameLoop() {
        gameState.characters.values.sortedByDescending { if (it == gameState.player) 1 else 0 }.forEach {
            if (it.alive) {
                if (it.frozen > 0) {
                    it.frozen--
                    if (!it.trait.contains("robot")) {//Robots don't need to eat.
                        it.health -= DT / const("HealthConsumptionTau") * const("HealthMax")
                        it.hunger += DT / const("HungerConsumptionTau") * const("HungerMax")
                        it.thirst += DT / const("ThirstConsumptionTau") * const("ThirstMax")
                        if (it.hunger > const("hungerThreshold")) it.health -= DT / const("HealthConsumptionTau") * const(
                            "HealthMax"
                        )
                        if (it.thirst > const("thirstThreshold")) it.health -= DT / const("HealthConsumptionTau") * const(
                            "HealthMax"
                        )

                    }
                }
                while (it.frozen == 0) {
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

    private fun hourlyProgression() {
        distributeResourcesHourly()
        workApparatusesHourly()
        ageInformationHourly()
        spreadPublicInfo()
        checkMarketResourcesHourly()
        cancelMeetings()
    }

    private fun dailyProgression() {
        partySizeAdjust()
        scheduleDailyConferences()
    }


    //This function is called at the end of each turn, after all the characters have performed their actions.
    fun progression() {
        gameState.time += 1
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
        onObserverCall.forEach { it(gameState) }

    }

    fun performAction(char: Character) {
        var action: GameAction
        val actionList = availableActions(
            this.gameState, gameState.places.values.find { it.characters.contains(char.name) }!!.name,
            char.name
        )
        if (gameState.nonPlayerAgents[char.name] == null && char.name == gameState.playerName)//If the player character agent exists, the game does not wait for player input.
        //This is for automatic progression and test purposes.
        {
            do {
                action = acquire("Action", hashMapOf("actionList" to actionList))
                action.injectParent(gameState)
                if (action.isValid()) {
                    gameState.onPlayerAction.forEach { it() }
                    break
                } else
                    println(
                        "Invalid action: ${action.javaClass.simpleName} by ${char.name} at ${
                            char.place.name
                        }, reason: ${action.invalidReason}"
                    )
            } while (true)


        } else {
            action = gameState.nonPlayerAgents[char.name]?.chooseAction()
                ?: throw Exception("Non player character ${char.name} does not have a nonPlayerAgent.")
            action.injectParent(gameState)
            if (action.javaClass.simpleName !in actionList)
                Logger.write(
                    "Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.formatTime()}, which is not in the action list. This may be a bug."
                )
            if (!action.isValid()) {
                Logger.write(
                    "Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.formatTime()}, which is not valid. This may be a bug."
                )
                Logger.write(Json.encodeToString(GameAction.serializer(), action), Logger.LogLevel.INFO)
                throw Exception("Non player character ${char.name} is performing an invalid action.")
            }
            if (action.sbjCharacter != char.name) {
                Logger.write(
                    "Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.formatTime()}, which is not targeting itself. This may be a bug."
                )
                Logger.write(Json.encodeToString(GameAction.serializer(), action), Logger.LogLevel.INFO)
            }
            if (action.tgtPlace != char.place.name) {
                Logger.write(
                    "Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.formatTime()}, which is not targeting its own place. This may be a bug."
                )
                Logger.write(Json.encodeToString(GameAction.serializer(), action), Logger.LogLevel.INFO)
            }

        }
        char.history.add(
            "Action" +
                    action.javaClass.simpleName + ":" +
                    gameState.formatTime() + " at " + gameState.places.values.find { it.characters.contains(char.name) }!!.name
        )
        val place = gameState.places.values.find {
            it.characters.contains(
                char.name
            )
        }!!.name
        //Unless the information is wait. I think wait info is useless. It adds a lot of overhead, and the info that a character saw someone can be obtained by talking to them instead.
        //Move info is used for moved alert currently.
        if (action !is Wait && char.type != Type.ANON) {
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
                gameState.addInformation(it)
            }
        }
        if (action.sbjCharacter != gameState.playerName)
            onBeforeNonPlayerCharacterAction.forEach { it(action) }
        action.execute()
        gameState.setMutuality(char.name, delta = action.deltaWill())

    }

    private fun calculateMutuality() {
        //Wealth display effect: opportunists mutuality to more wealthy character increases. to less wealthy character decreases.

        //If there are meetings where some characters are missing, all the characters in the meeting lose mutuality toward the missing characters.
        gameState.ongoingMeetings.forEach { meeting ->
            meeting.value.scheduledCharacters.forEach { char ->
                if (!meeting.value.currentCharacters.contains(char))
                    meeting.value.currentCharacters.forEach { char2 ->
                        gameState.setMutuality(
                            char,
                            char2,
                            -DT / const("MutualityReinforcementTau") * ReadOnly.mutualityScale
                        )
                    }
            }
        }
    }

    // information does not affect the approval after some time.
    private fun ageInformationHourly() {
        val removed = arrayListOf<String>()
        gameState.informations.forEach {
            it.value.life -= 3600 //1 hour
            if (it.value.life <= 0 && it.value.rememberedBy.isEmpty())
                removed.add(it.key)
        }
        removed.forEach { gameState.removeInformation(it) }
    }

    //TODO: optimize this function.
    private fun spreadPublicInfo() {
        gameState.parties.forEach { party ->
            //bad news affect the approval. casualty, stolen resource, TODO: low water ration oxygen, high wealth, crimes
            gameState.informations.filter { it.value.type == InformationType.CASUALTY }.forEach {
                var factor = 1.0
                if (it.value.author == null) factor *= 2.0//rumors affect the approval negatively.
                if (it.value.auxParty == party.key) factor *= 2.0//If the casualty is in our party, approval of the responsible party drops even more.
                //If casualty is not localized, does not affect mutualities.
                if (it.value.tgtPlace == "everywhere" || gameState.places[it.value.tgtPlace]!!.responsibleDivision == null) {
                    //Do nothing
                } else
                    gameState.setPartyMutuality(
                        party.key,
                        gameState.places[it.value.tgtPlace]!!.responsibleDivision!!,
                        -it.value.amount * gameState.publicity(
                            it.key,
                            party.key
                        ) / party.value.size * factor * S_PER_HR / const("MutualityFromInfoTau") * ReadOnly.mutualityScale,
                        "CasualtyNews"
                    )
                //if our party is responsible, integrity drops.

            }
            gameState.informations.filter { it.value.type == InformationType.ACTION && it.value.action!!.javaClass.simpleName == "unofficialResourceTransfer" }
                .forEach {
                    var factor = 1
                    if (it.value.author == null) factor = 2//rumors affect the approval negatively.

                    //party loses mutuality toward the responsible party. TODO: consider affecting the individual mutuality toward the perpetrator.
                    //TODO: item value must be put into consideration
                    gameState.places[it.value.tgtPlace]!!.responsibleDivision?.run {

                        gameState.setPartyMutuality(
                            party.key, this, -log(
                                it.value.amount.toDouble() + 1, 2.0
                            ) * gameState.publicity(
                                it.key,
                                party.key
                            ) / party.value.size * factor * S_PER_HR / const("MutualityFromInfoTau") * ReadOnly.mutualityScale,
                            "ResourceStolenNews"
                        )
                    }
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
    fun partySizeAdjust() {
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

    /**
     * This method must be called when gameState.time is midnight.
     */
    fun scheduleDailyConferences() {
        for (daysAhead in 0 until 5) {
            //Schedule meetings for the next 5 days.
            val tgtMidnight = gameState.time + 24 * 3600 / DT * daysAhead
            //Each division has a conference every day. The conference is attended by the head of the division and the directors of the division.
            gameState.parties.values.filter { it.type == "division" }.forEach { party ->
                if (party.leader != null && party.isBudgetProposed && party.isBudgetResolved) {
                    //If there is no conference scheduled that day at the same location,
                    if (gameState.scheduledMeetings.values.none {
                            it.time == tgtMidnight + 9 * 3600 / DT /*9 in the morning*/ && it.place == party.home
                        }) {
                        //If the division leader is assigned, the daily conference is scheduled.
                        val conference = Meeting(
                            tgtMidnight + 9 * 3600 / DT /*9 in the morning*/,
                            Meeting.MeetingType.DIVISION_DAILY_CONFERENCE,
                            place = party.home!!,
                            scheduledCharacters = party.directorMembers //Without low level members
                        ).also { it.involvedParty = party.name }

                        gameState.addScheduledMeeting(conference)
                    }
                } else if (party.leader != null) {
                    //If the division leader is not assigned, the conference for electing the division leader is scheduled.
                    //If there is no election scheduled at all, schedule a division leader election.
                    if (gameState.scheduledMeetings.values.none {
                            it.place == party.home &&
                                    it.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION
                        }) {
                        //If the division leader is not assigned, the division leader election is scheduled.
                        //The conference is attended by the director of the workplace and all division members.
                        //Election is only scheduled for today. If the election is not held today, it will be scheduled again tomorrow.
                        if (daysAhead == 0) {
                            val conference = Meeting(
                                tgtMidnight + 9 * 3600 / DT /*9 in the morning*/,
                                Meeting.MeetingType.DIVISION_LEADER_ELECTION,
                                place = party.home!!,
                                scheduledCharacters = (setOf("ctrler") + party.directorMembers).toHashSet() //Without low level members
                            ).also { it.involvedParty = party.name }
                            gameState.addScheduledMeeting(conference)
                        }
                    }
                } else if (!party.isBudgetProposed) {
                    if (gameState.scheduledMeetings.values.none {
                            it.place == party.home &&
                                    it.type == Meeting.MeetingType.BUDGET_PROPOSAL
                        }) {
                        //If budget is not proposed, the budget proposal meeting is scheduled.
                        //The conference is attended by directors.
                        //Budget proposal is only scheduled for today. If the proposal is not made today, it will be scheduled again tomorrow.
                        if (daysAhead == 0) {
                            val conference = Meeting(
                                tgtMidnight + 9 * 3600 / DT /*9 in the morning*/,
                                Meeting.MeetingType.BUDGET_PROPOSAL,
                                place = party.home!!,
                                scheduledCharacters = party.directorMembers //Without low level members
                            ).also { it.involvedParty = party.name }
                            gameState.addScheduledMeeting(conference)
                        }
                    }
                } else if (!party.isBudgetResolved) {
                    if (gameState.scheduledMeetings.values.none {
                            it.place == party.home &&
                                    it.type == Meeting.MeetingType.BUDGET_RESOLUTION
                        }) {
                        //If budget is not resolved, the budget resolution meeting is scheduled.
                        //The conference is attended by directors.
                        //Budget resolution is only scheduled for today. If the resolution is not made today, it will be scheduled again tomorrow.
                        if (daysAhead == 0) {
                            val conference = Meeting(
                                tgtMidnight + 9 * 3600 / DT /*9 in the morning*/,
                                Meeting.MeetingType.BUDGET_RESOLUTION,
                                place = party.home!!,
                                scheduledCharacters = party.directorMembers //Without low level members
                            ).also { it.involvedParty = party.name }
                            gameState.addScheduledMeeting(conference)
                        }
                    }
                }
            }
            //Each workplace has a conference every day. The conference is attended by the director of the workplace.
            gameState.parties.values.filter { it.type == "workplace" }.forEach { party ->
                if (party.leader != null) {
                    if (gameState.scheduledMeetings.values.none {
                            it.time == tgtMidnight + 12 * 3600 / DT /*9 in the morning*/ && it.place == party.home &&
                                    it.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE
                        }) {
                        val conference = Meeting(
                            tgtMidnight + 12 * 3600 / DT /*12 in the afternoon*/,
                            Meeting.MeetingType.DIVISION_DAILY_CONFERENCE,
                            place = party.home!!,
                            scheduledCharacters = party.realMembers //Without anonymous members
                        ).also { it.involvedParty = party.name }

                        gameState.addScheduledMeeting(conference)
                    }
                }
            }

            val cabinet = gameState.parties["cabinet"]!!

            if (cabinet.isBudgetProposed) { //TODO: schedule the mechanic election here too.

                //Cabinet has a conference every day. The conference is attended by the division leaders
                if (gameState.scheduledMeetings.values.none {
                        it.time == tgtMidnight + 12 * 3600 / DT /*9 in the morning*/ && it.place == cabinet.home!! &&
                                it.type == Meeting.MeetingType.CABINET_DAILY_CONFERENCE
                    }) {
                    val conference = Meeting(
                        tgtMidnight + 12 * 3600 / DT /*12 in the afternoon*/,
                        Meeting.MeetingType.CABINET_DAILY_CONFERENCE,
                        place = cabinet.home!!,
                        scheduledCharacters = cabinet.members
                    ).also { it.involvedParty = cabinet.name }

                    gameState.addScheduledMeeting(conference)
                }
            } else {
                if (gameState.scheduledMeetings.values.none {
                        it.place == cabinet.home &&
                                it.type == Meeting.MeetingType.BUDGET_PROPOSAL
                    }) {
                    //If budget is not proposed, the budget proposal meeting is scheduled.
                    //The conference is attended by directors.
                    //Budget proposal is only scheduled for today. If the proposal is not made today, it will be scheduled again tomorrow.
                    if (daysAhead == 0) {
                        val conference = Meeting(
                            tgtMidnight + 12 * 3600 / DT /*12 in the afternoon*/,
                            Meeting.MeetingType.BUDGET_PROPOSAL,
                            place = cabinet.home!!,
                            scheduledCharacters = cabinet.directorMembers //Without low level members
                        ).also { it.involvedParty = cabinet.name }
                        gameState.addScheduledMeeting(conference)
                    }
                }
            }


            val triumvirate = gameState.parties["triumvirate"]!!

            if (cabinet.isBudgetProposed && !triumvirate.isBudgetResolved) {
                if (gameState.scheduledMeetings.values.none {
                        it.place == triumvirate.home &&
                                it.type == Meeting.MeetingType.BUDGET_RESOLUTION
                    }) {
                    //If budget is not resolved, the budget resolution meeting is scheduled.
                    //The conference is attended by directors.
                    //Budget resolution is only scheduled for today. If the resolution is not made today, it will be scheduled again tomorrow.
                    if (daysAhead == 0) {
                        val conference = Meeting(
                            tgtMidnight + 9 * 3600 / DT /*9 in the morning*/,
                            Meeting.MeetingType.BUDGET_RESOLUTION,
                            place = triumvirate.home!!,
                            scheduledCharacters = triumvirate.directorMembers //Without low level members
                        ).also { it.involvedParty = triumvirate.name }
                        gameState.addScheduledMeeting(conference)
                    }
                }
            } else {
                //Triumvirate has a conference every day.
                if (gameState.scheduledMeetings.values.none {
                        it.time == tgtMidnight + 15 * 3600 / DT /*9 in the morning*/ && it.place == triumvirate.home!! &&
                                it.type == Meeting.MeetingType.TRIUMVIRATE_DAILY_CONFERENCE
                    }) {
                    val conference2 = Meeting(
                        tgtMidnight + 15 * 3600 / DT /*3 in the afternoon*/,
                        Meeting.MeetingType.TRIUMVIRATE_DAILY_CONFERENCE,
                        place = triumvirate.home!!,
                        scheduledCharacters = triumvirate.members
                    ).also { it.involvedParty = triumvirate.name }

                    gameState.addScheduledMeeting(conference2)
                }
            }

        }
    }

    fun distributeResourcesHourly() {
        //Some resources are scheduled to be distributed to other places. Other resources are distributed manually.
        //Distribute energy. Each energy storage value slowly moves to the average of all energy storage values.
        val energyDistributionTau = 10000 //[s]
        val energyPlaces =
            gameState.places.values.filter { place -> place.apparatuses.any { it.name == "energyStorage" } }
        val energyStorage =
            energyPlaces.sumOf {
                it.resources["energy"]
            }
        val energyStorageCount =
            gameState.places.values.sumOf { place -> place.apparatuses.filter { it.name == "energyStorage" }.size }
        energyPlaces
            .forEach { place ->
                place.resources["energy"] = (place.resources["energy"]
                        ) + (energyStorage / energyStorageCount * place.apparatuses.filter { it.name == "energyStorage" }.size - (place.resources["energy"]
                        )) / energyDistributionTau * S_PER_HR
            }
    }

    fun diffuseGas() {
        gameState.places.forEach {
            it.value.diffuseGasAndTemp()
        }
    }

    fun workApparatusesHourly() {
        gameState.places.forEach { it.value.workApparatusHourly() }
    }

    fun checkMarketResourcesHourly() {
        gameState.places.forEach { (placeName, place) ->
            place.gasResources.forEach {
                place.gasResources[it.key] = it.value * 0.999
            }//1/1000 of the floating resources is lost
            place.addHeat(place.currentTotalPop * const("IdleHumanHeatProduction") * S_PER_HR) //Humans generate heat

            if (place.gasResources["oxygen"] < place.currentTotalPop * const("MarketOxygenConsumptionRate") * 86400)//TODO: Migrate to gas system.
                Logger.write("Less than 24 hours of oxygen out in $placeName", Logger.LogLevel.INFO)
            val consumptionOxygen = (place.currentTotalPop * const("MarketOxygenConsumptionRate") * S_PER_HR)
            if (place.gasResources["oxygen"] > consumptionOxygen) {
                place.gasResources["oxygen"] -= consumptionOxygen
                place.gasResources["carbonDioxide"] += consumptionOxygen * 44 / 32 //Oxygen is converted to carbonDioxide.
            }

        }


        //Total redistribution of resources among anonymous people every hour.
        val marketResources = Resources()
        var anonPeople = 0
        gameState.characters.filter { it.value.type == Character.Type.ANON }.forEach {
            marketResources += it.value.resources
            anonPeople += it.value.reliant
        }

        gameState.characters.filter { it.value.type == Character.Type.ANON }
            .forEach { it.value.resources = marketResources * (it.value.reliant * 1.0 / anonPeople) }

    }

    fun cancelMeetings() {
        val missedMeetings = hashSetOf<String>()

        with(gameState) {
            scheduledMeetings.filter {
                it.value.time + ReadOnly.constInt("MeetingStartTolerance") < time && !missedMeetings.contains(
                    it.key
                )
            }.forEach {
                missedMeetings.add(it.key)
                Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
                Logger.write("!Missed meeting:${it.key} at ${it.value.place}.", Logger.LogLevel.INFO)
                Logger.write("Scheduled: ${GameState.formatTime(it.value.time)}", Logger.LogLevel.INFO)
                Logger.write("What people are doing:", Logger.LogLevel.INFO)
                it.value.scheduledCharacters.forEach { ch ->
                    Logger.write(
                        "\t$ch:${characters[ch]!!.history.last { it.startsWith("Action") }}",
                        Logger.LogLevel.INFO
                    )
                    if (nonPlayerAgents[ch] is NonPlayerAgent) {
                        Logger.write(
                            "\t\tunder ${(nonPlayerAgents[ch] as NonPlayerAgent).routines[0]::class.java.simpleName}",
                            Logger.LogLevel.INFO
                        )
                        Logger.write(
                            "\t\troutine started: ${
                                GameState.formatTime(
                                    (nonPlayerAgents[ch] as NonPlayerAgent
                                            ).routines[0].routineStartTime
                                )
                            }",
                            Logger.LogLevel.INFO
                        )
                        (nonPlayerAgents[ch] as NonPlayerAgent).routines[0].variables.forEach { (key, value) ->
                            Logger.write("\t\t$key: $value", Logger.LogLevel.INFO)
                        }
                    }
                }
                Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)


                //TODO: Check ScheduleDailyConferences functions, try to unify the logic of scheduling meetings.
                //If we missed an election meeting, schedule a new one.
                if (it.value.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION) {
                    //If the division leader election is missed, schedule a new one.
                    val newElection = Meeting(
                        it.value.time + 24 * 3600 / DT * 3, //same time as the missed meeting, 3 days later
                        Meeting.MeetingType.DIVISION_LEADER_ELECTION,
                        place = it.value.place,
                        scheduledCharacters = it.value.scheduledCharacters
                    ).also { new -> new.involvedParty = it.value.involvedParty }
                    addScheduledMeeting(newElection)
                }
                //If the budget proposal meeting is missed, schedule a new one.
                if (it.value.type == Meeting.MeetingType.BUDGET_PROPOSAL) {
                    val newBudgetProposal = Meeting(
                        it.value.time + 24 * 3600 / DT, //Next day
                        Meeting.MeetingType.BUDGET_PROPOSAL,
                        place = it.value.place,
                        scheduledCharacters = it.value.scheduledCharacters
                    ).also { new -> new.involvedParty = it.value.involvedParty }
                    addScheduledMeeting(newBudgetProposal)
                }
            }

            //For some meeting types, check if the leader is assigned. If not, remove the meeting.
            scheduledMeetings.filter {
                (it.value.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE || it.value.type == Meeting.MeetingType.CABINET_DAILY_CONFERENCE ||
                        it.value.type == Meeting.MeetingType.BUDGET_PROPOSAL
                        ) && parties[it.value.involvedParty!!]!!.leader == null && !missedMeetings.contains(
                    it.key
                )
            }.forEach {
                missedMeetings.add(it.key)
            }
            missedMeetings.forEach {
                removeScheduledMeeting(it)
            }
        }
    }


    //TODO: Check for win/lose/interrupt conditions
    fun conditionCheck() {
        gameState.aliveCharacters.forEach { entry ->
            val char = entry.value

            //Robots do not need to eat, breathe, or suffer from extreme temperatures.
            if ("robot" !in char.trait) {
                //If air is not breathable, take damage.
                if (char.place.gasPressure("oxygen") < const("CriticalOxygenPressure") || char.place.gasPressure(
                        "carbonDioxide"
                    ) / char.place.gasPressure(
                        "oxygen"
                    ) > const("CriticalCarbonDioxideRatio")
                ) {
                    char.health -= DT / const("SuffocationTau") * const("HealthMax")
                    //If in a workplace, opinion of the leader decreases.
                    char.place.workplaceParty?.let {
                        if (char.name in it.members) {
                            it.leader?.let { wkLeader ->
                                gameState.setMutuality(
                                    char.name, wkLeader,
                                    delta = -DT / const("SuffocationIntegrityDamageTau") * const("mutualityMax") * abs(
                                        char.place.temperature / 300 /*[K]*/ - 1
                                    ),
                                    "SuffocationIntegrityDamage"
                                )
                            }
                            //If the character is in a division, the opinion of the division leader also decreases.
                            char.division?.leader?.let { divisionLeader ->
                                gameState.setMutuality(
                                    char.name, divisionLeader,
                                    delta = -DT / const("SuffocationIntegrityDamageTau") * const("mutualityMax") * abs(
                                        char.place.temperature / 300 /*[K]*/ - 1
                                    ),
                                    "SuffocationLeaderTrustDamage"
                                )
                            }

                        }
                    }

                }
                //If temperature is extreme, take damage.
                if (char.place.temperature - 300 /*[K]*/ !in -const("TemperatureDifferenceTolerance")..const("TemperatureDifferenceTolerance")
                ) {
                    char.health -= DT / const("TemperatureDamageTau") * abs(char.place.temperature / 300 /*[K]*/ - 1) * const(
                        "HealthMax"
                    )
                    //If in a workplace, opinion of the leader decreases.
                    char.place.workplaceParty?.let {
                        if (char.name in it.members) {
                            it.leader?.let { wkLeader ->
                                gameState.setMutuality(
                                    char.name, wkLeader,
                                    delta = -DT / const("TemperatureIntegrityDamageTau") * const("mutualityMax") * abs(
                                        char.place.temperature / 300 /*[K]*/ - 1
                                    ),
                                    "TemperatureIntegrityDamage"
                                )
                            }
                            //If the character is in a division, the opinion of the division leader also decreases.
                            char.division?.leader?.let { divisionLeader ->
                                gameState.setMutuality(
                                    char.name, divisionLeader,
                                    delta = -DT / const("TemperatureIntegrityDamageTau") * const("mutualityMax") * abs(
                                        char.place.temperature / 300 /*[K]*/ - 1
                                    ),
                                    "TemperatureLeaderTrustDamage"
                                )
                            }

                        }
                    }
                }
                with(char) {
                    if ((hunger > const("hungerThreshold") || thirst > const("thirstThreshold")) && reliant > 1)
                        killReliant(max(reliant / 10, 1))
                    if (alive && health <= 0) {
                        if (type == Type.ANON) {
                            killReliant(
                                max(
                                    reliant / 10,
                                    1
                                )
                            ) //If the character is an anon, kill an arbitrary fraction of them.
                            //If the number of reliant becomes 0, the anon does not die but does not provide any labor.
                            health = const("HealthMax") //Reset health to max.
                        } else
                            killCharacter(char)
                    }
                }
            }
        }
        val l = gameState.aliveCharacters.filter { !it.value.trait.contains("robot") }
        if (!l.contains(gameState.playerName)) {

            Logger.write("You died. Game over.", Logger.LogLevel.INFO)
            gameState.dump()
            exitProcess(0)

        } else if (l.size == 1) {
            Logger.write("You are the last survivor.", Logger.LogLevel.INFO)
            gameState.dump()
            exitProcess(0)
        }

        if (gameState.time % (ReadOnly.constInt("lengthOfDay") * 15) == 0) { //Every 15 days, reset the budget.
            gameState.parties.values.forEach {
                it.isBudgetProposed = false
                it.isBudgetResolved = false
                it.proposedBudgets.clear()
                it.budget = Budget(hashMapOf())
            }
            //Since the party is division, it pays out the salary of the members.
            gameState.parties.values.filter { it.type in listOf("division", "cabinet", "workplace") }.forEach { party ->
                party.isSalaryPaid = false
            }
        }
        if (gameState.time % ReadOnly.constInt("lengthOfDay") == 0) { //Every day, we used to inform the infrastructure minister about total resource.
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

    private fun killCharacter(char: Character) {
        if (!char.alive)
            Logger.write("${char.name} is already dead.", Logger.LogLevel.ERROR)
        if (char.type == Type.ANON)
            Logger.write(
                "${char.name} is an anon, killing them is not allowed.",
                Logger.LogLevel.ERROR
            )
        Logger.write("${char.name} died.", Logger.LogLevel.INFO)
        char.place.resources.plusAssign(Resources("corpse" to 100.0 * char.reliant)) //Add corpses to the place.
        char.place.characters -= char.name //Remove from the place.
        gameState.parties.values.forEach {
            it.members -= char.name
            if (it.leader == char.name) {
                it.leader = null
            }
        } //Remove from all parties.
        char.alive = false
    }

    fun destroy() {
        gameState.destroy()
    }

    companion object {
        var acquireCallback: (Any) -> Unit = {}
        var acquireEvent = arrayListOf<(AcquireParams) -> Unit>()
        val onAccident = ArrayList<(String, Int) -> Unit>()//Place and Casualty
        val onBeforeNonPlayerCharacterAction =
            ArrayList<(GameAction) -> Unit>()//Character and Action, used for UI animation between player turns.

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
                acquireCallback = { x ->
                    try {
                        wanted = x as T
                    } catch (e: Exception) {
                        Logger.write("Acquire failed: Wanted type: ${T::class}, Acquired type: ${x::class}")
                        throw e
                    }
                    // Resume the coroutine to signal completion
                    acquireCallback = {
                        Logger.write("Acquire callback was called again with type: ${it::class}")
                    }
                    //Logger.write("Acquire callback resumed.", Logger.LogLevel.INFO)
                    continuation.resume(Unit)
                }
                //Logger.write("acquireCallback set.", Logger.LogLevel.INFO)
                Gdx.app.postRunnable {
                    //Logger.write("Acquiring $dataType with params: $params", Logger.LogLevel.INFO)
                    (acquireEvent).toList().forEach {
                        it(
                            AcquireParams(
                                dataType,
                                params
                            )
                        )
                    }//Shallow copy of the list to avoid concurrent modification.
                }
            }


            // Return the acquired value
            return@runBlocking wanted as T
        }

        /**When someone else requests an action, request class will check isValid function of the action, not this function.
         * However, NPC routines will use this function to plan their actions ahead.
         * Hence, do not include temporal conditions such as "is it your turn now".
         */
        fun availableActions(gameState: GameState, place: String, character: String): HashSet<String> {
            val actions = hashSetOf<String>()
            val placeObj = gameState.places[place]!!
            if (gameState.ongoingMeetings.any { it.value.currentCharacters.contains(character) }) {
                val conf = gameState.ongoingMeetings.filter {
                    it.value.currentCharacters.contains(
                        character
                    )
                }.values.first()
                if (character == gameState.playerName) {
                    Logger.write("You are in a meeting.", Logger.LogLevel.INFO)
                    Logger.write(
                        "Attendees: ${
                            conf.currentCharacters
                        }", Logger.LogLevel.INFO
                    )
                }
                val subject = conf.type
                when (subject) {
                    Meeting.MeetingType.TALK -> {}
                    Meeting.MeetingType.DIVISION_LEADER_ELECTION -> {
                        if (character == "ctrler")
                            actions.add("FinishNomination") //Only the controller can finish the nomination.
                    }

                    Meeting.MeetingType.DIVISION_DAILY_CONFERENCE -> {
                        if (character == gameState.parties[conf.involvedParty]!!.leader)//Only the leader can do below actions.
                        {
                            actions.add("Resign") //Only leaders can resign right now. Resign is one of the few actions that can be done without an agenda.
                            if (!gameState.parties[conf.involvedParty]!!.isSalaryPaid)
                                actions.add("Salary") //Salary is distributed in a divisionDailyConference.
                        }
                    }

                    Meeting.MeetingType.BUDGET_PROPOSAL -> TODO()
                    Meeting.MeetingType.BUDGET_RESOLUTION -> TODO()
                    Meeting.MeetingType.CABINET_DAILY_CONFERENCE -> {

                    }

                    Meeting.MeetingType.TRIUMVIRATE_DAILY_CONFERENCE -> {

                    }
                }
                if (conf.currentSpeaker == character) {
                    actions.add("NewAgenda")
                    actions.add("AddInfo")
                    actions.add("EndMeeting")
                    actions.add("EndSpeech")
                } else {
                    actions.add("Wait")
                    actions.add("Intercept")

                    //Takeover/Refuse as a separate action is not useful. Once you are nominated to speak, you can either speak or end the speech.
                }
                actions.add("LeaveMeeting")
                return actions
            }
            ////////////////////////////////////////////////////MEETING ACTIONS//////////////////////////////////////////////////////////
            actions.add("Talk") //if (placeObj.realCharacters.count() > 1), but this condition is temporal.
            if (placeObj.isAccidentScene) {
                if (placeObj.responsibleDivision != null && gameState.parties[placeObj.responsibleDivision]!!.members.contains(
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
            if (place.contains("home")) {
                actions.add("Sleep")
                actions.add("Eat")
                actions.add("PrepareInfo")
            }

            if (place == "mainControlRoom" || place == "market" || place == "squareNorth" || place == "squareSouth") {
                //actions.add("InfoAnnounce") Only the leader of the internal division can announce.
            }

            if (placeObj.workplaceParty?.overseer == character) {
                actions.add("SetWorkers")
                actions.add("SetWorkHours")
            }
            if (placeObj.workplaceParty?.treasurer == character) {
                actions.add("OfficialResourceTransfer")
                actions.add("UnofficialResourceTransfer")//can steal if I am the treasurer.
            }
            if (placeObj.workplaceParty?.treasurer == null
            ) {
                actions.add("UnofficialResourceTransfer")//can steal if there is no treasurer.
            }
            if (place == "home_$character") {
                actions.add("UnofficialResourceTransfer")//can only move resources from their home.
            }
            val availableMeetings =
                gameState.scheduledMeetings.filter {
                    it.value.isValidTimeToStart(gameState.time)
                            && it.value.place == place
                }
                    .filter { !gameState.ongoingMeetings.containsKey(it.key) }
                    .filter { it.value.scheduledCharacters.contains(character) }
            if (availableMeetings.isNotEmpty() && availableMeetings.values.first().scheduledCharacters.intersect(
                    gameState.places[place]!!.characters
                ).size >= 2
            )
                actions.add("StartMeeting")//If there are more than one character scheduled to attend the meeting, you can start it.
            val meetingsToJoin = gameState.ongoingMeetings.filter {
                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(character) && it.value.place == place
            }
            if (meetingsToJoin.isNotEmpty()) {
                gameState.ongoingMeetings.firstNotNullOf { entry ->
                    entry.value.type.takeIf {
                        entry.value.scheduledCharacters.contains(character) && !entry.value.currentCharacters.contains(
                            character
                        ) && entry.value.place == place
                    }
                }
                actions.add("JoinMeeting")
            }
            if (!place.contains("home")) {
                if (character in gameState.parties["infrastructure"]!!.members && gameState.characters[character]!!.trait.contains(
                        "engineer"
                    ) && /*If there is an apparatus in this place*/
                    placeObj.apparatuses.isNotEmpty()
                )
                    actions.add("Repair") //Infrastructure party members can repair the place.
                if (character in gameState.parties["safety"]!!.members && gameState.characters[character]!!.trait.contains(
                        "soldier"
                    )
                ) {
                    actions.add("BlockAccess")
                    actions.add("Arrest")
                }
                if (character in gameState.parties["education"]!!.members) {
                    actions.add("IssueDiploma")
                }

            }
            return actions
        }

        val random = Random(System.currentTimeMillis())


    }
}