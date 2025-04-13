package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.LogUI
import kotlinx.coroutines.runBlocking
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
    val random = Random(System.currentTimeMillis())


    fun startGame()
    {
        //Start the game.

        runBlocking {
            suspendCoroutine { cont ->
                Gdx.app.postRunnable {
                    val current =
                        gameState.updateUI.clone() as ArrayList<(GameState) -> Unit> //Clone the list to prevent concurrent modification, because updateUI can be modified by UI elements during the update.
                    current.forEach { it(gameState) }//Update UI
                    cont.resume(Unit)
                }
            }
        }
        println("Game started. Time: ${gameState.time}. Starting main loop.")
        //Main loop
        while (true)
        {
            gameState.characters.values.sortedByDescending { if (it == gameState.player) 1 else 0 }.forEach {
                if (it.alive)
                {
                    if (it.frozen > 0)
                    {
                        it.frozen--
                        if (!it.trait.contains("robot"))
                        {//Robots don't need to eat.
                            it.health--
                            it.hunger++
                            it.thirst++
                            with(ReadOnly) {
                                if (it.hunger > const("hungerThreshold")) it.health -= ((const("hungerMax") - const(
                                    "hungerThreshold"
                                )) / (const("hungerMax") + 1 - it.hunger)).toInt()
                                if (it.thirst > const("thirstThreshold")) it.health -= ((const("thirstMax") - const(
                                    "thirstThreshold"
                                )) / (const("thirstMax") + 1 - it.thirst)).toInt()
                            }
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
            if (gameState.time % ReadOnly.const("lengthOfDay").toInt() == 0)//Every day
            {
                partySizeAdjust()
                scheduleDailyConferences()
            }
        }
    }

    fun performAction(char: Character)
    {
        var action: GameAction
        val actionList = availableActions(
            this.gameState, gameState.places.values.find { it.characters.contains(char.name) }!!.name,
            char.name
        )
        if (char.name == gameState.playerName)
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
                println(
                    "Warning: Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.time}, which is not in the action list. This may be a bug."
                )
            if (!action.isValid())
            {
                println(
                    "Warning: Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.time}, which is not valid. This may be a bug."
                )
                throw Exception("Non player character ${char.name} is performing an invalid action.")
            }
            if (action.sbjCharacter != char.name)
                println(
                    "Warning: Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.time}, which is not targeting itself. This may be a bug."
                )
            if (action.tgtPlace != char.place.name)
                println(
                    "Warning: Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        char.place.name
                    }, time=${gameState.time}, which is not targeting its own place. This may be a bug."
                )

        }
        char.history[gameState.time] = action.javaClass.simpleName
        val place = gameState.places.values.find {
            it.characters.contains(
                char.name
            )
        }!!.name
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
        action.execute()
        gameState.setMutuality(char.name, char.name, action.deltaWill().toDouble())

    }

    //This function is called at the end of each turn, after all the characters have performed their actions.
    fun progression()
    {
        gameState.time += 1
        println("[${gameState.time}]")
        distributePopulation()
        distributeResources()
        calculateMutuality()
        workAppratuses()
        conditionCheck()
        checkMarketResources(gameState)
        ageInformation()
        spreadPublicInfo()
        gameState.ongoingMeetings.forEach {
            progressMeeting(gameState, it.value)
        }
        gameState.requests.forEach {
            it.value.refresh(gameState)
        }


        //println("Time: ${gameState.time}")
        //println("My approval:${gameState.characters[gameState.playerAgent]!!.approval}")
        runBlocking {
            suspendCoroutine { cont ->
                val current =
                    gameState.updateUI.clone() as ArrayList<(GameState) -> Unit> //Clone the list to prevent concurrent modification, because updateUI can be modified by UI elements during the update.

                Gdx.app.postRunnable {
                    current.forEach { it(gameState) }//Update UI
                    cont.resume(Unit)
                }
            }
        }
    }

    //TODO: Normalization of mutuality is not implemented yet.
    private fun calculateMutuality()
    {
        //Wealth display effect: opportunists mutuality to more wealthy character increases. to less wealthy character decreases.

        //If there are meetings where some characters are missing, all the characters in the meeting lose mutuality toward the missing characters.
        gameState.ongoingMeetings.forEach { meeting ->
            meeting.value.scheduledCharacters.forEach { char ->
                if (!meeting.value.currentCharacters.contains(char))
                    meeting.value.currentCharacters.forEach { char2 ->
                        gameState.setMutuality(char, char2, -.5)
                    }
            }
        }
    }

    // information does not affect the approval after some time.
    private fun ageInformation()
    {
        val removed = arrayListOf<String>()
        gameState.informations.forEach {
            it.value.life--
            if (it.value.life <= 0)
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
                if (it.value.tgtPlace == "everywhere")
                {
                    //Do nothing
                } else
                    gameState.setPartyMutuality(
                        party.key,
                        gameState.places[it.value.tgtPlace]!!.responsibleParty,
                        -it.value.amount * gameState.publicity(it.key, party.key) * factor / 1000
                    )
                //if our party is responsible, integrity drops.

            }
            gameState.informations.filter { it.value.type == InformationType.ACTION && it.value.action!!.javaClass.simpleName == "unofficialResourceTransfer" }
                .forEach {
                    var factor = 1
                    if (it.value.author == "") factor = 2//rumors affect the approval negatively.

                    //party loses mutuality toward the responsible party. TODO: consider affecting the individual mutuality toward the perpetrator.
                    gameState.setPartyMutuality(
                        party.key, gameState.places[it.value.tgtPlace]!!.responsibleParty, -log(
                            it.value.amount.toDouble() + 1, 2.0
                        ) * gameState.publicity(it.key, party.key) / 100 * factor
                    )
                }
            //The fact that resource is low itself does not affect the mutuality.--------------------------------------------------------------------
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
        gameState.parties.values.filter { it.type == "division" }.forEach {
            val targetSize = gameState.places.values.filter { place -> place.responsibleParty == it.name }
                .sumOf { place -> place.plannedWorker }
            if (it.size < targetSize)
            {
                if (gameState.idlePop >= targetSize - it.size)
                {
                    //unnamed people join the party.
                    val tmp = targetSize - it.size
                    it.anonymousMembers += tmp
                    gameState.idlePop -= tmp
                } else
                {
                    //unnamed people join the party.
                    it.anonymousMembers += gameState.idlePop
                    gameState.idlePop = 0
                }
            } else if (it.size > targetSize)
            {
                //unnamed people leave the party.
                it.reduceAnonMembers(it.size - targetSize)
                gameState.idlePop += it.size - targetSize
            }
        }

    }

    fun scheduleDailyConferences()
    {
        //Each division has a conference every day. The conference is attended by the head of the division and the members of the division.
        gameState.parties.values.filter { it.type == "division" }.forEach { party ->
            if (party.leader != "")
            {
                val conference = Meeting(
                    gameState.time + 18 /*9 in the morning*/,
                    "divisionDailyConference",
                    place = party.home,
                    scheduledCharacters = party.members
                ).also { it.involvedParty = party.name }

                gameState.scheduledMeetings["conference-${party.home}-${party.name}-${gameState.time}"] = conference
            } else
            {
                //If the division leader is not assigned, the conference for electing the division leader is scheduled.
                val conference = Meeting(
                    gameState.time + 18 /*9 in the morning*/,
                    "divisionLeaderElection",
                    place = party.home,
                    scheduledCharacters = (setOf("ctrler") + party.members).toHashSet()
                ).also { it.involvedParty = party.name }
                gameState.scheduledMeetings["conference-${party.home}-${party.name}-${gameState.time}"] = conference
            }
        }

        //Cabinet has a conference every day. The conference is attended by the division leaders

        val conference = Meeting(
            gameState.time + 24 /*12 in the afternoon*/,
            "cabinetDailyConference",
            place = gameState.parties["cabinet"]!!.home,
            scheduledCharacters = gameState.parties["cabinet"]!!.members
        ).also { it.involvedParty = "cabinet" }

        gameState.scheduledMeetings["conference-${gameState.parties["cabinet"]!!.home}-cabinet-${gameState.time}"] =
            conference


        val conference2 = Meeting(
            gameState.time + 30 /*3 in the afternoon*/,
            "triumvirateDailyConference",
            place = gameState.parties["triumvirate"]!!.home,
            scheduledCharacters = gameState.parties["triumvirate"]!!.members
        ).also { it.involvedParty = "triumvirate" }

        gameState.scheduledMeetings["conference-${gameState.parties["triumvirate"]!!.home}-triumvirate-${gameState.time}"] =
            conference2

    }

    //Workers are assigned to apparatuses. If there is not enough workers, some apparatuses are not worked.
    fun distributePopulation()
    {
        val popsDivision = hashMapOf<String, Int>()
        gameState.parties.values.filter { it.type == "division" }.forEach {
            popsDivision[it.name] = it.size//TODO: named members should also go to work and tracked here,
        }
        gameState.places.values.forEach { place ->
            if (place.workHoursStart < gameState.hour && place.workHoursEnd > gameState.hour)
            {
                if (place.isAccidentScene) return@forEach //If there is an accident, no one works until it is resolved.
                val idealWorker = place.apparatuses.sumOf { apparatus -> apparatus.idealWorker }
                if (place.plannedWorker > place.resources["water"]!!)//out of budget. Shut down the facility until the water is back.
                {
                    place.apparatuses.forEach { apparatus -> apparatus.currentWorker = 0 }
                    return@forEach
                }
                if (popsDivision[place.responsibleParty]!! >= place.plannedWorker)
                {
                    place.apparatuses.forEach lambda@{ apparatus ->
                        if (idealWorker == 0) return@lambda
                        apparatus.currentWorker =
                            place.plannedWorker * apparatus.idealWorker / idealWorker//Distribute workers according to ideal worker
                        popsDivision[place.responsibleParty] =
                            popsDivision[place.responsibleParty]!! - apparatus.currentWorker
                    }
                } else
                {
                    //If there is not enough workers, the lack of workers is distributed to apparatuses.
                    place.apparatuses.forEach lambda@{ apparatus ->
                        if (idealWorker == 0) return@lambda
                        apparatus.currentWorker =
                            popsDivision[place.responsibleParty]!! * apparatus.idealWorker / idealWorker//Distribute workers according to ideal worker
                        popsDivision[place.responsibleParty] =
                            popsDivision[place.responsibleParty]!! - apparatus.currentWorker
                    }
                }

            } else
            //If it is not work hours, no one works.
                place.apparatuses.forEach { apparatus -> apparatus.currentWorker = 0 }
        }
    }

    fun distributeResources()
    {
        //Some resources are scheduled to be distributed to other places. Other resources are distributed manually.
        //Distribute energy. Each energy storage value slowly moves to the average of all energy storage values.
        val energyDistributionSpeed = 1
        val energyStorage =
            gameState.places.values.filter { place -> place.apparatuses.any { it.name == "energyStorage" } }.sumOf {
                it.resources["energy"] ?: 0
            }
        val energyStorageCount =
            gameState.places.values.sumOf { place -> place.apparatuses.filter { it.name == "energyStorage" }.size }
        gameState.places.values.filter { place -> place.apparatuses.any { it.name == "energyStorage" } }
            .forEach { place ->
                place.resources["energy"] = (place.resources["energy"]
                    ?: 0) + (energyStorage / energyStorageCount * place.apparatuses.filter { it.name == "energyStorage" }.size - (place.resources["energy"]
                    ?: 0)) * energyDistributionSpeed / 100 //TODO: make sure that the energy is not lost during integer division.
            }
    }

    fun workAppratuses()
    {
        gameState.places.forEach { entry ->
            entry.value.apparatuses.forEach app@{ apparatus ->
                apparatus.durability -= 1//Apparatuses are damaged over time. TODO: get rid of unexpected behaviors, if any
                if (apparatus.durability < 0)
                    apparatus.durability = 0
                //Check if it is workable------------------------------------------------------------------------------
                if (entry.value.isAccidentScene) return@app //If there is an accident, no one works until it is resolved.
                apparatus.currentProduction.forEach {
                    if ((entry.value.resources[it.key] ?: 0) + it.value > (entry.value.maxResources[it.key] ?: 0))
                        return@app //If the resource is full, no one works.
                }
                if (isShortOfResources(apparatus, place = entry.value) != "")
                    return@app //If there is not enough resources, no one works.
                if (isShortOfAbsorbableResources(apparatus, place = entry.value, gameState) != "")
                    return@app //If there is not enough resources, no one works.
                //-----------------------------------------------------------------------------------------------------
                apparatus.currentProduction.forEach {
                    entry.value.resources[it.key] = (entry.value.resources[it.key] ?: 0) + it.value
                }
                val waterConsumption = min(apparatus.currentWorker, (entry.value.resources["water"] ?: 0))
                entry.value.resources["water"] = (entry.value.resources["water"]
                    ?: 0) - waterConsumption//Distribute water to workers. TODO: if there is not enough water, workers should grunt.

                gameState.marketResources["water"] = (gameState.marketResources["water"] ?: 0) + waterConsumption
                apparatus.currentConsumption.forEach {
                    entry.value.resources[it.key] = (entry.value.resources[it.key] ?: 0) - it.value
                }
                apparatus.currentDistribution.forEach {
                    gameState.marketResources[it.key] = (gameState.marketResources[it.key] ?: 0) + it.value
                }
                apparatus.currentAbsorption.forEach {
                    gameState.floatingResources[it.key] = (gameState.floatingResources[it.key] ?: 0) - it.value
                }

                if (apparatus.currentGraveDanger > random.nextDouble())
                {
                    //Catastrophic accident occurred.
                    println("Catastrophic accident occurred at: ${entry.value.name}")
                    entry.value.isAccidentScene = true
                    generateCatastrophicAccidents(gameState, entry.value)

                } else if (apparatus.currentDanger > random.nextDouble())
                {
                    //Accident occurred.
                    println("Accident occurred at: ${entry.value.name}")
                    entry.value.isAccidentScene = true
                    generateAccidents(gameState, entry.value)

                }
                if (apparatus.name in listOf(
                        "waterStorage",
                        "oxygenStorage",
                        "lightMetalStorage",
                        "componentStorage",
                        "rationStorage"
                    )
                )
                {
                    apparatus.durability += 1//Storages are repaired if they are worked.
                }


            }

        }
    }

    fun checkMarketResources(tgtState: GameState)
    {
        gameState.floatingResources.forEach {
            gameState.floatingResources[it.key] = it.value * 999 / 1000
        } //1/1000 of the floating resources is lost

        if ((gameState.marketResources["water"] ?: 0) < gameState.pop)
            println("Less than 12 hours of water out in the market.")
        if ((gameState.marketResources["water"] ?: 0) > gameState.pop / 24)
        {
            gameState.marketResources["water"] = (gameState.marketResources["water"]
                ?: 0) - gameState.pop / 24 //2L/day consumption.
            gameState.floatingResources["water"] = (gameState.floatingResources["water"]
                ?: 0) + gameState.pop / 24 //No water is lost.
        }
        //TODO: adjust consumption rate when resource is low?
        //does not affect approval when resource is low.

        else
        {
            val death = gameState.pop / 100 + 1//Death from dehydration.
            gameState.pickRandomParty.apply {
                causeDeaths(death)
                println("Casualties: at most $death, due to dehydration. Pop left: ${gameState.pop}")
                Information(
                    author = "",
                    creationTime = tgtState.time,
                    type = InformationType.CASUALTY,
                    tgtPlace = "everywhere",
                    amount = death,
                    auxParty = this.name
                ).also { /*spread rumor*/
                    tgtState.informations[it.generateName()] = it //cpy.publicity = 5
                }
            }
        }
        if ((gameState.marketResources["oxygen"] ?: 0) < gameState.pop / 4)
            println("Less than 12 hours of oxygen out in the market.")
        if ((gameState.marketResources["oxygen"] ?: 0) > gameState.pop / 96)
        {
            gameState.marketResources["oxygen"] =
                (gameState.marketResources["oxygen"] ?: 0) - gameState.pop / 96 //0.5kg/day consumption.
            gameState.floatingResources["co2"] = (gameState.floatingResources["co2"]
                ?: 0) + gameState.pop / 64 //Oxygen is converted to CO2.
        } else
        {
            val death =
                gameState.pop / 100 + 1//TODO: adjust deaths. Also, productivity starts to drop when oxygen is low.
            gameState.pickRandomParty.apply {
                causeDeaths(death)
                println("Casualties: at most $death, due to suffocation. Pop left: ${gameState.pop}")
                Information(
                    author = "",
                    creationTime = tgtState.time,
                    type = InformationType.CASUALTY,
                    tgtPlace = "everywhere",
                    amount = death,
                    auxParty = this.name
                ).also { /*spread rumor*/
                    tgtState.informations[it.generateName()] = it //it.publicity = 5
                }
            }
        }
        if ((gameState.marketResources["ration"] ?: 0) < gameState.pop / 2)
            println("Less than 12 hours of ration out in the market.")
        if ((gameState.marketResources["ration"] ?: 0) > gameState.pop / 48)
        {
            gameState.marketResources["ration"] =
                (gameState.marketResources["ration"] ?: 0) - gameState.pop / 48 //1kg/day consumption.
            gameState.floatingResources["water"] = (gameState.floatingResources["water"]
                ?: 0) + gameState.pop / 96 //Ration is converted to water. In carbohydrate, C:H:O = 1:2:1
        } else
        {
            val death = gameState.pop / 100 + 1//TODO: adjust deaths.
            gameState.pickRandomParty.apply {
                causeDeaths(death)
                println("Casualties: at most $death, due to starvation. Pop left: ${gameState.pop}")
                Information(
                    author = "",
                    creationTime = tgtState.time,
                    type = InformationType.CASUALTY,
                    tgtPlace = "everywhere",
                    amount = death,
                    auxParty = this.name
                ).also { /*spread rumor*/
                    tgtState.informations[it.generateName()] = it //cpy.publicity = 5
                }
            }
        }

    }

    fun generateAccidents(tgtState: GameState, tgtPlace: Place)
    {
        //Generate casualties.
        val death = tgtPlace.currentWorker / 100 + 1 //TODO: what about injuries?
        tgtState.parties[tgtPlace.responsibleParty]!!.causeDeaths(death)//TODO: we are assuming that all deaths are from the responsible party.
        Information(
            author = "",
            creationTime = tgtState.time,
            type = InformationType.CASUALTY,
            tgtPlace = tgtPlace.name,
            auxParty = tgtPlace.responsibleParty,
            amount = death
        )/*store info*/.also {
            gameState.informations[it.generateName()] = it
            //Add all people in the place to the known list.
            it.knownTo.addAll(tgtPlace.characters)
            tgtPlace.accidentInformationKeys += it.name
        }

        //Generate resource loss.
        val loss = min(50, tgtPlace.resources["water"] ?: 0)
        tgtPlace.resources["water"] = (tgtPlace.resources["water"] ?: 0) - loss
        Information(
            author = "",
            creationTime = tgtState.time,
            type = InformationType.LOST_RESOURCES,
            tgtPlace = tgtPlace.name,
            resources = hashMapOf("water" to loss)
        )/*store info*/.also {
            gameState.informations[it.generateName()] = it
            //Add all people in the place to the known list.
            it.knownTo.addAll(tgtPlace.characters)
            tgtPlace.accidentInformationKeys += it.name
        }

        //Generate apparatus damage.
        tgtPlace.apparatuses.forEach { app ->
            val tmp = tgtPlace.maxResources
            app.durability -= 30
            if (app.durability <= 0)
            {
                app.durability = 0
                //If storage durability = 0, lose resources.
                if (app.name in listOf(
                        "waterStorage",
                        "oxygenStorage",
                        "lightMetalStorage",
                        "componentStorage",
                        "rationStorage"
                    )
                )
                {
                    //TODO: resources should be stored in storages, not in places.
                    val resourceName = app.name.substring(0, app.name.length - 7)
                    tgtPlace.resources[resourceName] = (tgtPlace.resources[resourceName]
                        ?: 0) * (tgtPlace.maxResources[resourceName] ?: 0) / tmp[resourceName]!!
                    //For example, unbroken storage number 8->7 then lose 1/8 of the resource.
                    //TODO: generate information about the resource loss.
                }

                Information(
                    author = "",
                    creationTime = tgtState.time,
                    type = InformationType.DAMAGED_APPARATUS,
                    tgtPlace = tgtPlace.name,
                    amount = death,
                    tgtApparatus = app.name
                )/*store info*/.also {
                    gameState.informations[it.generateName()] = it
                    //Add all people in the place to the known list.
                    it.knownTo.addAll(tgtPlace.characters)
                    tgtPlace.accidentInformationKeys += it.name
                }
            }
            onAccident.forEach { it(tgtPlace.name, death) }
        }


    }

    fun generateCatastrophicAccidents(tgtState: GameState, tgtPlace: Place)
    {
        //Generate casualties.
        val death = tgtPlace.currentWorker / 5 + 1 //TODO: what about injuries?
        tgtState.parties[tgtPlace.responsibleParty]!!.causeDeaths(death)
        Information(
            author = "",
            creationTime = tgtState.time,
            type = InformationType.CASUALTY,
            tgtPlace = tgtPlace.name,
            auxParty = tgtPlace.responsibleParty,
            amount = death
        )/*store info*/.also {
            gameState.informations[it.generateName()] = it
            //Add all people in the place to the known list.
            it.knownTo.addAll(tgtPlace.characters)
            tgtPlace.accidentInformationKeys += it.name
        }

        //Generate resource loss.
        val loss = min(50, tgtPlace.resources["water"] ?: 0)
        tgtPlace.resources["water"] = (tgtPlace.resources["water"] ?: 0) - loss
        Information(
            author = "",
            creationTime = tgtState.time,
            type = InformationType.LOST_RESOURCES,
            tgtPlace = tgtPlace.name,
            resources = hashMapOf("water" to loss)
        )/*store info*/.also {
            gameState.informations[it.generateName()] = it
            //Add all people in the place to the known list.
            it.knownTo.addAll(tgtPlace.characters)
            tgtPlace.accidentInformationKeys += it.name
        }

        //Generate apparatus damage.
        tgtPlace.apparatuses.forEach { app ->
            val tmp = tgtPlace.maxResources
            app.durability -= 75
            if (app.durability <= 0)
            {
                app.durability = 0
                //If storage durability = 0, lose resources.
                if (app.name in listOf(
                        "waterStorage",
                        "oxygenStorage",
                        "lightMetalStorage",
                        "componentStorage",
                        "rationStorage"
                    )
                )
                {
                    val resourceName = app.name.substring(0, app.name.length - 7)
                    tgtPlace.resources[resourceName] =
                        (tgtPlace.resources[resourceName]
                            ?: 0) * tgtPlace.maxResources[resourceName]!! / tmp[resourceName]!!
                    //For example, unbroken storage number 8->7 then lose 1/8 of the resource.
                    //TODO: generate information about the resource loss.
                }

                Information(
                    author = "",
                    creationTime = tgtState.time,
                    type = InformationType.DAMAGED_APPARATUS,
                    tgtPlace = tgtPlace.name,
                    tgtApparatus = app.name
                )/*store info*/.also {
                    gameState.informations[it.generateName()] = it
                    //Add all people in the place to the known list.
                    it.knownTo.addAll(tgtPlace.characters)
                    tgtPlace.accidentInformationKeys += it.name
                }
            }
        }
        onAccident.forEach { it(tgtPlace.name, death) }
    }


    //TODO: Check for win/lose/interrupt conditions
    fun conditionCheck()
    {
        gameState.characters.forEach { entry ->
            if (entry.value.alive && entry.value.health <= 0)
            {
                println("${entry.value.name} died.")
                //TODO: Do we need to gameState.pop -= 1
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

        if (gameState.time % 720 == 0)
        { //Every 15 days, reset the budget.
            gameState.isBudgetProposed = false
            gameState.isBudgetResolved = false
            //Since the party is division, it pays out the salary of the members.
            gameState.parties.values.filter { it.type == "division" }.forEach { party ->
                party.isSalaryPaid = false
            }
        }
        if (gameState.time % 48 == 0)
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
//                        it.resources["water"] ?: 0
//                    }).also { it.knownTo.add(infraName);gameState.informations[it.generateName()] = it }
//                Information(
//                    infraName,
//                    creationTime = gameState.time,
//                    tgtTime = gameState.time,
//                    type = "resource",
//                    tgtResource = "oxygen",
//                    tgtPlace = "everywhere",
//                    amount = gameState.places.values.sumOf {
//                        it.resources["oxygen"] ?: 0
//                    }).also { it.knownTo.add(infraName);gameState.informations[it.generateName()] = it }
//                Information(
//                    infraName,
//                    creationTime = gameState.time,
//                    tgtTime = gameState.time,
//                    type = "resource",
//                    tgtResource = "ration",
//                    tgtPlace = "everywhere",
//                    amount = gameState.places.values.sumOf {
//                        it.resources["ration"] ?: 0
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

        fun isShortOfResources(app: Apparatus, place: Place): String
        {
            app.currentConsumption.forEach {
                if ((place.resources[it.key] ?: 0) < it.value)
                    return it.key //If the resource is less than a unit time worth of consumption, return the resource name.
            }
            //Distribution does not consume resource.
            /*app.currentDistribution.forEach {
                when(it.key){
                    "water"->if((place.resources[it.key]?:0)<it.value+ (app.currentConsumption["water"]?:0)) return "water"
                    "oxygen"->if((place.resources[it.key]?:0)<it.value+ (app.currentConsumption["oxygen"]?:0)) return "oxygen"
                    "ration"->if((place.resources[it.key]?:0)<it.value+ (app.currentConsumption["ration"]?:0)) return "ration"
                }
            }*/
            return ""

        }

        fun isShortOfAbsorbableResources(app: Apparatus, place: Place, gameState: GameState): String
        {
            app.currentAbsorption.forEach {
                if ((gameState.floatingResources[it.key] ?: 0) < it.value)
                    return it.key //If the resource is less than a unit time worth of consumption, return the resource name.
            }
            //Distribution does not consume resource.
            /*app.currentDistribution.forEach {
                when(it.key){
                    "water"->if((place.resources[it.key]?:0)<it.value+ (app.currentConsumption["water"]?:0)) return "water"
                    "oxygen"->if((place.resources[it.key]?:0)<it.value+ (app.currentConsumption["oxygen"]?:0)) return "oxygen"
                    "ration"->if((place.resources[it.key]?:0)<it.value+ (app.currentConsumption["ration"]?:0)) return "ration"
                }
            }*/
            return ""

        }

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
                            println("Acquire failed.")
                            println("Wanted type: ${T::class}")
                            println("Acquired type: ${x::class}")
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
            if (gameState.ongoingMeetings.any { it.value.currentCharacters.contains(character) })
            {
                val meeting = gameState.ongoingMeetings.filter {
                    it.value.currentCharacters.contains(
                        character
                    )
                }.values.first()
                if (character == gameState.playerName)
                {
                    println("You are in a meeting.")
                    println("Attendees: ${meeting.currentCharacters}")
                }
                if (meeting.currentSpeaker == character)
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

//                if (gameState.parties.values.any { it.leader == character && it.members.containsAll(meeting.currentCharacters) })//Only the leader of a party can command.
//                {
//                    //actions.add("UnofficialCommand") UnofficialCommand is gone. Command is always official. Trade can be used for unofficial commands.
//                    if (meeting.currentCharacters.count() >= 3)
//                        actions.add("InfoRequest")
//                }
                //actions.add("AppointMeeting") AppointMeeting is an agenda now.
                //actions.add("Wait")
                actions.add("UseItem")
                actions.add("LeaveMeeting")
                return actions
            }
            if (gameState.ongoingMeetings.any { it.value.currentCharacters.contains(character) })
            {
                val conf = gameState.ongoingMeetings.filter {
                    it.value.currentCharacters.contains(
                        character
                    )
                }.values.first()
                if (character == gameState.playerName)
                {
                    println("You are in a conference.")
                    println(
                        "Attendees: ${
                            conf.currentCharacters
                        }"
                    )
                }
                val subject = conf.type
                if (subject == "informal")
                {
                } else
                    if (character == gameState.parties[conf.involvedParty]!!.leader)//Only the leader can do below actions.
                    {
                        actions.add("Resign") //Only leaders can resign right now. Resign is one of the few actions that can be done without an agenda.
                        if (subject == "divisionDailyConference" && !gameState.parties[conf.involvedParty]!!.isSalaryPaid)
                            actions.add("Salary") //Salary is distributed in a divisionDailyConference.
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
            if (gameState.places[place]!!.characters.count() > 1)
                actions.add("Talk")
            if (gameState.places[place]!!.isAccidentScene)
            {
                if (gameState.places[place]!!.responsibleParty != "" && gameState.parties[gameState.places[place]!!.responsibleParty]!!.members.contains(
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
            if (gameState.places[place]!!.responsibleParty != "" && gameState.parties[gameState.places[place]!!.responsibleParty]!!.members.contains(
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
                gameState.scheduledMeetings.filter { it.value.time + 2 > gameState.time && gameState.time + 2 > it.value.time && it.value.place == place }
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


        //Agreement change is computed every turn based on deltaAgreement, rather than changing once when information are added.
        //This is to prevent the meeting going nowhere when there isn't enough supporting information.
        fun progressMeeting(gameState: GameState, mt: Meeting)
        {
            mt.agendas.forEach { agenda ->


            }
        }


    }
}