package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.min

/*
*  NonPlayerAgent is a character that is not controlled by the player.
* It has a list of routines, which are executed in order of priority.
* The first routine is the current routine.
* When the current routine is finished, the next routine is executed.
* Routines ultimately return GameAction, which is executed by the GameEngine.
* This logic is called by GameEngine.chooseAction(), once per character per turn.
*
* */
@Serializable
class NonPlayerAgent(val character: String) : GameStateElement()
{


    private var routines = arrayListOf<Routine>()//Routines are sorted by priority. The first element is the current routine. All other routines are executed when the current routine is finished.
    val place
        get() = parent.places.values.find { it.characters.contains(character) }!!.name

    fun chooseAction(): GameAction
    {
        //1. High priority routine change
        selectRoutine()
        //2. Execute action according to the current routine. This includes low priority switching routines.
        return executeRoutine()
    }

    private fun selectRoutine()
    {
        //If there is almost no food or water, stop all activities and try to get some. ----------------------------------------------------------------------------
        if ((parent.characters[character]!!.resources["ration"]
                        ?: 0) <= (parent.characters[character]!!.reliants.size + 1) || (parent.characters[character]!!.resources["water"]
                        ?: 0) <= (parent.characters[character]!!.reliants.size + 1)
        )
        {
            val wantedResource = if ((parent.characters[character]!!.resources["ration"]
                            ?: 0) <= (parent.characters[character]!!.reliants.size + 1)
            ) "ration" else "water"
            if (parent.characters[character]!!.trait.contains("thief"))
            {
                //Find a place within my division with maximum res.
                if (routines.none { it.name == "steal" })
                    routines.add(Routine("steal", 1000).also { it.variables["stealResource"] = wantedResource })//Add a routine, priority higher than work.

            } else if (parent.characters[character]!!.trait.contains("bargainer"))
            {
                if (routines.none { it.name == "steal" })
                    routines.add(Routine("buy", 1000).also { it.variables["wantedResource"] = wantedResource })//Add a routine, priority higher than work.
            }
        }


    }

