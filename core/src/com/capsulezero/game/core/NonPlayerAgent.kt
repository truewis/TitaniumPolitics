package com.capsulezero.game.core

import com.capsulezero.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
class NonPlayerAgent(val character: String) : GameStateElement() {
    //TODO: implement pathfinding. For now, just teleport to the place
    var commands = arrayListOf<Command>()
    private var routines = arrayListOf<Routine>()//Routines are sorted by priority. 0th element is the current routine.
    val place
    get() = parent.places.values.find { it.characters.contains(character) }!!.name

    fun chooseAction(): GameAction {
        //1. High priority routine change
        //---------------------------------------------------------------------------------------------------------
        selectRoutine()
        //2. Execute action according to the current routine.--------------------------------------------------------------------------------------------------------
        return executeRoutine()
    }

    private fun selectRoutine() {
        //If there is almost no food or water, stop all activities and try to get some. ----------------------------------------------------------------------------
        if ((parent.characters[character]!!.resources["ration"]
                ?: 0) <= (parent.characters[character]!!.reliants.size + 1) || (parent.characters[character]!!.resources["water"]
                ?: 0) <= (parent.characters[character]!!.reliants.size + 1)
        ) {
            val wantedResource = if ((parent.characters[character]!!.resources["ration"]
                    ?: 0) <= (parent.characters[character]!!.reliants.size + 1)
            ) "ration" else "water"
            if (parent.characters[character]!!.trait.contains("thief")) {
                //Find a place within my division with maximum res.
                if(routines.none{it.name=="steal"})
                    routines.add(Routine("steal", 100).also { it.variables["stealResource"] = wantedResource })//Add a routine, priority higher than work.

            } else if (parent.characters[character]!!.trait.contains("bargainer")) {
                if(routines.none{it.name=="steal"})
                    routines.add(Routine("buy", 100).also { it.variables["wantedResource"] = wantedResource })//Add a routine, priority higher than work.
            }
        }


    }


