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
class NonPlayerAgent : GameStateElement()
{
    override val name: String
        get() = parent.nonPlayerAgents.filter { it.value == this }.keys.first()

    val character: Character
        get() = parent.characters[name]!!

    private var routines =
        arrayListOf<Routine>()//Routines are sorted by priority. The first element is the current routine. All other routines are executed when the current routine is finished.
    val place
        get() = parent.places.values.find { it.characters.contains(name) }!!.name

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
        if ((parent.characters[name]!!.resources["ration"]
                ?: 0) <= (parent.characters[name]!!.reliants.size + 1) || (parent.characters[name]!!.resources["water"]
                ?: 0) <= (parent.characters[name]!!.reliants.size + 1)
        )
        {
            val wantedResource = if ((parent.characters[name]!!.resources["ration"]
                    ?: 0) <= (parent.characters[name]!!.reliants.size + 1)
            ) "ration" else "water"
            if (parent.characters[name]!!.trait.contains("thief"))
            {
                //Find a place within my division with maximum res.
                if (routines.none { it.name == "steal" })
                    routines.add(Routine("steal", 1000).also {
                        it.variables["stealResource"] = wantedResource
                    })//Add a routine, priority higher than work.

            } else if (parent.characters[name]!!.trait.contains("bargainer"))
            {
                if (routines.none { it.name == "steal" })
                    routines.add(Routine("buy", 1000).also {
                        it.variables["wantedResource"] = wantedResource
                    })//Add a routine, priority higher than work.
            }
        }


    }

    //This is a recursive function. It returns the action to be executed.
    private fun executeRoutine(): GameAction
    {
        val charObject = parent.characters[name]!!//Shortcut
        if (routines.isEmpty())
        {
            whenIdle()
            if (routines.isEmpty())
                return Wait(name, place)
        }
        routines.sortByDescending { it.priority }
        //Leave meeting or conference if the routine was changed.
        if ((routines.none { it.name == "attendConference" } && routines.none { it.name == "attendMeeting" } && charObject.currentMeeting != null))
        {
            return LeaveMeeting(name, place)
        }
        //Stop meeting routine if the character is not in a meeting.
        if (routines.any { it.name == "attendMeeting" || it.name == "attendConference" } && charObject.currentMeeting == null)
        {
            while (routines.any { it.name == "attendMeeting" || it.name == "attendConference" })
                routines.removeAt(0)
        }
        //Force start meeting routing if the character is in a meeting. Note that the character will leave the meeting immediately if nothing interests it.
        if(parent.ongoingMeetings.any{it.value.currentCharacters.contains(name)} && routines.none { it.name=="attendMeeting" })
            routines.add(
                Routine(
                    "attendMeeting",
                    routines[0].priority + 10
                )
            )//Add the routine with higher priority.
        //If the character is in a conference but not in attendConference routine, throw an error.
        if(parent.ongoingConferences.any{it.value.currentCharacters.contains(name)} && routines.none { it.name=="attendConference" })
            throw IllegalStateException("$name is in ${character.currentMeeting!!}, which is a conference, but not in attendConference Routine.")
        val commands = charObject.commands
        //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable (AvailableActions), start execution routine.
        //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
        //We should not enter executeCommand routine if it is already in the routine list.
        if (routines.none { it.name == "executeCommand" })
        {
            if (commands.any {
                    it.executeTime in parent.time - 3..parent.time + 3 && parent.parties[it.issuedParty]!!.integrity > 30.0 && GameEngine.availableActions(
                        parent,
                        it.place,
                        name
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
                if (place != "home_$name")
                {
                    routines.add(Routine("move", routines[0].priority + 10).also {
                        it.variables["movePlace"] = "home_$name"
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                }
                if (charObject.hunger > 50 || charObject.thirst > 50)
                    return Eat(name, place)
                when (charObject.health)
                {
                    in 0..40 -> return Sleep(name, place)
                    else ->
                        if (parent.hour in 8..18)
                        {//Preparation for work takes 1 hour. Normal work hours are 9-17.
                            routines.removeAt(0)//Remove the current routine.
                            routines.add(Routine("work", 0))
                            return executeRoutine()
                        } else
                        {
                            return Wait(name, place)
                        }
                }
            }

            "executeCommand" ->
            {
                //The condition should be same with the executeCommand entry condition.
                val executableCommands = charObject.commands.filter {
                    it.executeTime in parent.time - 3..parent.time + 3 && parent.parties[it.issuedParty]!!.integrity > 30.0 && GameEngine.availableActions(
                        parent,
                        it.place,
                        name
                    ).contains(it.action.javaClass.simpleName)
                }
                if (executableCommands.isEmpty())
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                println("$name is executing the command from ${executableCommands}.")
                executableCommands.forEach { command ->
                    if (place == command.place)
                    {
                        if (command.action.isValid())
                        {
                            println("$name: The command ${command.action} is valid. Executing...")
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

                    if (routines[0].variables["movePlace"] == "home_$name")
                    {
                        if (place != charObject.livingBy)
                        {
                            return Move(name, place).also {
                                it.placeTo = charObject.livingBy
                            }//If player is far from the home, go outside the home.
                        } else
                        {
                            return Move(name, place).also {
                                it.placeTo = "home_$name"
                            }//If player is outside the home, go inside.
                        }
                    } else
                    {
                        if (place == "home")//If the character is at home, go outside.
                            return Move(name, place).also { it.placeTo = charObject.livingBy }
                        return Move(name, place).also { it.placeTo = routines[0].variables["movePlace"]!! }
                    }

                    //TODO: implement pathfinding. For now, just teleport to the place
                }

            }


            "steal" ->
            {
                val resplace =
                    parent.places.values.filter {
                        it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                            name
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

                        UnofficialResourceTransfer(name, place).also {
                            it.resources = hashMapOf(
                                routines[0].variables["stealResource"]!! to min(
                                    (resplace.resources["ration"] ?: 0) / 2,
                                    (charObject.reliants.size + 1) * 7
                                )
                            )
                            it.toWhere = "home_$name"
                            println("$name is stealing ${it.resources} from ${resplace.name}!")
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
                        name
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

                            ClearAccidentScene(name, place).also {
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
                if (charObject.health < 30 || charObject.hunger > 80 || charObject.thirst > 60)
                {
                    routines.removeAt(0)//Remove the current routine.
                    routines.add(Routine("rest", 50)) //Rest with higher priority.
                    return executeRoutine()
                }
                if (parent.ongoingConferences.any {
                        it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                            name
                        )
                    })//If missed a conference
                {
                    val conference = parent.ongoingConferences.filter {
                        it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                            name
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
                        JoinConference(name, place).also {
                            it.meetingName = parent.ongoingConferences.filter {
                                it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                                    name
                                )
                            }.keys.first()
                            return it
                        }
                    }
                    //----------------------------------------------------------------------------------Move to the conference
                }
                if (parent.scheduledConferences.any { it.value.scheduledCharacters.contains(name) && it.value.time - parent.time in -2..2 })//If a conference is soon
                {//TODO: consider the distance to the conference place.
                    val conf = parent.scheduledConferences.filter {
                        it.value.scheduledCharacters.contains(name) && it.value.time - parent.time in -2..2
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
                        if (parent.parties[conf.involvedParty]!!.leader == name)
                        {
                            routines.add(Routine("attendConference", routines[0].priority + 10).also {
                                it.intVariables["time"] = conf.time
                            })//Add a routine with higher priority.
                            StartConference(name, place).also { action ->
                                action.meetingName =
                                    parent.scheduledConferences.keys.first { parent.scheduledConferences[it] == conf }
                                return action
                            }
                        } else
                        {
                            return Wait(
                                name,
                                place
                            )//Wait for the conference to start.
                        }

                    }
                    //----------------------------------------------------------------------------------Move to the conference
                }
                //If missed a meeting
                if (parent.ongoingMeetings.any {
                        it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                            name
                        )
                    })
                {
                    //----------------------------------------------------------------------------------Move to the meeting
                    val meeting = parent.ongoingMeetings.filter {
                        it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                            name
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
                        JoinMeeting(name, place).also {
                            it.meetingName = parent.ongoingMeetings.filter {
                                it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                                    name
                                )
                            }.keys.first()
                            return it
                        }
                    }
                    //----------------------------------------------------------------------------------Move to the meeting
                }
                if (parent.scheduledMeetings.any { it.value.scheduledCharacters.contains(name) && it.value.time - parent.time in -1..3 })//If a meeting is soon
                {
                    //----------------------------------------------------------------------------------Move to the meeting
                    val meeting = parent.scheduledMeetings.filter {
                        it.value.scheduledCharacters.contains(name) && it.value.time - parent.time in -1..3
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
                    StartMeeting(name, place).also { action ->
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
                                        name
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
                                OfficialResourceTransfer(name, place).also {
                                    it.resources = hashMapOf(res to (resplace.resources[res] ?: 0) / 2)
                                    it.toWhere = place1.name
                                    return it
                                }
                            }
                        }
                    }

                }
                //Corruption for power: If the character is the leader of a party, and a party member is short of resources, steal resources from workplace to party member's home
                //Only attempted once a day or once a work, whichever is shorter.
                if (parent.time - (routines[0].intVariables["corruptionTimer"] ?: 0) > 48)
                    if (parent.parties.values.any { it.leader == name })
                    {
                        val party = parent.parties.values.find { it.leader == name }!!
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
                            val wantedResource = if ((charObject.resources["ration"]
                                    ?: 0) <= rationThreshold * (charObject.reliants.size + 1)
                            ) "ration" else "water"
                            routines[0].intVariables["corruptionTimer"] = parent.time
                            routines.add(
                                Routine(
                                    "steal",
                                    routines[0].priority + 10
                                ).also {
                                    it.variables["stealResource"] = wantedResource; it.variables["stealFor"] = member
                                })//Add a routine, priority higher than work.
                            return executeRoutine()
                        }
                    }


                //If there is nothing above to do, move to a place that is the home of one of the parties of the character.
                //If already at home, wait.
                if (parent.parties.values.any { party -> party.home == place && party.members.contains(name) })
                {
                    return Wait(name, place)
                } else
                //Move to a place that is the home of one of the parties of the character.
                {
                    try
                    {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = parent.places.values.filter { place ->
                                parent.parties.values.any { party ->
                                    party.home == place.name && party.members.contains(
                                        name
                                    )
                                }
                            }.random().name
                        })//Add a move routine with higher priority.
                        return executeRoutine()

                    } catch (e: Exception)
                    {
                        println("Warning: No place to commute found for $name.")
                        return Wait(name, place)
                    }

                }
            }

            "supportAgenda" ->
            {
                val conf =
                    charObject.currentMeeting!!
                when (routines[0].variables["agenda"])
                {
                    "budgetResolve" ->
                    {
                        routines.removeAt(0)//Remove the current routine.
                        return executeRoutine()
                    }

                    "budgetProposal" ->
                    {
                        routines.removeAt(0)//Remove the current routine.
                        return executeRoutine()
                    }

                    "proofOfWork" ->
                    {
                        //TODO: implement support proofOfWork within the deck of information
                        when (conf.involvedParty)
                        {
                            "infrastructure" ->
                            {
                                //Repair jobs prove the work.
                                val info = parent.informations.filter {
                                    it.value.type == "action" && it.value.action == "repair" && parent.places.filterValues { it.responsibleParty == conf.involvedParty }.keys.contains(
                                        it.value.tgtPlace
                                    )
                                }
                                if (info.isNotEmpty())
                                {
                                    return AddInfo(name, place).also {
                                        it.infoKey = info.keys.first()
                                    }
                                }
                            }
                        }
                        routines.removeAt(0)//Remove the current routine.
                        return executeRoutine()
                    }

                    "salary" ->
                    {

                        routines.removeAt(0)//Remove the current routine.
                        return executeRoutine()
                    }
                }

            }

            "attendMeeting" ->
            {
                //TODO: check the subject variable to request something.
                //If the meeting is over, leave the routine.
                if (charObject.currentMeeting == null)
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
                return Wait(name, place)
                //TODO: do something in the meeting. Leave the meeting if nothing to do.
            }

            "attendConference" ->
            {

                //If the conference is over, leave the routine.
                if (charObject.currentMeeting == null)
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                val conf =
                    charObject.currentMeeting!!
                //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
                //TODO: stay in the meeting until I have something to do, or the work hours are over.
                if (routines[0].intVariables["time"]!! + 4 <= parent.time)
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                when (conf.type)
                {
                    "triumvirateDailyConference" ->
                    {
                        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
                        if (conf.currentSpeaker != name)
                        {
                            if (parent.getMutuality(
                                    name,
                                    conf.currentSpeaker
                                ) > 50 || conf.currentAttention > Intercept.threshold
                            )
                                return Wait(name, place)
                            else
                                return Intercept(name, place)
                        }
                        //If this character is the speaker
                        else
                        {
                            //Budget is resolved through voting, which is not in the meeting.
//                            if (parent.isBudgetProposed && !parent.isBudgetResolved && conf.agendas.none { it.subjectType == "budgetResolution" })
//                            {
//                                return NewAgenda(name, place).also {
//                                    it.agenda = MeetingAgenda("budgetResolution")
//                                }
//                            } else
//                            {
//                                //If we haven't tried this branch in the current routine
//                                if (routines[0].intVariables["try_support_budgetResolution"] == 0)
//                                {
//                                    routines[0].intVariables["try_support_budgetResolution"] = 1
//                                    //If the agenda is already proposed, support it.
//                                    routines.add(
//                                        Routine(
//                                            "supportAgenda",
//                                            routines[0].priority + 10
//                                        ).also {
//                                            it.intVariables["agendaIndex"] =
//                                                conf.agendas.indexOfFirst { it.subjectType == "budgetResolution" }
//                                        })//Add a routine, priority higher than work.
//                                }
//                            }
                        }
                    }

                    "cabinetDailyConference" ->
                    {
                        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
                        if (conf.currentSpeaker != name)
                        {
                            if (parent.getMutuality(
                                    name,
                                    conf.currentSpeaker
                                ) > 50 || conf.currentAttention > Intercept.threshold
                            )
                                return Wait(name, place)
                            else
                                return Intercept(name, place)
                        } else
                        {
                            //If budget is not proposed, propose it.
                            if (!parent.isBudgetProposed && conf.agendas.none { it.subjectType == "budgetProposal" })
                            {
                                return NewAgenda(name, place).also {
                                    it.agenda = MeetingAgenda("budgetProposal").also { agenda ->
                                        //TODO: calculate the budget based on the information. Right now the budget is calculated based on the work hours of the places.
                                        parent.places.forEach {
                                            if (it.key == "home" || it.value.responsibleParty == "") return@forEach else agenda.subjectIntParams[it.value.responsibleParty] =
                                                (agenda.subjectIntParams[it.value.responsibleParty]
                                                    ?: 0) + it.value.plannedWorker * (it.value.workHoursEnd - it.value.workHoursStart) * 15
                                        }
                                    }
                                }
                            } else
                            {
                                //If we haven't tried this branch in the current routine
                                if (routines[0].intVariables["try_support_budgetProposal"] == 0)
                                {
                                    routines[0].intVariables["try_support_budgetProposal"] = 1
                                    //If the agenda is already proposed, support it.
                                    routines.add(
                                        Routine(
                                            "supportAgenda",
                                            routines[0].priority + 10
                                        ).also {
                                            it.intVariables["agendaIndex"] =
                                                conf.agendas.indexOfFirst { it.subjectType == "budgetProposal" }
                                        })//Add a routine, priority higher than work.
                                }
                            }
                        }
                    }

                    "divisionDailyConference" ->
                    {
                        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
                        if (conf.currentSpeaker != name)
                        {
                            if (parent.getMutuality(
                                    name,
                                    conf.currentSpeaker
                                ) > 50 || conf.currentAttention > Intercept.threshold
                            )
                                return Wait(name, place)
                            else
                                return Intercept(name, place)
                        } else
                        {
                            //If speaker, propose proof of work if nothing else is important.
                            if (conf.agendas.none { it.subjectType == "proofOfWork" })
                            {
                                return NewAgenda(name, place).also {
                                    it.agenda = MeetingAgenda("proofOfWork")
                                }
                            } else
                            {
                                //If we haven't tried this branch in the current routine
                                if (routines[0].intVariables["try_support_proofOfWork"] == 0)
                                {
                                    //If the agenda is already proposed, and we have a supporting information, support it.
                                    routines[0].intVariables["try_support_proofOfWork"] = 1
                                    routines.add(
                                        Routine(
                                            "supportAgenda",
                                            routines[0].priority + 10
                                        ).also {
                                            it.intVariables["agendaIndex"] =
                                                conf.agendas.indexOfFirst { it.subjectType == "proofOfWork" }
                                        })//Add a routine, priority higher than work.
                                    return executeRoutine()
                                }
                            }

                            //Grab the salary if not grabbed yet.
                            if (conf.currentSpeaker == name && !parent.parties[conf.involvedParty]!!.isSalaryPaid)
                            {
                                if (conf.agendas.none { it.subjectType == "salary" })
                                    return NewAgenda(name, place).also {
                                        it.agenda = MeetingAgenda(
                                            "salary"
                                        )
                                    }
                                else
                                {
                                    //If we haven't tried this branch in the current routine
                                    if (routines[0].intVariables["try_support_salary"] == 0)
                                    {
                                        routines[0].intVariables["try_support_salary"] = 1
                                        //If the agenda is already proposed, support it.
                                        routines.add(
                                            Routine(
                                                "supportAgenda",
                                                routines[0].priority + 10
                                            ).also {
                                                it.intVariables["agendaIndex"] =
                                                    conf.agendas.indexOfFirst { it.subjectType == "salary" }
                                            })//Add a routine, priority higher than work.
                                    }
                                }
                            }
                            //If division leader, praise or criticize the division members.
                            if (parent.parties[conf.involvedParty]!!.leader == name)
                            {
                                //Praise or criticize the division members, if there is any relevant information.
                                parent.parties[conf.involvedParty]!!.members.forEach { member ->
                                    if (member != name && parent.informations.values.any {
                                            it.tgtCharacter == member && it.knownTo.contains(
                                                name
                                            )
                                        })
                                    {
                                        //praise if the mutuality is high, criticize if the mutuality is low.
                                        val mutuality = parent.getMutuality(name, member)
                                        if (mutuality > 80)
                                        {
                                            return NewAgenda(name, place).also {
                                                it.agenda =
                                                    MeetingAgenda("praise", subjectParams = hashMapOf("who" to member))
                                            }
                                        } else if (mutuality < 20)
                                        {
                                            return NewAgenda(name, place).also {
                                                it.agenda =
                                                    MeetingAgenda(
                                                        "denounce",
                                                        subjectParams = hashMapOf("who" to member)
                                                    )
                                            }
                                        }
                                    }//TODO: there must be a cooldown, stored in party class.
                                }
                                //TODO: If it is not covered above, if the division is short of resources, share the information about the resource shortage.
                                //TODO: Criticize the common enemies of the division. It is determined by the party with the low mutuality with the division.
                                val enemyParty = parent.parties.values.filter { it.name != conf.involvedParty }
                                    .minByOrNull { parent.getPartyMutuality(it.name, conf.involvedParty) }!!.name
                                //TODO: Criticize the leader if there is any relevant information.
//                                if (parent.parties[enemyParty]!!.leader != "")
//                                    if (parent.informations.values.any {
//                                            (it.tgtParty == enemyParty || it.tgtCharacter == parent.parties[enemyParty]!!.leader) && it.knownTo.contains(
//                                                name
//                                            )
//                                        })
//                                        InfoShare(name, place).also { action ->
//                                            action.what = parent.informations.values.filter {
//                                                (it.tgtParty == enemyParty || it.tgtCharacter == parent.parties[enemyParty]!!.leader) && it.knownTo.contains(
//                                                    name
//                                                )
//                                            }
//                                                .random().name//TODO: take the information that is most useful for criticizing.
//                                            action.application = "criticize"
//                                            action.who = hashSetOf(parent.parties[enemyParty]!!.leader)
//                                            return action
//                                        }
                                //Criticize the common enemy. It is determined by average individual mutuality.
                                val enemy = parent.characters.maxByOrNull { ch ->
                                    parent.parties[conf.involvedParty]!!.members.sumOf { mem ->
                                        parent.getMutuality(
                                            mem,
                                            ch.key
                                        )
                                    }
                                }
                                //TODO: request information about the commands issued today.
                            }
                            //TODO: If not division leader, Share information about what happened in the division today.
                            if (parent.parties[conf.involvedParty]!!.leader != name)
                            {
//                                parent.informations.filter {
//                                    it.value.tgtParty == conf.involvedParty && it.value.knownTo.contains(
//                                        name
//                                    ) && it.value.type == "action" && it.value.tgtTime in parent.time / 48..parent.time / 48 + 47
//                                }.forEach {
//                                    InfoShare(name, place).also { action ->
//                                        action.what = it.key
//                                        return action
//                                    }
//                                }
                            }


                        }
                    }

                }
                return if (conf.currentSpeaker == name)
                //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
                    EndSpeech(name, place).also {
                        it.nextSpeaker =
                            conf.currentCharacters.maxBy { char -> parent.getMutuality(name, char) }
                    }
                //If I'm not the speaker, wait.
                else Wait(name, place)
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
                        return Wait(name, place)

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
                    it.type == "resource" && it.tgtCharacter != "" && it.tgtCharacter != name && it.resources.containsKey(
                        routines[0].variables["wantedResource"]
                    ) && it.resources[routines[0].variables["wantedResource"]]!! > 10 && it.knownTo.contains(
                        name
                    )
                }
                tradeCharacter = if (info.isNotEmpty())
                {//If this character knows a character with the resource
                    info.random().tgtCharacter
                } else
                    parent.characters.keys.filter { it != name }.random()

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
                                    name,
                                    tradeCharacter
                                )
                            )
                        })
                    {
                        routines.add(Routine("attendMeeting", routines[0].priority + 10).also {
                            it.variables["subject"] = "requestResource"
                            it.variables["resource"] = routines[0].variables["wantedResource"]!!
                            it.variables["requestTo"] = tradeCharacter
                        })//Add a move routine with higher priority.
                        return Talk(name, place).also{
                            it.who = tradeCharacter
                        }
                    }