    //This is a recursive function. It returns the action to be executed.
    private fun executeRoutine(): GameAction
    {
        if (routines.isEmpty())
        {
            whenIdle()
            if (routines.isEmpty())
                return wait(character, place)
        }
        routines.sortByDescending { it.priority }
        //Leave meeting or conference if the routine was changed.
        if (routines.none { it.name == "attendMeeting" } && parent.ongoingMeetings.any {
                    it.value.currentCharacters.contains(
                            character
                    )
                })
        {
            return leaveMeeting(character, place)
        }
        if (routines.none { it.name == "attendConference" } && parent.ongoingConferences.any {
                    it.value.currentCharacters.contains(
                            character
                    )
                })
        {
            return leaveConference(character, place)
        }
        val commands = parent.characters[character]!!.commands
        //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable (AvailableActions), start execution routine.
        //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
        //We should not enter executeCommand routine if it is already in the routine list.
        if (routines.none { it.name == "executeCommand" })
        {
            if (commands.any {
                        it.executeTime in parent.time - 3..parent.time + 3 && parent.parties[it.issuedParty]!!.integrity > 30.0 && GameEngine.availableActions(
                                parent,
                                it.place,
                                character
                        ).contains(it.action.javaClass.simpleName)
                    })
            {
                routines.add(
                        Routine(
                                "executeCommand",
                                routines[0].priority + 10
                        )
                )//Add the routine with higher priority.
            }
        }

        //Execute action by the current routine
        when (routines[0].name)
        {
            "rest" ->
            {
                if (place != "home")
                {
                    routines.add(Routine("move", routines[0].priority + 10).also {
                        it.variables["movePlace"] = "home"
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                }
                if (parent.characters[character]!!.hunger > 50 || parent.characters[character]!!.thirst > 50)
                    return eat(character, place)
                when (parent.characters[character]!!.health)
                {
                    in 0..40 -> return sleep(character, place)
                    else ->
                        if (parent.hour in 8..18)
                        {//Preparation for work takes 1 hour. Normal work hours are 9-17.
                            routines.removeAt(0)//Remove the current routine.
                            routines.add(Routine("work", 0))
                            return executeRoutine()
                        } else
                        {
                            return wait(character, place)
                        }
                }
            }

            "executeCommand" ->
            {
                //The condition should be same with the executeCommand entry condition.
                val executableCommands = parent.characters[character]!!.commands.filter { it.executeTime in parent.time - 3..parent.time + 3 && parent.parties[it.issuedParty]!!.integrity > 30.0 && GameEngine.availableActions(parent, it.place, character).contains(it.action.javaClass.simpleName) }
                if (executableCommands.isEmpty())
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                println("$character is executing the command from ${executableCommands}.")
                executableCommands.forEach { command ->
                    if (place == command.place)
                    {
                        if (command.action.isValid())
                        {
                            println("$character: The command ${command.action} is valid. Executing...")
                            commands.remove(command)//Execute the command.
                            return command.action
                        }
                    }
                    //Check other executable commands.
                }
                executableCommands.forEach { command ->
                    if (place != command.place)
                    {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = command.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    }
                }

            }

            "move" ->
            {
                if (place == routines[0].variables["movePlace"])
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else
                {

                    if (routines[0].variables["movePlace"] == "home")
                    {
                        if (place != parent.characters[character]!!.home)
                        {
                            return move(character, place).also { it.placeTo = parent.characters[character]!!.home }//If player is far from the home, go outside the home.
                        } else
                        {
                            return home(character, place)//If player is outside the home, go inside.
                        }
                    } else
                    {
                        if (place == "home")//If the character is at home, go outside.
                            return move(character, place).also { it.placeTo = parent.characters[character]!!.home }
                        return move(character, place).also { it.placeTo = routines[0].variables["movePlace"]!! }
                    }

                    //TODO: implement pathfinding. For now, just teleport to the place
                }

            }


            "steal" ->
            {
                val resplace =
                        parent.places.values.filter {
                            it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                                    character
                            )
                        }
                                .maxByOrNull { it.resources[routines[0].variables["wantedResource"]] ?: 0 }

                if (resplace == null)
                {
                    //Stop stealing because there is no place to steal from.
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else
                {
                    if (place != resplace.name)
                    {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = resplace.name
                        })//Add a move routine with higher priority.
                    } else
                    {
                        //Finish stealing

                        unofficialResourceTransfer(character, place).also {
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

            "work" ->
            {
                //If an accident happened in the place of my control, investigate and clear it.
                parent.places.values.filter {
                    it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                            character
                    )
                }.forEach { place1 ->
                    if (place1.isAccidentScene)
                    {
                        if (place != place1.name)
                        {
                            routines.add(Routine("move", routines[0].priority + 10).also {
                                it.variables["movePlace"] = place1.name
                            })//Add a move routine with higher priority.
                            return executeRoutine()
                        } else
                        //TODO: implement investigateAccidentScene. Right now the information is immediately known to the division leader.

                            clearAccidentScene(character, place).also {
                                return it
                            }
                    }
                }

                //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest.
                if (parent.hour !in 8..18)
                {
                    routines.removeAt(0)//Remove the current routine.
                    routines.add(Routine("rest", 0))
                    return executeRoutine()
                }
                if (parent.characters[character]!!.health < 30 || parent.characters[character]!!.hunger > 80 || parent.characters[character]!!.thirst > 60)
                {
                    routines.removeAt(0)//Remove the current routine.
                    routines.add(Routine("rest", 50)) //Rest with higher priority.
                    return executeRoutine()
                }
                if (parent.ongoingConferences.any {
                            it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                    character
                            )
                        })//If missed a conference
                {
                    val conference = parent.ongoingConferences.filter {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                character
                        )
                    }.values.first()
                    //----------------------------------------------------------------------------------Move to the conference
                    if (place != conference.place)
                    {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = conference.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else
                    {
                        routines.add(Routine("attendConference", routines[0].priority + 10).also {
                            it.intVariables["time"] = conference.time
                        })//Add a routine with higher priority.
                        joinConference(character, place).also {
                            it.meetingName = parent.ongoingConferences.filter {
                                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                        character
                                )
                            }.keys.first()
                            return it
                        }
                    }
                    //----------------------------------------------------------------------------------Move to the conference
                }
                if (parent.scheduledConferences.any { it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3 })//If a conference is soon
                {
                    val conf = parent.scheduledConferences.filter {
                        it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3
                    }.values.first()
                    //----------------------------------------------------------------------------------Move to the conference
                    if (place != conf.place)
                    {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = conf.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else
                    {
                        //if this character is the leader, start the conference.
                        if (parent.parties[conf.involvedParty]!!.leader == character)
                        {
                            routines.add(Routine("attendConference", routines[0].priority + 10).also {
                                it.intVariables["time"] = conf.time
                            })//Add a routine with higher priority.
                            startConference(character, place).also { action ->
                                action.meetingName =
                                        parent.scheduledConferences.keys.first { parent.scheduledConferences[it] == conf }
                                return action
                            }
                        } else
                        {
                            return wait(
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
                        })
                {
                    //----------------------------------------------------------------------------------Move to the meeting
                    val meeting = parent.ongoingMeetings.filter {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                character
                        )
                    }.values.first()
                    if (place != meeting.place)
                    {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = meeting.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else
                    {
                        routines.add(Routine("attendMeeting", routines[0].priority + 10).also {
                            it.intVariables["time"] = meeting.time
                        })//Add a routine with higher priority.
                        joinMeeting(character, place).also {
                            it.meetingName = parent.ongoingMeetings.filter {
                                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                        character
                                )
                            }.keys.first()
                            return it
                        }
                    }
                    //----------------------------------------------------------------------------------Move to the meeting
                }
                if (parent.scheduledMeetings.any { it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3 })//If a meeting is soon
                {
                    //----------------------------------------------------------------------------------Move to the meeting
                    val meeting = parent.scheduledMeetings.filter {
                        it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3
                    }.values.first()
                    if (place != meeting.place)
                    {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = meeting.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else //If there is no meeting yet, create one. Concurrent meetings do not happen, as the meeting is created immediately only if there is no meeting in the place.
                        routines.add(Routine("attendMeeting", routines[0].priority + 10).also {
                            it.intVariables["time"] = meeting.time
                        })//Add a routine with higher priority.
                    startMeeting(character, place).also { action ->
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
                        if (res != "")
                        {
                            //Find a place within my division with maximum res.
                            val resplace =
                                    parent.places.values.filter {
                                        it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                                                character
                                        )
                                    }
                                            .maxByOrNull { it.resources[res] ?: 0 }
                                            ?: return@fe
                            if (place != resplace.name)
                            {
                                routines.add(
                                        Routine(
                                                "move",
                                                routines[0].priority + 10
                                        ).also {
                                            it.variables["movePlace"] = resplace.name
                                        })//Add a move routine with higher priority.
                                return executeRoutine()
                            } else
                            {
                                officialResourceTransfer(character, place).also {
                                    it.what = res
                                    it.toWhere = place1.name
                                    it.amount = (resplace.resources[res] ?: 0) / 2
                                    return it
                                }
                            }
                        }
                    }

                }
                //Corruption for power: If the character is the leader of a party, and a party member is short of resources, steal resources from workplace to party member's home
                //Only attempted once a day or once a work, whichever is shorter.
                if (parent.time - (routines[0].intVariables["corruptionTimer"] ?: 0) > 48)
                    if (parent.parties.values.any { it.leader == character })
                    {
                        val party = parent.parties.values.find { it.leader == character }!!
                        val rationThreshold = 10//TODO: threshold change depending on member's trait and need
                        val waterThreshold = 10
                        val member = party.members.find {
                            (parent.characters[it]!!.resources["ration"]
                                    ?: 0) <= rationThreshold * (parent.characters[it]!!.reliants.size + 1) || (parent.characters[it]!!.resources["water"]
                                    ?: 0) <= waterThreshold * (parent.characters[it]!!.reliants.size + 1)
                        }
                        if (member != null)
                        {
                            //The resource to steal is what the member is short of, either ration or water.
                            val wantedResource = if ((parent.characters[character]!!.resources["ration"]
                                            ?: 0) <= rationThreshold * (parent.characters[character]!!.reliants.size + 1)
                            ) "ration" else "water"
                            routines[0].intVariables["corruptionTimer"] = parent.time
                            routines.add(Routine("steal", routines[0].priority + 10).also { it.variables["stealResource"] = wantedResource; it.variables["stealFor"] = member })//Add a routine, priority higher than work.
                            return executeRoutine()
                        }
                    }


                //If there is nothing above to do, move to a place that is the home of one of the parties of the character.
                //If already at home, wait.
                if (parent.parties.values.any { party -> party.home == place && party.members.contains(character) })
                {
                    return wait(character, place)
                } else
                //Move to a place that is the home of one of the parties of the character.
                {
                    try
                    {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = parent.places.values.filter { place ->
                                parent.parties.values.any { party -> party.home == place.name && party.members.contains(character) }
                            }.random().name
                        })//Add a move routine with higher priority.
                        return executeRoutine()

                    } catch (e: Exception)
                    {
                        println("Warning: No place to commute found for $character.")
                        return wait(character, place)
                    }

                }
            }

            "attendMeeting" ->
            {
                //If the meeting is over, leave the routine.
                if (parent.ongoingMeetings.none { it.value.currentCharacters.contains(character) })
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                //If an hour has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
                if (routines[0].intVariables["time"]!! + 1 <= parent.time)
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                return wait(character, place)
                //TODO: do something in the meeting. Leave the meeting if nothing to do.
            }

            "attendConference" ->
            {

                //If the conference is over, leave the routine.
                if (parent.ongoingConferences.none { it.value.currentCharacters.contains(character) })
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                val conf = parent.ongoingConferences.filter { it.value.currentCharacters.contains(character) }.values.first()
                //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
                if (routines[0].intVariables["time"]!! + 4 <= parent.time)
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                when (conf.subject)
                {
                    "budgetProposal" ->
                    {
                        //If leader, propose budget
                        if (parent.parties[conf.involvedParty]!!.leader == character)
                        {
                            if (!parent.isBudgetProposed)
                            {
                                return budgetProposal(character, place)
                            } else //If budget is proposed, leave the conference.
                            {
                                routines.removeAt(0)//Remove the current routine.
                                return executeRoutine()
                            }
                        } else
                        {
                            //If not leader, wait for the budget to be proposed.
                            if (!parent.isBudgetProposed)
                            {
                                return wait(character, place)
                            } else
                            {
                                routines.removeAt(0)//Remove the current routine.
                                return executeRoutine()
                            }
                        }
                    }

                    "leaderAssignment" ->
                    {
                        //If leader, assign new leader
                        if (parent.parties[conf.involvedParty]!!.leader == character)
                        {
                            if (parent.parties[conf.auxSubject]!!.leader == "")//If the subject party does not have a leader yet, assign a leader.
                            {
                                return leaderAssignment(character, place).also {
                                    it.targetParty = conf.auxSubject
                                    //First option: Assign the leader of the subject party to the character with the highest mutuality.
                                    it.who = parent.parties[conf.auxSubject]!!.members.maxByOrNull { parent.getMutuality(it, character) }!!
                                    //TODO: Second option: Assign the leader of the subject party to the character with the highest support within the party.

                                }
                            } else //If leader is assigned, leave the conference.
                            {
                                routines.removeAt(0)//Remove the current routine.
                                return executeRoutine()
                            }
                        } else
                        {
                            //If not leader, wait for the leader to be assigned.
                            if (parent.parties[conf.auxSubject]!!.leader == "")
                            {
                                return wait(character, place)
                            } else
                            {
                                routines.removeAt(0)//Remove the current routine.
                                return executeRoutine()
                            }
                        }
                    }

                    "divisionDailyConference" ->
                    {
                        //Grab the salary if not grabbed yet.
                        if (!parent.parties[conf.involvedParty]!!.isDailySalaryPaid[character]!!)
                        {
                            return salary(character, place)
                        }
                        //If division leader, share information about the division.
                        //Also, praise or criticize the division members.
                        if (parent.parties[conf.involvedParty]!!.leader == character)
                        {
                            //Share information about the division, if the information is not known to all division members.
                            parent.informations.filter { it.value.tgtParty == conf.involvedParty && it.value.knownTo.contains(character) && !it.value.knownTo.containsAll(conf.currentCharacters) && it.value.tgtTime in parent.time / 48..parent.time / 48 + 47 }.forEach {
                                infoShare(character, place).also { action ->
                                    action.what = it.key
                                    return action
                                }//TODO: do not share all information. Share only the information that the leader wants to share.
                            }
                            //Praise or criticize the division members.
                            parent.parties[conf.involvedParty]!!.members.forEach { member ->
                                if (member != character)
                                {
                                    //praise if the mutuality is high, criticize if the mutuality is low.
                                    val mutuality = parent.getMutuality(character, member)
                                    if (mutuality > 80)
                                    {
                                        infoShare(character, place).also { action ->
                                            //just take random information for now. TODO: take the information that is most useful for praising.
                                            action.what = parent.informations.values.filter { it.tgtCharacter == member && it.knownTo.contains(character) }.random().name
                                            action.application = "praise"
                                            action.who = hashSetOf(member)
                                            return action
                                        }
                                    } else if (mutuality < 20)
                                    {
                                        infoShare(character, place).also { action ->
                                            //just take random information for now. TODO: take the information that is most useful for criticizing.
                                            action.what = parent.informations.values.filter { it.tgtCharacter == member && it.knownTo.contains(character) }.random().name
                                            action.application = "criticize"
                                            action.who = hashSetOf(member)
                                            return action
                                        }
                                    }
                                }//TODO: there must be a cooldown, stored in party class.
                            }
                            //TODO: If it is not covered above, if the division is short of resources, share the information about the resource shortage.
                            //Criticize the common enemies of the division. It is determined by the party with the low mutuality with the division.
                            val enemyParty = parent.parties.values.filter { it.name != conf.involvedParty }.minByOrNull { parent.getPartyMutuality(it.name, conf.involvedParty) }!!.name
                            //Criticize the leader if there is any relevant information.
                            if (parent.informations.values.any { (it.tgtParty == enemyParty || it.tgtCharacter == parent.parties[enemyParty]!!.leader) && it.knownTo.contains(character) })
                                infoShare(character, place).also { action ->
                                    action.what = parent.informations.values.filter { (it.tgtParty == enemyParty || it.tgtCharacter == parent.parties[enemyParty]!!.leader) && it.knownTo.contains(character) }.random().name//TODO: take the information that is most useful for criticizing.
                                    action.application = "criticize"
                                    action.who = hashSetOf(parent.parties[enemyParty]!!.leader)
                                    return action
                                }
                            //Criticize the common enemy. It is determined by average individual mutuality.
                            val enemy = parent.characters.maxByOrNull { ch -> parent.parties[conf.involvedParty]!!.members.sumOf { mem -> parent.getMutuality(mem, ch.key) } }
                            //TODO: request information about the commands issued today.
                        }
                        //If not division leader, Share information about what happened in the division today.
                        if (parent.parties[conf.involvedParty]!!.leader != character)
                        {
                            parent.informations.filter { it.value.tgtParty == conf.involvedParty && it.value.knownTo.contains(character) && it.value.type == "action" && it.value.tgtTime in parent.time / 48..parent.time / 48 + 47 }.forEach {
                                infoShare(character, place).also { action ->
                                    action.what = it.key
                                    return action
                                }
                            }
                        }

                    }
                }
                return wait(character, place)
                //TODO: do something in the meeting. Leave the meeting if nothing to do.

            }

            "findCharacter" ->
            {
                //Stop if the character is at the same place
                if (place == parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name)
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else

                //Move to findCharacter if the character is not at home
                {
                    if (parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name == "home")
                        return wait(character, place)

                    routines.add(Routine("move", routines[0].priority + 10).also {
                        it.variables["movePlace"] =
                                parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                }


            }

            "buy" ->
            {
                //Try to trade for resources
                //Select a character to trade with, based on the information known to the character.
                val tradeCharacter: String
                val info = parent.informations.values.filter {
                    it.type == "resource" && it.tgtCharacter != "" && it.tgtCharacter != character && it.tgtResource == routines[0].variables["wantedResource"] && it.amount > 10 && it.knownTo.contains(
                            character
                    )
                }
                tradeCharacter = if (info.isNotEmpty())
                {//If this character knows a character with the resource
                    info.random().tgtCharacter
                } else
                    parent.characters.keys.filter { it != character }.random()

                //FindCharacter
                // if the character is not in the same place.
                if (place != parent.places.values.find { it.characters.contains(tradeCharacter) }!!.name)
                {
                    routines.add(Routine("findCharacter", routines[0].priority + 10).also {
                        it.variables["character"] = tradeCharacter
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                } else
                {
                    //If the character is in the same place, start a conversation first

                    if (parent.ongoingMeetings.none {
                                it.value.currentCharacters.containsAll(
                                        listOf(
                                                character,
                                                tradeCharacter
                                        )
                                )
                            })
                    {
                        routines.add(
                                Routine(
                                        "talkToCharacter",
                                        routines[0].priority + 10
                                ).also {
                                    it.variables["character"] = tradeCharacter
                                })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else
                    {
                        //if the character is in the same meeting, trade for the resource

                        trade(character, place).also { trade ->
                            trade.who = tradeCharacter
                            trade.item2[routines[0].variables["wantedResource"]!!] = parent.characters[tradeCharacter]!!.reliants.size + 1
                            //Give away unwanted resources
                            val res = parent.characters[character]!!.resources.keys.filter { it != routines[0].variables["wantedResource"]!! }
                                    .random()
                            trade.item[res] = parent.characters[character]!!.resources[res] ?: 0
                            //Give away information they want
                            trade.info = parent.informations.values.filter {
                                it.tgtCharacter == tradeCharacter && it.knownTo.contains(character)
                            }.random()
                            //Give away actions they want
                            trade.onFinished = {
                                if (it)//Trade accepted
                                    routines.removeAt(0)//Remove the current routine.
                                else
                                    routines[0].variables["desperation"] =
                                            ((routines[0].variables["desperation"]?.toInt()
                                                    ?: 0) + 1).toString() //Increase desperation and try again.
                            }
                            return trade
                        }
                    }
                }
            }

        }
        return wait(character, place)

    }


    private fun whenIdle()
    {
        //When work hours, work
        if (parent.hour in 8..18)
        {
            routines.add(Routine("work", 0))
            return
        } else
        //When not work hours, rest
            routines.add(Routine("rest", 0))
    }

    fun decideTrade(who: String, value: Double /*value of the items I am giving away*/, value2: Double/*value of the items I will receive*/, valuea: Double, valuea2: Double): Boolean
    {
        val friendlinessFactor = 0.5//TODO: this should be determined by the character's trait. More friendly characters are more likely to accept the trade which benefits the other character.
        return value >= value2 + (parent.getMutuality(character, who) - 50) * (valuea - valuea2) * friendlinessFactor / 100
    }

    @Serializable
    class Routine(val name: String, val priority: Int)
    {
        val variables: HashMap<String, String> = hashMapOf()
        val intVariables: HashMap<String, Int> = hashMapOf()
    }


}