    private fun executeRoutine(): GameAction {
        if (routines.isEmpty()) {
            whenIdle()
            if (routines.isEmpty())
                return wait(parent, character, place)
        }
        routines.sortByDescending { it.priority }
        when (routines[0].name) {//Execute action by the current routine
            "rest" -> {
                if (place != "home") {
                    routines.add(Routine("move", routines[0].priority + 10).also {
                        it.variables["movePlace"] = "home"
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                }
                if (parent.characters[character]!!.hunger > 50 || parent.characters[character]!!.thirst > 50)
                    return eat(parent, character, place)
                when (parent.characters[character]!!.health) {
                    in 0..40 -> return sleep(parent, character, place)
                    else ->
                        if (parent.hour in 9..17) {
                            routines.removeAt(0)//Remove the current routine.
                            routines.add(Routine("work", 0))
                            return executeRoutine()
                        } else {
                            return wait(parent, character, place)
                        }
                }
            }

            "move" -> {
                if (place == routines[0].variables["movePlace"]) {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else
                    return move(parent, character, place).also { it.placeTo = routines[0].variables["movePlace"]!! }
            }

            "steal" -> {
                val resplace =
                    parent.places.values.filter {
                        it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                            character
                        )
                    }
                        .maxByOrNull { it.resources["ration"] ?: 0 }
                        ?: parent.places.values.filter {
                            it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                                character
                            )
                        }
                            .maxByOrNull { it.resources["water"] ?: 0 }
                if (resplace == null) {
                    //Stop stealing because there is no place to steal from.
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else {
                    if (place != resplace.name) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = resplace.name
                        })//Add a move routine with higher priority.
                    } else {
                        //Finish stealing

                        unofficialResourceTransfer(parent, character, place).also {
                            it.what = routines[0].variables["stealResource"]!!
                            it.amount = min(
                                (resplace.resources["ration"] ?: 0) / 2,
                                (parent.characters[character]!!.reliants.size + 1) * 7
                            )
                            println("$character is stealing ${it.what} from ${resplace.name}: ${it.amount}")
                            routines.removeAt(0)//Remove the current routine.
                            return it
                        }


                    }
                }


            }

            "work" -> {
                //If an accident happened in the place of my control, investigate and clear it.
                parent.places.values.filter {
                    it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                        character
                    )
                }.forEach { place1 ->
                    if (place1.isAccidentScene) {
                        if (place != place1.name) {
                            routines.add(Routine("move", routines[0].priority + 10).also {
                                it.variables["movePlace"] = place1.name
                            })//Add a move routine with higher priority.
                            return executeRoutine()
                        } else
                        //TODO: implement investigateAccidentScene. Right now the information is immediately known to the division leader.

                            clearAccidentScene(parent, character, place).also {
                                return it
                            }
                    }
                }
                //If in a conference or a meeting, wait for it to end.
                if (parent.ongoingConferences.any {
                        it.value.scheduledCharacters.contains(character) && it.value.currentCharacters.contains(
                            character
                        )
                    })//If in a conference
                {
                    return wait(parent, character, place)
                }
                if (parent.ongoingMeetings.any {
                        it.value.scheduledCharacters.contains(character) && it.value.currentCharacters.contains(
                            character
                        )
                    })//If in a meeting
                {
                    return wait(parent, character, place)
                }
                //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest.
                if (parent.hour !in 9..17) {
                    routines.removeAt(0)//Remove the current routine.
                    routines.add(Routine("rest", 0))
                    return executeRoutine()
                }
                if(parent.characters[character]!!.health < 30 || parent.characters[character]!!.hunger > 80 || parent.characters[character]!!.thirst > 60)
                {
                    routines.removeAt(0)//Remove the current routine.
                    routines.add(Routine("rest", 50)) //Rest with higher priority.
                    return executeRoutine()
                }
                //If there is a command, execute it.
                if (commands.isNotEmpty()) {
                    val command = commands.first()
                    if (place != command.place)
                        return move(parent, character, place).also { it.placeTo = command.place }
                    else {
                        commands.removeAt(0)
                        routines.add(
                            Routine(
                                command.action,
                                routines[0].priority + 10
                            )
                        )//Add an action routine with higher priority.
                        return executeRoutine()
                    }
                }
                if (parent.ongoingConferences.any {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                            character
                        )
                    })//If missed a conference
                {
                    val conferencePlace = parent.ongoingConferences.filter {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                            character
                        )
                    }.values.first().place
                    //----------------------------------------------------------------------------------Move to the conference
                    if (place != conferencePlace) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = conferencePlace
                        })//Add a move routine with higher priority.
                        executeRoutine()
                    } else
                        joinConference(parent, character, place).also {
                            it.meetingName = parent.ongoingConferences.filter {
                                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                    character
                                )
                            }.keys.first()
                            return it
                        }
                    //----------------------------------------------------------------------------------Move to the conference
                }
                if (parent.scheduledConferences.any { it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3 })//If a conference is soon
                {
                    val conf = parent.scheduledConferences.filter {
                        it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3
                    }.values.first()
                    //----------------------------------------------------------------------------------Move to the conference
                    if (place != conf.place) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = conf.place
                        })//Add a move routine with higher priority.
                        executeRoutine()
                    } else {
                        //if this character is the leader, start the conference.
                        if (parent.parties[conf.involvedParty]!!.leader == character) {
                            startConference(parent, character, place).also { action ->
                                action.meetingName =
                                    parent.scheduledConferences.keys.first { parent.scheduledConferences[it] == conf }
                                return action
                            }
                        } else {
                            return wait(
                                parent,
                                character,
                                place
                            )//Wait for the conference to start.
                        }

                    }
                    //----------------------------------------------------------------------------------Move to the conference
                }
                //If missed a meeting
                if (parent.ongoingMeetings.any {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                            character
                        )
                    }) {
                    //----------------------------------------------------------------------------------Move to the meeting
                    val meetingPlace = parent.ongoingMeetings.filter {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                            character
                        )
                    }.values.first().place
                    if (place != meetingPlace) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = meetingPlace
                        })//Add a move routine with higher priority.
                        executeRoutine()
                    } else
                        joinMeeting(parent, character, place).also {
                            it.meetingName = parent.ongoingMeetings.filter {
                                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                    character
                                )
                            }.keys.first()
                            return it
                        }
                    //----------------------------------------------------------------------------------Move to the meeting
                }
                if (parent.scheduledMeetings.any { it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3 })//If a meeting is soon
                {
                    //----------------------------------------------------------------------------------Move to the meeting
                    val meeting = parent.scheduledMeetings.filter {
                        it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3
                    }.values.first()
                    if (place != meeting.place) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = meeting.place
                        })//Add a move routine with higher priority.
                        executeRoutine()
                    } else //If there is no meeting yet, create one. Concurrent meetings do not happen, as the meeting is created immediately only if there is no meeting in the place.
                        startMeeting(parent, character, place).also { action ->
                            action.meetingName =
                                parent.scheduledMeetings.filter { it.value == meeting }.keys.first()
                            return action
                        }
                    //----------------------------------------------------------------------------------Move to the meeting
                }
                //If a place in the map is short of resources, transfer resources to it.
                parent.places.values.forEach fe@{ place1 ->
                    place1.apparatuses.forEach { apparatus ->
                        val res = GameEngine.isShortOfResources(apparatus, place1) //Type of resource that is short of.
                        if (res != "") {
                            //Find a place within my division with maximum res.
                            val resplace =
                                parent.places.values.filter {
                                    it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                                        character
                                    )
                                }
                                    .maxByOrNull { it.resources[res] ?: 0 }
                                    ?: return@fe
                            if (place != resplace.name) {
                                routines.add(
                                    Routine(
                                        "move",
                                        routines[0].priority + 10
                                    ).also {
                                        it.variables["movePlace"] = resplace.name
                                    })//Add a move routine with higher priority.
                                executeRoutine()
                            } else {
                                officialResourceTransfer(parent, character, place).also {
                                    it.what = res
                                    it.toWhere = place1.name
                                    it.amount = (resplace.resources[res] ?: 0) / 2
                                    return it
                                }
                            }
                        }
                    }

                }
                //If the character is at workplace, wait.
                if ((parent.places[place]!!.responsibleParty != "" && parent.parties[parent.places[place]!!.responsibleParty]!!.members.contains(
                        character
                    ))
                ) {
                    return wait(parent, character, place)
                } else
                //Move to a random place such that the responsible party is one of the parties of the character.
                {
                    return try {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = parent.places.values.filter {
                                it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                                    character
                                )
                            }.random().name
                        })//Add a move routine with higher priority.
                        executeRoutine()

                    } catch (e: Exception) {
                        println("Warning: No place to commute found for $character.")
                        return wait(parent, character, place)
                    }

                }
            }

            "findCharacter" -> {
                //Stop if the character is at the same place
                if (place == parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name) {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else

                //Move to findCharacter if the character is not at home
                {
                    if(parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name=="home")
                        return wait(parent, character, place)

                    routines.add(Routine("move", routines[0].priority + 10).also {
                        it.variables["movePlace"] =
                            parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                }


            }

            "buy" -> {
                //Try to trade for resources
                //Select a character to trade with, based on the information known to the character.
                val tradeCharacter: String
                val info = parent.informations.values.filter {
                    it.type == "resource" && it.tgtCharacter != "" && it.tgtCharacter != character && it.tgtResource == routines[0].variables["wantedResource"] && it.amount > 10 && it.knownTo.contains(
                        character
                    )
                }
                tradeCharacter = if (info.isNotEmpty()) {//If this character knows a character with the resource
                    info.random().tgtCharacter
                } else
                    parent.characters.keys.filter { it != character }.random()

                //FindCharacter
                // if the character is not in the same place.
                if (place != parent.places.values.find { it.characters.contains(tradeCharacter) }!!.name) {
                    routines.add(Routine("findCharacter", routines[0].priority + 10).also {
                        it.variables["character"] = tradeCharacter
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                } else {
                    //If the character is in the same place, start a conversation first

                    if (parent.ongoingMeetings.none {
                            it.value.currentCharacters.containsAll(
                                listOf(
                                    character,
                                    tradeCharacter
                                )
                            )
                        }) {
                        routines.add(
                            Routine(
                                "talkToCharacter",
                                routines[0].priority + 10
                            ).also {
                                it.variables["character"] = tradeCharacter
                            })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else {
                        //if the character is in the same meeting, trade for the resource

                        trade(parent, character, place).also { trade ->
                            trade.who = tradeCharacter
                            trade.item2 = routines[0].variables["wantedResource"]!!
                            trade.amount2 = parent.characters[tradeCharacter]!!.reliants.size + 1
                            //Give away unwanted resources
                            trade.item =
                                parent.characters[character]!!.resources.keys.filter { it != routines[0].variables["wantedResource"]!! }
                                    .random()
                            trade.amount = parent.characters[character]!!.resources[trade.item] ?: 0
                            //Give away information they want
                            trade.info = parent.informations.values.filter {
                                it.tgtCharacter == tradeCharacter && it.knownTo.contains(character)
                            }.random()
                            //Give away actions they want
                            trade.onNextTurn = {
                                if (it)//Trade accepted
                                    routines.removeAt(0)//Remove the current routine.
                                else
                                    routines[0].variables["desperation"] =
                                        ((routines[0].variables["desperation"]?.toInt()
                                            ?: 0) + 1).toString() //Increase desperation and try again.
                            }//TODO: implement onNextTurn callback
                            routines.removeAt(0)//Remove the current routine.//TODO: remove this line after implementing the above line.
                            return trade
                        }
                    }
                }
            }

        }
        return wait(parent, character, place)

    }

    private fun whenIdle() {
        //When work hours, work
        if (parent.hour in 9..17) {
            routines.add(Routine("work", 0))
            return
        }
        else
        //When not work hours, rest
            routines.add(Routine("rest", 0))
    }

    @Serializable
    class Routine(val name: String, val priority: Int) {
        val variables: HashMap<String, String> = hashMapOf()
    }
}