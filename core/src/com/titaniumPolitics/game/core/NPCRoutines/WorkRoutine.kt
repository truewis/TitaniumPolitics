package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class WorkRoutine() : Routine() {
    val meetingsAttended = hashSetOf<String>()

    init {
        priority = PRIORITY_WORK
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val character = gState.characters[name]!!

        //These routines will start even if the character is in a meeting./////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (subroutines.any { it is IMeetingRoutine })//I am already in a meeting, do not start a new routine.
            return null

        //I am forced into a meeting. Pick a meeting routine. Do not attend the meeting if it is already attended.
        if (character.currentMeeting != null) {
            if (gState.meetingName(character.currentMeeting!!) in meetingsAttended)
                return null//LeaveMeeting must be issued by NonPlayerAgent.
            return pickMeetingRoutine(name, character.currentMeeting!!)
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //1. If an accident happened in the place of my control, investigate and clear it.
        gState.places.values.firstOrNull {
            it.responsibleDivision != null && gState.parties[it.responsibleDivision]!!.members.contains(
                name
            ) && it.isAccidentScene
        }?.also { place ->
            if (subroutines.none { it is InvestigateAndClearAccidentRoutine && it.variables["place"] == place.name }) {
                //If there is no routine to investigate and clear the accident in this place, create a new one.
                return InvestigateAndClearAccidentRoutine().apply {
                    variables["place"] = place.name
                }
            }
        }

        //2. If missed a conference
        val missingMeeting = gState.ongoingMeetings.values
            .firstOrNull { it.scheduledCharacters.contains(name) && !it.currentCharacters.contains(name) }

        //Do not attend the meeting if it is already attended.
        if (missingMeeting != null && gState.meetingName(missingMeeting) !in meetingsAttended) {
            // Move to the meeting if not already there
            if (place != missingMeeting.place) {
                if (subroutines.none { it is MoveRoutine }) {
                    return MoveRoutine().apply {
                        variables["movePlace"] = missingMeeting.place
                        priority = PRIORITY_MEETING//Higher priority
                    }
                }
            } else {
                return pickMeetingRoutine(name, missingMeeting)

            }
        }

        //3. If a conference is scheduled
        gState.scheduledMeetings.values.firstOrNull {
            if (!it.scheduledCharacters.contains(name)) return@firstOrNull false //If I am not scheduled to attend this meeting, skip it.
            val eta = gState.places[it.place]!!.shortestPathAndTimeTo(place)?.second ?: return@firstOrNull false
            return@firstOrNull it.isValidTimeToStart(gState.time + eta) || it.isValidTimeToStart(gState.time + eta + 30)
        }?.also { conf ->
            //----------------------------------------------------------------------------------Move to the Meeting
            if (place != conf.place) {
                if (subroutines.none { it is MoveRoutine })
                    return MoveRoutine().apply {
                        variables["movePlace"] = conf.place
                        priority = PRIORITY_MEETING//Higher priority
                    }
            } else {
                if (conf.isValidTimeToStart(gState.time))
                    return pickMeetingRoutine(name, conf)
                else
                    if (subroutines.none { it is WaitRoutine })
                        return WaitRoutine().apply {
                            priority = PRIORITY_MEETING - 10//Higher priority.
                            until = this@WorkRoutine.gState.time + 5
                        } //Wait until the meeting is valid to start. This is necessary if the character arrives early to the meeting place.
            }
        }

        //4. Corruption for power: If the character is the leader of a party, and a party member is short of resources, steal resources from workplace to party member's home
        //Only attempted once a day or once a work, whichever is shorter.
        if (gState.time - (intVariables["corruptionTimer"] ?: 0) > ReadOnly.constInt("CorruptionTau") / ReadOnly.DT)
            if (gState.parties.values.any { it.leader == name }) {
                val party = gState.parties.values.find { it.leader == name }!!
                val rationThreshold =
                    ReadOnly.const("StealAmountMultiplier")//TODO: threshold change depending on member's trait and need
                val waterThreshold = ReadOnly.const("StealAmountMultiplier")
                val member = party.members.find {
                    gState.characters[it]!!.resources["ration"] <= rationThreshold * (gState.characters[it]!!.reliant) || gState.characters[it]!!.resources["water"] <= waterThreshold * (gState.characters[it]!!.reliant)
                }
                if (member != null && subroutines.none { it is StealRoutine }) {
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
        //5. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
        //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
        //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.

        gState.requests.values.firstOrNull {
            if (name !in it.issuedTo) return@firstOrNull false
            val eta =
                gState.places[it.action.tgtPlace]!!.shortestPathAndTimeTo(place)?.second ?: return@firstOrNull false
            return@firstOrNull (it.executeTime in gState.time - ReadOnly.constInt("CommandExecuteTolerance") + eta..gState.time + ReadOnly.constInt(
                "CommandExecuteTolerance"
            ) + eta || it.executeTime == 0) && (it.issuedBy.isEmpty() /*System request must be executed regardless of mutualities.*/ || it.issuedBy.sumOf {
                gState.getMutuality(
                    name,
                    it
                )
            } / it.issuedBy.size > it.difficulty()) && GameEngine.availableActions(
                gState,
                it.action.tgtPlace,
                name
            )
                .contains(it.action.javaClass.simpleName) //If the character is not in a meeting, we can move to other places to execute the command, so we do not check if the place is here.

        }?.also { request ->
            if (subroutines.none { it is ExecuteCommandRoutine && it.variables["request"] == request.name })
                return ExecuteCommandRoutine().also {
                    it.variables["request"] = request.name
                    it.priority =
                        PRIORITY_WORK + 400
                }
        }
        //6. Supply resource
        gState.places.values.forEach { place1 ->
            place1.apparatuses.forEach { apparatus ->
                val res = place1.resourceShortOfHourly(apparatus) //Type of resource that is short of.
                if (res != null)
                //if there is a place within my division with the resource
                {
                    val resplace =
                        gState.places.values.filter {
                            it.responsibleDivision != null && gState.parties[it.responsibleDivision]!!.members.contains(
                                name
                            ) && it.shortestPathAndTimeTo(place) != null
                        }
                            .maxByOrNull { it.resources[res] }
                    if (resplace != null && place1.name != resplace.name)
                    //start new routine if there is a place with all the conditions met.
                        if (resplace.resources[res] > 0 && subroutines.none { it is TransferResourceRoutine }) {
                            return TransferResourceRoutine().also {
                                it.res = res; it.source = resplace.name; it.dest = place1.name
                            }
                        }

                }
            }
        }

        //7. If there is some time, prepare information
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
                } && subroutines.none { it is PrepareInfoRoutine }) {
                //If we haven't tried this branch in the current routine
                if (intVariables["try_prepare_info"] != 1) {
                    intVariables["try_prepare_info"] = 1
                    return PrepareInfoRoutine()
                }
            }
        }

        //8. Hire a new employee if there is a vacancy in the party.
        gState.parties.values.filter { party ->
            party.leader == name
        }.forEach { party ->
            party.vacancyRole()?.let {
                if (subroutines.none { it is HireRoutine }) {
                    return HireRoutine().apply {
                        variables["party"] = party.name; variables["role"] = it
                    }
                }
            }
        }

//
//        //8. If there is nothing above to do, move to a place that is the home of one of the parties of the character.
//        //If already at home, wait.
//        if (gState.parties.values.any { party -> party.home == place && party.members.contains(name) }) {
//        } else
//        //Move to a place that is the home of one of the parties of the character.
//        {
//            if (subroutines.none { it is MoveRoutine })
//                try {
//                    return MoveRoutine().apply {
//                        gState = this@WorkRoutine.gState
//                        variables["movePlace"] = gState.places.values.filter { place ->
//                            gState.parties.values.any { party ->
//                                party.home == place.name && party.members.contains(
//                                    name
//                                )
//                            }
//                        }.random().name
//                    }
//                } catch (e: NoSuchElementException) {
//                    Logger.write("Warning: No place to commute found for $name", Logger.LogLevel.INFO)
//                }
//
//
//        }
        return null
    }

    //TODO: move name to class parameter
    private fun pickMeetingRoutine(name: String, conf: Meeting): Routine {
        meetingsAttended += gState.meetingName(conf)
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

        //Wait until there is some routine available above.
        return Wait(name, place) //If no subroutine is found, wait at the current place.
    }

    override fun endCondition(name: String, place: String): Boolean {
        //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest. (Which is checked earlier.)
        return !isWorkHourWithETA(gState, place, variables["workplace"]!!, (1 / ReadOnly.DTH).toInt())
                || gState.characters[name]!!.health <= ReadOnly.const("CriticalHealth")
    }

    @Transient
    override val availableActions = listOf("Eat", "Sleep", "Wait")
}