package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.LogUI
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log
import kotlin.math.max
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
        gameState.updateUI.forEach { it(gameState) }//Update UI

        //Main loop
        while (true)
        {
            gameState.characters.values.forEach {
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
                            if (it.hunger > 90) it.health -= 10 / (101 - it.hunger)
                            if (it.thirst > 70) it.health -= 30 / (101 - it.thirst)
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
            if (gameState.time % 48 == 0)//Every day
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
        if (char.name == gameState.playerAgent)
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
                        gameState.places.values.find {
                            it.characters.contains(
                                char.name
                            )
                        }!!.name
                    }, time=${gameState.time}, which is not in the action list. This may be a bug."
                )
            if (!action.isValid())
                println(
                    "Warning: Non player character ${char.name} is performing ${action.javaClass.simpleName} at ${
                        gameState.places.values.find {
                            it.characters.contains(
                                char.name
                            )
                        }!!.name
                    }, time=${gameState.time}, which is not valid. This may be a bug."
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
            "",
            char.name,
            creationTime = gameState.time,
            type = "action",
            tgtTime = gameState.time,
            tgtPlace = place,
            tgtCharacter = char.name,
            action = action.javaClass.simpleName
        ).also {
            gameState.informations[it.generateName()] = it
        }
        action.execute()

    }

    //This function is called at the end of each turn, after all the characters have performed their actions.
    fun progression()
    {
        gameState.time += 1
        distributePopulation()
        distributeResources()
        calculateMutuality()
        workAppratuses()
        conditionCheck()
        checkMarketResources(gameState)
        ageInformation()
        spreadPublicInfo()

        //println("Time: ${gameState.time}")
        //println("My approval:${gameState.characters[gameState.playerAgent]!!.approval}")
        gameState.updateUI.forEach { it(gameState) }
    }

    //TODO: Normalization of mutuality is not implemented yet.
    private fun calculateMutuality()
    {
        //Party mutualities are correlated; if a is friendly to b, a is also friendly to b's friends and hostile to b's enemies. If a is hostile to b, a is also hostile to b's friends and friendly to b's enemies.
        gameState.parties.keys.forEach { a ->
            gameState.parties.keys.forEach { b ->
                var factora = 0.0
                var factorb = 0.0
                gameState.parties[a]!!.members.forEach { c ->
                    gameState.parties[b]!!.members.forEach { d ->
                        factora += gameState.getMutuality(c, d) - 50
                        factorb += gameState.getMutuality(d, c) - 50
                    }
                }
                val sizea = gameState.parties[a]!!.members.size + gameState.parties[a]!!.anonymousMembers
                val sizeb = gameState.parties[b]!!.members.size + gameState.parties[b]!!.anonymousMembers
                gameState.setPartyMutuality(a, b, factora / sizea / sizeb)
                gameState.setPartyMutuality(b, a, factorb / sizea / sizeb)

            }
        }

        //Individual mutualities are correlated with their parties, basically the reciprocation of the above.
        gameState.characters.keys.forEach { a ->
            gameState.characters.keys.forEach { b ->
                var factora = 0.0
                var factorb = 0.0
                gameState.parties.filter { it.value.members.contains(a) }.forEach { c ->
                    gameState.parties.filter { it.value.members.contains(b) }.forEach { d ->
                        factora += gameState.getPartyMutuality(c.key, d.key) - 50
                        factorb += gameState.getPartyMutuality(d.key, c.key) - 50
                    }
                }
                //Make sure that denominator is not 0.
                val sizea = gameState.parties.filter { it.value.members.contains(a) }.count() + 1
                val sizeb = gameState.parties.filter { it.value.members.contains(b) }.count() + 1
                gameState.setMutuality(a, b, factora / sizea / sizeb)
                gameState.setMutuality(b, a, factorb / sizea / sizeb)

            }
        }


        //If there are meetings where some characters are missing, all the characters in the meeting lose mutuality toward the missing characters.
        gameState.ongoingConferences.forEach { conference ->
            conference.value.scheduledCharacters.forEach { char ->
                if (!conference.value.currentCharacters.contains(char))
                    conference.value.currentCharacters.forEach { char2 ->
                        gameState.setMutuality(char, char2, -1.0)
                    }
            }
        }
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
        //spread information within each party, if known.
        gameState.informations.values.forEach { information ->
            information.publicity.forEach {
                //Spread information only if the information is known to the party.
                if (it.value > 0) information.publicity[it.key] = it.value + 1
                if (it.value > 100) information.publicity[it.key] = 100
            }


        }

        gameState.parties.forEach { party ->
            val l = gameState.informations.filter { (it.value.publicity[party.key] ?: 0) > 0 }.toList()
            //incompatible information within the party and its member fight each other.

            for (i in gameState.informations.filter { it.value.knownTo.intersect(party.value.members).isNotEmpty() })
                for (j in 0 until l.count())
                {
                    val a = i.value
                    val b = l[j].second

                    if (a.compatibility(b) == 0.0)//If the two information are incompatible
                    {
                        val aStrength =
                            if (a.author == "") 5000.0 /*rumor has fixed strength*/ else
                                a.credibility * (party.value.individualMutuality(a.author) + a.supporters.sumOf {
                                    party.value.individualMutuality(
                                        it
                                    )
                                } / 2/*supporter penalty*/)
                        val bStrength =
                            if (b.author == "") 5000.0 /*rumor has fixed strength*/ else
                                b.credibility * (party.value.individualMutuality(b.author) + b.supporters.sumOf {
                                    party.value.individualMutuality(
                                        it
                                    )
                                } / 2/*supporter penalty*/)
                        if (aStrength < bStrength)
                        {
                            //The party information might affect individual information if the party information is stronger. The converse is not true.
                            a.publicity.keys.forEach {
                                b.publicity[it] = max(
                                    (b.publicity[it]
                                        ?: 1) - 1, 0
                                )//Publicity of the weaker information drops. The minimum is 0.
                            }
                            if (gameState.characters[b.author] != null)
                            //This character has decreased mutuality toward b.author and b.supporters.
                                a.knownTo.intersect(party.value.members).forEach {
                                    gameState.setMutuality(it, b.author, -1.0)
                                    b.supporters.forEach { supporter ->
                                        gameState.setMutuality(
                                            it,
                                            supporter,
                                            -1.0
                                        )
                                    }
                                }
                        }
                    }
                }
            //incompatible information within the same party fight each other
            if (l.count() > 1)
            {
                for (i in 0 until l.count())
                    for (j in 0 until l.count())
                    {
                        val a = l[i].second
                        val b = l[j].second

                        if (a.compatibility(b) == 0.0)//If the two information are incompatible
                        {
                            val aStrength =
                                if (a.author == "") 5000.0 /*rumor has fixed strength*/ else
                                    a.credibility * (party.value.individualMutuality(a.author) + a.supporters.sumOf {
                                        party.value.individualMutuality(
                                            it
                                        )
                                    } / 2/*supporter penalty*/)
                            val bStrength =
                                if (b.author == "") 5000.0 /*rumor has fixed strength*/ else
                                    b.credibility * (party.value.individualMutuality(b.author) + b.supporters.sumOf {
                                        party.value.individualMutuality(
                                            it
                                        )
                                    } / 2/*supporter penalty*/)
                            if (aStrength > bStrength)
                            {
                                //Fight within each party
                                a.publicity.keys.forEach {
                                    b.publicity[it] = max(
                                        (b.publicity[it]
                                            ?: 1) - 1, 0
                                    )//Publicity of the weaker information drops. The minimum is 0.
                                }
                                if (gameState.characters[b.author] != null)
                                //This party has decreased party mutuality toward b.author and b.supporters parties. Maybe amount proportional to the party size?
                                    gameState.parties.filter { party2 ->
                                        party2.value.members.contains(b.author) or party2.value.members.any { m ->
                                            b.supporters.contains(
                                                m
                                            )
                                        }
                                    }.forEach {
                                        gameState.setPartyMutuality(party.key, it.key, -1.0)
                                    }
                                //Individual opinions are not directly affected by the party information.


                            }

                        }
                    }


            }

            //TODO: similar information merge into one. Do we really need this feature?

            //bad news affect the approval. casualty, stolen resource, TODO: low water ration oxygen, high wealth, crimes
            gameState.informations.filter { it.value.type == "casualty" }.forEach {
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
                        -it.value.amount * (it.value.publicity[party.key]
                            ?: 0) * factor / 1000
                    )
                //if our party is responsible, integrity drops.

            }
            gameState.informations.filter { it.value.type == "action" && it.value.action == "unofficialResourceTransfer" }
                .forEach {
                    var factor = 1
                    if (it.value.author == "") factor = 2//rumors affect the approval negatively.

                    //party loses mutuality toward the responsible party. TODO: consider affecting the individual mutuality toward the perpetrator.
                    gameState.setPartyMutuality(
                        party.key, gameState.places[it.value.tgtPlace]!!.responsibleParty, -log(
                            it.value.amount.toDouble() + 1, 2.0
                        ) * (it.value.publicity[party.key] ?: 0) / 100 * factor
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
                val tmp = it.size - targetSize
                it.anonymousMembers -= tmp
                gameState.idlePop += tmp
                if (it.anonymousMembers < 0) it.anonymousMembers = 0
            }
        }

    }

    fun scheduleDailyConferences()
    {
        //Each division has a conference every day. The conference is attended by the head of the division and the members of the division.
        gameState.parties.values.filter { it.type == "division" }.forEach { party ->
            val conference = Meeting(
                gameState.time + 18 /*9 in the morning*/,
                "divisionDailyConference",
                place = party.home,
                scheduledCharacters = party.members
            ).also { it.involvedParty = party.name }

            gameState.scheduledConferences["conference-${party.home}-${party.name}-${gameState.time}"] = conference
        }

        //If some of the division leaders are not assigned, a conference is scheduled to assign them.
        gameState.parties.filter { it.value.type == "division" && it.value.leader == "" }.forEach { stringPartyEntry ->
            val conference = Meeting(
                gameState.time + 18 /*9 in the morning*/,
                "leaderAssignment",
                place = stringPartyEntry.value.home,
                scheduledCharacters = stringPartyEntry.value.members
            ).also { it.auxSubject = stringPartyEntry.value.name }

            gameState.scheduledConferences["conference-${stringPartyEntry.value.home}-${stringPartyEntry.value.name}-${gameState.time}"] =
                conference
        }
        //Since the party is division, it pays out the salary of the members.
        gameState.parties.values.filter { it.type == "division" }.forEach { party ->
            party.members.forEach { char -> party.isDailySalaryPaid[char] = false }
        }
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
                    type = "casualty",
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
                    type = "casualty",
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
                    type = "casualty",
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
            type = "casualty",
            tgtPlace = tgtPlace.name,
            auxParty = tgtPlace.responsibleParty,
            amount = death
        )/*store dummy info*/.also { tgtPlace.accidentInformations[it.generateName()] = it }.also { /*spread rumor*/
            val cpy = Information(it); tgtState.informations[cpy.generateName()] =
                cpy; cpy.publicity[tgtPlace.responsibleParty] = 5
        }
            .also { /*copy this information to the responsible character.*/
                if (gameState.parties[tgtPlace.responsibleParty]!!.leader != "")
                {
                    val cpy = Information(it)
                    cpy.author = gameState.parties[tgtPlace.responsibleParty]!!.leader
                    tgtState.informations[cpy.generateName()] = cpy
                    cpy.publicity[tgtPlace.responsibleParty] = 0
                }
            }

        //Generate resource loss.
        val loss = min(50, tgtPlace.resources["water"] ?: 0)
        tgtPlace.resources["water"] = (tgtPlace.resources["water"] ?: 0) - loss
        Information(
            author = "",
            creationTime = tgtState.time,
            type = "lostResource",
            tgtPlace = tgtPlace.name,
            resources = hashMapOf("water" to loss)
        )/*store dummy info*/.also { tgtPlace.accidentInformations[it.generateName()] = it }.also { /*spread rumor*/
            val cpy = Information(it); tgtState.informations[cpy.generateName()] =
                cpy; cpy.publicity[tgtPlace.responsibleParty] = 5
        }
            .also { /*copy this information to the responsible character.*/
                if (gameState.parties[tgtPlace.responsibleParty]!!.leader != "")
                {
                    val cpy = Information(it)
                    cpy.author = gameState.parties[tgtPlace.responsibleParty]!!.leader
                    tgtState.informations[cpy.generateName()] = cpy
                    cpy.publicity[tgtPlace.responsibleParty] = 0
                }
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
                    type = "damagedApparatus",
                    tgtPlace = tgtPlace.name,
                    amount = death,
                    tgtApparatus = app.name
                )/*store dummy info*/.also { tgtPlace.accidentInformations[it.generateName()] = it }
                    .also { /*spread rumor*/
                        val cpy = Information(it); tgtState.informations[cpy.generateName()] =
                            cpy; cpy.publicity[tgtPlace.responsibleParty] = 5
                    }
                    .also { /*copy this information to the responsible character.*/
                        if (gameState.parties[tgtPlace.responsibleParty]!!.leader != "")
                        {
                            val cpy = Information(it)
                            cpy.author =
                                gameState.parties[tgtPlace.responsibleParty]!!.leader
                            tgtState.informations[cpy.generateName()] =
                                cpy
                            cpy.publicity[tgtPlace.responsibleParty] = 0
                        }
                    }
            }
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
            type = "casualty",
            tgtPlace = tgtPlace.name,
            auxParty = tgtPlace.responsibleParty,
            amount = death
        ).also { tgtPlace.accidentInformations[it.generateName()] = it }.also { /*spread rumor*/
            val cpy = Information(it); tgtState.informations[cpy.generateName()] =
                cpy; cpy.publicity[tgtPlace.responsibleParty] = 75
        }
            .also { information -> /*copy this information to the responsible character.*/
                if (gameState.parties[tgtPlace.responsibleParty]!!.leader != "")
                {
                    val cpy = Information(information)
                    cpy.author =
                        gameState.parties[tgtPlace.responsibleParty]!!.leader
                    tgtState.informations[cpy.generateName()] =
                        cpy
                    cpy.publicity[tgtPlace.responsibleParty] = 0
                }
            }

        //Generate resource loss.
        val loss = min(50, tgtPlace.resources["water"] ?: 0)
        tgtPlace.resources["water"] = (tgtPlace.resources["water"] ?: 0) - loss
        Information(
            author = "",
            creationTime = tgtState.time,
            type = "lostResource",
            tgtPlace = tgtPlace.name,
            resources = hashMapOf("water" to loss)
        ).also { tgtPlace.accidentInformations[it.generateName()] = it }.also { /*spread rumor*/
            val cpy = Information(it); tgtState.informations[cpy.generateName()] =
                cpy; cpy.publicity[tgtPlace.responsibleParty] = 75
        }
            .also { /*copy this information to the responsible character.*/
                if (gameState.parties[tgtPlace.responsibleParty]!!.leader != "")
                {
                    val cpy = Information(it)
                    cpy.author =
                        gameState.parties[tgtPlace.responsibleParty]!!.leader
                    tgtState.informations[cpy.generateName()] =
                        cpy
                    cpy.publicity[tgtPlace.responsibleParty] = 0
                }
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
                    type = "damagedApparatus",
                    tgtPlace = tgtPlace.name,
                    tgtApparatus = app.name
                )/*store dummy info*/.also { tgtPlace.accidentInformations[it.generateName()] = it }
                    .also { /*spread rumor*/
                        val cpy = Information(it); tgtState.informations[cpy.generateName()] =
                            cpy; cpy.publicity[tgtPlace.responsibleParty] = 75
                    }
                    .also { /*copy this information to the responsible character.*/
                        if (gameState.parties[tgtPlace.responsibleParty]!!.leader != "")
                        {
                            val cpy = Information(it)
                            cpy.author =
                                gameState.parties[tgtPlace.responsibleParty]!!.leader
                            tgtState.informations[cpy.generateName()] =
                                cpy
                            cpy.publicity[tgtPlace.responsibleParty] = 0
                        }
                    }
            }
        }
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
        if (!l.contains(gameState.playerAgent))
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
                if (character == gameState.playerAgent)
                {
                    println("You are in a meeting.")
                    println("Attendees: ${meeting.currentCharacters}")
                }
                actions.add("chat")
                //Trade is allowed only if there are exactly two people in the meeting.
                //InfoShare is allowed only if there are more than two people in the meeting.
                if (meeting.currentCharacters.count() == 2)
                    actions.add("trade")
                else
                    actions.add("infoShare")

                if (gameState.parties.values.any { it.leader == character && it.members.containsAll(meeting.currentCharacters) })//Only the leader of a party can command.
                {
                    actions.add("unofficialCommand")
                    if (meeting.currentCharacters.count() >= 3)
                        actions.add("infoRequest")
                }
                actions.add("appointMeeting")
                actions.add("wait")
                actions.add("leaveMeeting")
                return actions
            }
            if (gameState.ongoingConferences.any { it.value.currentCharacters.contains(character) })
            {
                val conf = gameState.ongoingConferences.filter {
                    it.value.currentCharacters.contains(
                        character
                    )
                }.values.first()
                if (character == gameState.playerAgent)
                {
                    println("You are in a conference.")
                    println(
                        "Attendees: ${
                            conf.currentCharacters
                        }"
                    )
                }
                //You cannot trade in a conference.
                val subject = conf.subject
                if (character == gameState.parties[conf.involvedParty]!!.leader)//Only the leader can do below actions.
                {
                    when (subject)
                    {
                        "budgetProposal" -> if (!gameState.isBudgetProposed) actions.add("budgetProposal")
                        "budgetResolution" -> if (!gameState.isBudgetResolved) actions.add("budgetResolution")
                        "leaderAssignment" -> if (gameState.parties[conf.auxSubject]!!.leader == "") actions.add("leaderAssignment")

                    }
                    actions.add("resign") //Only leaders can resign right now.
                }
                //When not the leader, you can only do below actions.
                when (subject)
                {
                    "divisionDailyConference" -> if (gameState.parties[conf.involvedParty]!!.isDailySalaryPaid[character] == false) actions.add(
                        "salary"
                    )
                }
                //Command is allowed only if the character is the division leader.
                if (gameState.parties[conf.involvedParty]?.leader == character)
                {
                    actions.add("command")
                    actions.add("infoRequest")
                }
                actions.add("infoShare")
                actions.add("appointMeeting")
                actions.add("wait")
                actions.add("leaveConference")
                return actions
            }
            if (place != "home" && gameState.places[place]!!.characters.count() > 1)
                actions.add("talk")
            if (gameState.places[place]!!.isAccidentScene)
            {
                if (gameState.places[place]!!.responsibleParty != "" && gameState.parties[gameState.places[place]!!.responsibleParty]!!.members.contains(
                        character
                    )
                )//Only the responsible party members can clear the accident scene.
                    actions.add("clearAccidentScene")
                actions.add("investigateAccidentScene")
            }
            actions.add("move")
            actions.add("examine")
            //actions.add("radio")
            actions.add("wait")
            if (place == "home")
            {
                actions.add("sleep")
                actions.add("eat")
            }
            if (place == gameState.characters[character]!!.home)
            {
                actions.add("home")
            }
            val availableConferences =
                gameState.scheduledConferences.filter { it.value.time + 2 > gameState.time && gameState.time + 2 > it.value.time && it.value.place == place }
                    .filter { !gameState.ongoingMeetings.containsKey(it.key) }
            if (availableConferences.isNotEmpty())
                if (gameState.parties[availableConferences.values.first().involvedParty]!!.leader == character)//Only the party leader can do below actions.
                {
                    actions.add("startConference")
                }
            val ongoingConferences = gameState.ongoingConferences.filter {
                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(character) && it.value.place == place
            }
            if (ongoingConferences.isNotEmpty())
                actions.add("joinConference")

            if (place == "mainControlRoom" || place == "market" || place == "squareNorth" || place == "squareSouth")
            {
                actions.add("infoAnnounce")
            }
            if (gameState.places[place]!!.responsibleParty != "" && gameState.parties[gameState.places[place]!!.responsibleParty]!!.members.contains(
                    character
                )
            )
            {
                actions.add("unofficialResourceTransfer")//can only steal from their own division.
                actions.add("officialResourceTransfer")//can only move resources from their own division.
            }
            val availableMeetings =
                gameState.scheduledMeetings.filter { it.value.time + 2 > gameState.time && gameState.time + 2 > it.value.time && it.value.place == place }
                    .filter { !gameState.ongoingMeetings.containsKey(it.key) }
                    .filter { it.value.scheduledCharacters.contains(character) }
            if (availableMeetings.isNotEmpty())
                actions.add("startMeeting")
            val meetingsToJoin = gameState.ongoingMeetings.filter {
                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(character) && it.value.place == place
            }
            if (meetingsToJoin.isNotEmpty())
            {
                val subject = gameState.ongoingMeetings.firstNotNullOf { entry ->
                    entry.value.subject.takeIf {
                        entry.value.scheduledCharacters.contains(character) && !entry.value.currentCharacters.contains(
                            character
                        ) && entry.value.place == place
                    }
                }
                if (gameState.playerAgent == character)
                {
                    when (subject)
                    {
                        "talk" -> println("Someone wants to talk to you.")//TODO: there must be a way to know NPC's intention to talk
                    }
                }
                actions.add("joinMeeting")
            }
            if (gameState.characters[character]!!.trait.contains("technician") && place != "home")
            {
                actions.add("repair")
            }
            return actions
        }
    }
}