//                    else
//                    {
//                        //if the character is in the same meeting, trade for the resource
//
//                        Trade(name, place).also { trade ->
//                            trade.who = tradeCharacter
//                            trade.item2[routines[0].variables["wantedResource"]!!] =
//                                parent.characters[tradeCharacter]!!.reliants.size + 1
//                            //Give away unwanted resources
//                            val res =
//                                charObject.resources.keys.filter { it != routines[0].variables["wantedResource"]!! }
//                                    .random()
//                            trade.item[res] = charObject.resources[res] ?: 0
//                            //Give away information they want
//                            trade.info = parent.informations.values.filter {
//                                it.tgtCharacter == tradeCharacter && it.knownTo.contains(name)
//                            }.random()
//                            //Give away actions they want
//                            trade.onFinished = {
//                                if (it)//Trade accepted
//                                    routines.removeAt(0)//Remove the current routine.
//                                else
//                                    routines[0].variables["desperation"] =
//                                        ((routines[0].variables["desperation"]?.toInt()
//                                            ?: 0) + 1).toString() //Increase desperation and try again.
//                            }
//                            return trade
//                        }
//                    }
                }
            }

        }
        return Wait(name, place)

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

    fun decideTrade(
        who: String,
        value: Double /*value of the items I am giving away*/,
        value2: Double/*value of the items I will receive*/,
        valuea: Double,
        valuea2: Double
    ): Boolean
    {
        val friendlinessFactor =
            0.5//TODO: this should be determined by the character's trait. More friendly characters are more likely to accept the trade which benefits the other character.
        return value >= value2 + (parent.getMutuality(
            name,
            who
        ) - 50) * (valuea - valuea2) * friendlinessFactor / 100
    }

    @Serializable
    class Routine(val name: String, val priority: Int)
    {
        val variables: HashMap<String, String> = hashMapOf()
        val intVariables: HashMap<String, Int> = hashMapOf()
    }


}