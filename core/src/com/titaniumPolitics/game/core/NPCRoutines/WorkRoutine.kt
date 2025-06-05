package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.JoinMeeting
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.StartMeeting
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable
import kotlin.text.set

@Serializable
class WorkRoutine() : Routine() {
    init {
        priority = PRIORITY_WORK
    }

    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        val character = gState.characters[name]!!

        if (routines.any { it is IMeetingRoutine })//I am already in a meeting, do not start a new routine.
            return null

        //I am forced into a meeting. Pick a meeting routine.
        if (character.currentMeeting != null) {
            return pickMeetingRoutine(name, character.currentMeeting!!)
        }

        //If an accident happened in the place of my control, investigate and clear it.
        gState.places.values.firstOrNull {
            it.responsibleDivision != "" && gState.parties[it.responsibleDivision]!!.members.contains(
                name
            ) && it.isAccidentScene
        }?.also { place ->
            if (routines.none { it is InvestigateAndClearAccidentRoutine && it.variables["place"] == place.name }) {
                //If there is no routine to investigate and clear the accident in this place, create a new one.
                return InvestigateAndClearAccidentRoutine().apply {
                    variables["place"] = place.name
                }
            }
        }

        //1. If missed a conference
        val missingMeeting = gState.ongoingMeetings.values
            .firstOrNull { it.scheduledCharacters.contains(name) && !it.currentCharacters.contains(name) }

        if (missingMeeting != null) {
            // Move to the meeting if not already there
            if (place != missingMeeting.place) {
                if (routines.none { it is MoveRoutine }) {
                    return MoveRoutine().apply {
                        variables["movePlace"] = missingMeeting.place
                    }
                }
            } else {
                return pickMeetingRoutine(name, missingMeeting)

            }
        }

        //2. If a conference is scheduled
        gState.scheduledMeetings.values.firstOrNull {
            val eta = gState.places[it.place]!!.shortestPathAndTimeTo(place)?.second ?: return@firstOrNull false
            return@firstOrNull it.scheduledCharacters.contains(name) && it.isValidTimeToStart(gState.time + eta)
        }?.also { conf ->
            //----------------------------------------------------------------------------------Move to the Meeting
            if (place != conf.place) {
                if (routines.none { it is MoveRoutine })
                    return MoveRoutine().apply {
                        variables["movePlace"] = conf.place
                    }
            } else {
                return pickMeetingRoutine(name, conf)
            }
        }

