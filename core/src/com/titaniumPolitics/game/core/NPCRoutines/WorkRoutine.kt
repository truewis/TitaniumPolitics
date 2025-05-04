package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.JoinMeeting
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.StartMeeting
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class WorkRoutine() : Routine()
{
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        val character = gState.characters[name]!!

        //If an accident happened in the place of my control, investigate and clear it.
        gState.places.values.firstOrNull {
            it.responsibleDivision != "" && gState.parties[it.responsibleDivision]!!.members.contains(
                name
            ) && it.isAccidentScene
        }?.also {
            return InvestigateAndClearAccidentRoutine().apply {
                variables["place"] = it.name
            }
        }

        if (gState.ongoingMeetings.any {
                it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                    name
                )
            })//If missed a conference
        {
            val conf = gState.ongoingMeetings.filter {
                it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                    name
                )
            }.values.first()
            //----------------------------------------------------------------------------------Move to the Meeting
            if (place != conf.place)
            {
                return MoveRoutine().apply {
                    variables["movePlace"] = conf.place
                }
            } else
            {
                return AttendMeetingRoutine().apply {
                    actionDelegated = JoinMeeting(name, place).also {
                        it.meetingName = this@WorkRoutine.gState.ongoingMeetings.filter {
                            it.value.scheduledCharacters.contains(name) && !it.value.currentCharacters.contains(
                                name
                            )
                        }.keys.first()
                    }
                }
            }
        }
        if (gState.scheduledMeetings.any {
                it.value.scheduledCharacters.contains(name) && it.value.time - gState.time in -ReadOnly.constInt(
                    "MeetingStartTolerance"
                ) + Place.timeBetweenPlaces(
                    it.value.place,
                    place
                )..ReadOnly.constInt("MeetingStartTolerance") + Place.timeBetweenPlaces(it.value.place, place)
            })//If a Meeting is soon
        {
            val conf = gState.scheduledMeetings.filter {
                it.value.scheduledCharacters.contains(name) && it.value.time - gState.time in -ReadOnly.constInt(
                    "MeetingStartTolerance"
                ) + Place.timeBetweenPlaces(
                    it.value.place,
                    place
                )..ReadOnly.constInt("MeetingStartTolerance") + Place.timeBetweenPlaces(it.value.place, place)
            }.values.first()
            //----------------------------------------------------------------------------------Move to the Meeting
            if (place != conf.place)
            {
                return MoveRoutine().apply {
                    variables["movePlace"] = conf.place
                }
            } else
            {
                //if this character is the leader and there are enough members, start the Meeting.
                if (gState.parties[conf.involvedParty]!!.leader == name && conf.scheduledCharacters.intersect(gState.places[place]!!.characters).size >= 2)
                {
                    return AttendMeetingRoutine().apply {
                        actionDelegated = StartMeeting(name, place).apply {
                            meetingName =
                                this@WorkRoutine.gState.scheduledMeetings.keys.first { this@WorkRoutine.gState.scheduledMeetings[it] == conf }
                        }
                    }
                } else //if this character is the controller and the election is planned, start the Meeting.
                    if (name == "ctrler" && conf.type == "divisionLeaderElection" && conf.scheduledCharacters.intersect(
                            gState.places[place]!!.characters
                        ).size >= 2
                    )
                    {
                        return AttendMeetingRoutine().apply {
                            actionDelegated = StartMeeting(name, place).apply {
                                meetingName =
                                    this@WorkRoutine.gState.scheduledMeetings.keys.first { this@WorkRoutine.gState.scheduledMeetings[it] == conf }
                            }
                        }
                    }
            }
        }

        //Corruption for power: If the character is the leader of a party, and a party member is short of resources, steal resources from workplace to party member's home
        //Only attempted once a day or once a work, whichever is shorter.
        if (gState.time - (intVariables["corruptionTimer"] ?: 0) > ReadOnly.constInt("CorruptionTau") / ReadOnly.dt)
            if (gState.parties.values.any { it.leader == name })
            {
                val party = gState.parties.values.find { it.leader == name }!!
                val rationThreshold =
                    ReadOnly.const("StealAmountMultiplier")//TODO: threshold change depending on member's trait and need
                val waterThreshold = ReadOnly.const("StealAmountMultiplier")
                val member = party.members.find {
                    gState.characters[it]!!.resources["ration"] <= rationThreshold * (gState.characters[it]!!.reliant) || gState.characters[it]!!.resources["water"] <= waterThreshold * (gState.characters[it]!!.reliant)
                }
                if (member != null)
                {
                    //The resource to steal is what the member is short of, either ration or water.
                    val wantedResource =
                        if (character.resources["ration"] <= rationThreshold * (character.reliant)
                        ) "ration" else "water"
                    intVariables["corruptionTimer"] = gState.time
                    return StealRoutine().apply {
                        variables["stealResource"] = wantedResource; variables["stealFor"] = member
                    }
                }
            }

        //Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
        //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
        //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.

        val request = gState.requests.values.firstOrNull {
            (it.executeTime in gState.time - ReadOnly.constInt("CommandExecuteTolerance") + Place.timeBetweenPlaces(
                it.action.tgtPlace,
                place
            )..gState.time + ReadOnly.constInt("CommandExecuteTolerance") + Place.timeBetweenPlaces(
                it.action.tgtPlace,
                place
            ) || it.executeTime == 0) && (it.issuedBy.isEmpty() || it.issuedBy.sumOf {
                gState.getMutuality(
                    name,
                    it
                )
            } / it.issuedBy.size > ReadOnly.const("RequestRejectAverageMutuality")) && GameEngine.availableActions(
                gState,
                it.action.tgtPlace,
                name
            )
                .contains(it.action.javaClass.simpleName) //Here, we can move to other places to execute the command, so we do not check if the place is here.
        }
        if (request != null)
        {
            return ExecuteCommandRoutine().also { it.variables["request"] = request.name }
        }

        //Supply resource
        //TODO: when pathfinding fails, skip.
        gState.places.values.forEach { place1 -> //TODO: right now, supply resource to any place regardless of the division. In the future, agents will not supply resources to hostile divisions.
            place1.apparatuses.forEach { apparatus ->
                val res = GameEngine.resourceShortOf(apparatus, place1) //Type of resource that is short of.
                if (res != "")
                //if there is a place within my division with the resource
                {
                    val resplace =
                        gState.places.values.filter {
                            it.responsibleDivision != "" && gState.parties[it.responsibleDivision]!!.members.contains(
                                name
                            )
                        }
                            .maxByOrNull { it.resources[res] }
                    if (resplace != null && place1.name != resplace.name)
                    //start new routine if there is a place with all the conditions met.
                        if (resplace.resources[res] > 0)
                            return TransferResourceRoutine().also {
                                it.res = res; it.source = resplace.name; it.dest = place1.name
                            }
                }
            }
        }

        //If there is some time, prepare information
        if (gState.scheduledMeetings.none {
                it.value.scheduledCharacters.contains(name) && it.value.time - gState.time in -ReadOnly.constInt(
                    "MeetingStartTolerance"
                ) + Place.timeBetweenPlaces(
                    it.value.place,
                    place
                ) + ReadOnly.constInt("PrepareInfoDuration")..ReadOnly.constInt("MeetingStartTolerance") + Place.timeBetweenPlaces(
                    it.value.place,
                    place
                ) + ReadOnly.constInt("PrepareInfoDuration")
            })//If a Meeting is not soon
        {
            //If we haven't prapared info recently
            if (gState.informations.none { (_, information) ->
                    information.author == character.name && information.type == InformationType.ACTION && information.action is PrepareInfo
                            && gState.time - information.creationTime > ReadOnly.constInt("lengthOfDay") * 2
                })
            //If we haven't tried this branch in the current routine
                if (intVariables["try_prepare_info"] != 1)
                {
                    intVariables["try_prepare_info"] = 1
                    return PrepareInfoRoutine()
                }
        }


        //If there is nothing above to do, move to a place that is the home of one of the parties of the character.
        //If already at home, wait.
        if (gState.parties.values.any { party -> party.home == place && party.members.contains(name) })
        {
        } else
        //Move to a place that is the home of one of the parties of the character.
        {
            try
            {
                return MoveRoutine().apply {
                    gState = this@WorkRoutine.gState
                    variables["movePlace"] = gState.places.values.filter { place ->
                        gState.parties.values.any { party ->
                            party.home == place.name && party.members.contains(
                                name
                            )
                        }
                    }.random().name
                }
            } catch (e: NoSuchElementException)
            {
                println("Warning: No place to commute found for $name")
            }


        }
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        //TODO: If there is nothing above to do, move to a place that is the home of one of the parties of the character.
        //If already at home, wait.
        if (gState.parties.values.any { party -> party.home == place && party.members.contains(name) })
        {
            return Wait(name, place)
        }
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest. (Which is checked earlier.)
        return (gState.hour !in 8..18)
    }

    @Transient
    override val availableActions = listOf("Eat", "Sleep", "Wait")
}