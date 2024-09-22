package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class WorkRoutine() : Routine()
{
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        val character = gState.characters[name]!!

        //If an accident happened in the place of my control, investigate and clear it.
        gState.places.values.filter {
            it.responsibleParty != "" && gState.parties[it.responsibleParty]!!.members.contains(
                name
            ) && it.isAccidentScene
        }.firstOrNull()?.also {
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
        if (gState.scheduledMeetings.any { it.value.scheduledCharacters.contains(name) && it.value.time - gState.time in -2..2 })//If a Meeting is soon
        {//TODO: consider the distance to the Meeting place.
            val conf = gState.scheduledMeetings.filter {
                it.value.scheduledCharacters.contains(name) && it.value.time - gState.time in -2..2
            }.values.first()
            //----------------------------------------------------------------------------------Move to the Meeting
            if (place != conf.place)
            {
                if (place != conf.place)
                {
                    return MoveRoutine().apply {
                        variables["movePlace"] = conf.place
                    }
                }
            } else
            {
                //if this character is the leader, start the Meeting.
                if (gState.parties[conf.involvedParty]!!.leader == name)
                {
                    return AttendMeetingRoutine().apply {
                        actionDelegated = StartMeeting(name, place).apply {
                            meetingName =
                                this@WorkRoutine.gState.scheduledMeetings.keys.first { this@WorkRoutine.gState.scheduledMeetings[it] == conf }
                        }
                    }
                } else //if this character is the controller and the election is planned, start the Meeting.
                    if (name == "ctrler" && conf.type == "divisionLeaderElection")
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
        if (gState.time - (intVariables["corruptionTimer"] ?: 0) > 48)
            if (gState.parties.values.any { it.leader == name })
            {
                val party = gState.parties.values.find { it.leader == name }!!
                val rationThreshold = 10//TODO: threshold change depending on member's trait and need
                val waterThreshold = 10
                val member = party.members.find {
                    (gState.characters[it]!!.resources["ration"]
                        ?: 0) <= rationThreshold * (gState.characters[it]!!.reliants.size + 1) || (gState.characters[it]!!.resources["water"]
                        ?: 0) <= waterThreshold * (gState.characters[it]!!.reliants.size + 1)
                }
                if (member != null)
                {
                    //The resource to steal is what the member is short of, either ration or water.
                    val wantedResource = if ((character.resources["ration"]
                            ?: 0) <= rationThreshold * (character.reliants.size + 1)
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
            (it.executeTime in gState.time - 3..gState.time + 3 || it.executeTime == 0) && (it.issuedBy.isEmpty() || it.issuedBy.sumOf {
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