        //3. Corruption for power: If the character is the leader of a party, and a party member is short of resources, steal resources from workplace to party member's home
        //Only attempted once a day or once a work, whichever is shorter.
        if (gState.time - (intVariables["corruptionTimer"] ?: 0) > ReadOnly.constInt("CorruptionTau") / ReadOnly.dt)
            if (gState.parties.values.any { it.leader == name }) {
                val party = gState.parties.values.find { it.leader == name }!!
                val rationThreshold =
                    ReadOnly.const("StealAmountMultiplier")//TODO: threshold change depending on member's trait and need
                val waterThreshold = ReadOnly.const("StealAmountMultiplier")
                val member = party.members.find {
                    gState.characters[it]!!.resources["ration"] <= rationThreshold * (gState.characters[it]!!.reliant) || gState.characters[it]!!.resources["water"] <= waterThreshold * (gState.characters[it]!!.reliant)
                }
                if (member != null && routines.none { it is StealRoutine }) {
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

        //4. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
        //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
        //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.

        gState.requests.values.firstOrNull {
            val eta =
                gState.places[it.action.tgtPlace]!!.shortestPathAndTimeTo(place)?.second ?: return@firstOrNull false
            return@firstOrNull (it.executeTime in gState.time - ReadOnly.constInt("CommandExecuteTolerance") + eta..gState.time + ReadOnly.constInt(
                "CommandExecuteTolerance"
            ) + eta || it.executeTime == 0) && (it.issuedBy.isEmpty() || it.issuedBy.sumOf {
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
        }?.also { request ->
            if (routines.none({ it is ExecuteCommandRoutine && it.variables["request"] == request.name }))
                return ExecuteCommandRoutine().also { it.variables["request"] = request.name }
        }

        //5. Supply resource
        gState.places.values.forEach { place1 -> //TODO: right now, supply resource to any place regardless of the division. In the future, agents will not supply resources to hostile divisions.
            place1.apparatuses.forEach { apparatus ->
                val res = place1.resourceShortOfHourly(apparatus) //Type of resource that is short of.
                if (res != null)
                //if there is a place within my division with the resource
                {
                    val resplace =
                        gState.places.values.filter {
                            it.responsibleDivision != "" && gState.parties[it.responsibleDivision]!!.members.contains(
                                name
                            ) && it.shortestPathAndTimeTo(place) != null
                        }
                            .maxByOrNull { it.resources[res] }
                    if (resplace != null && place1.name != resplace.name)
                    //start new routine if there is a place with all the conditions met.
                        if (resplace.resources[res] > 0 && routines.none { it is TransferResourceRoutine }) {
                            return TransferResourceRoutine().also {
                                it.res = res; it.source = resplace.name; it.dest = place1.name
                            }
                        }

                }
            }
        }

        //6. If there is some time, prepare information
        if (gState.scheduledMeetings.none {
                val eta =
                    gState.places[it.value.place]!!.shortestPathAndTimeTo(place)?.second ?: return@none false
                it.value.scheduledCharacters.contains(name) &&
                        it.value.isValidTimeToStart(gState.time + eta)
            })//If a Meeting is not soon
        {
            //If we haven't prapared info recently
            if (gState.informations.none { (_, information) ->
                    information.author == character.name && information.type == InformationType.ACTION && information.action is PrepareInfo
                            && gState.time - information.creationTime > ReadOnly.constInt("lengthOfDay") * 2
                } && routines.none { it is PrepareInfoRoutine }) {
                //If we haven't tried this branch in the current routine
                if (intVariables["try_prepare_info"] != 1) {
                    intVariables["try_prepare_info"] = 1
                    return PrepareInfoRoutine()
                }
            }
        }


        //7. If there is nothing above to do, move to a place that is the home of one of the parties of the character.
        //If already at home, wait.
        if (gState.parties.values.any { party -> party.home == place && party.members.contains(name) }) {
        } else
        //Move to a place that is the home of one of the parties of the character.
        {
            if (routines.none { it is MoveRoutine })
                try {
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
                } catch (e: NoSuchElementException) {
                    println("Warning: No place to commute found for $name")
                }


        }
        return null
    }

    //TODO: move name to class parameter
    private fun pickMeetingRoutine(name: String, conf: Meeting): Routine {
        when (conf.type) {
            Meeting.MeetingType.DIVISION_DAILY_CONFERENCE -> {
                if (name != gState.parties[conf.involvedParty]!!.leader) {
                    return AttendDivisionMeetingRoutine()
                } else {
                    return LeadDivisionMeetingRoutine()
                }
            }

            Meeting.MeetingType.DIVISION_LEADER_ELECTION -> {
                if (name != "ctrler") {
                    return AttendDivisionElectionRoutine()
                } else {
                    return LeadDivisionElectionRoutine()
                }
            }

            Meeting.MeetingType.TALK -> {
                return TalkRoutine()
            }
            //TODO: implement intentions

            Meeting.MeetingType.CABINET_DAILY_CONFERENCE -> {
                if (name != gState.parties["cabinet"]!!.leader) {
                    return AttendCabinetMeetingRoutine()
                } else {
                    return LeadCabinetMeetingRoutine()
                }
            }

            Meeting.MeetingType.TRIUMVIRATE_DAILY_CONFERENCE -> {
                return AttendTriumvirateRoutine()
            }

            else -> {
                TODO(conf.type.toString())
            }
        }
    }

    override fun execute(name: String, place: String): GameAction {

        //TODO: If there is no subroutine to do, move to a place that is the home of one of the parties of the character.
        //If already at home, wait.
        if (gState.parties.values.any { party -> party.home == place && party.members.contains(name) }) {
            return Wait(name, place)
        }
        return Wait(name, place) //If no subroutine is found, wait at the current place.
    }

    override fun endCondition(name: String, place: String): Boolean {
        //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest. (Which is checked earlier.)
        return (gState.hour !in 8..18)
    }

    @Transient
    override val availableActions = listOf("Eat", "Sleep", "Wait")
}