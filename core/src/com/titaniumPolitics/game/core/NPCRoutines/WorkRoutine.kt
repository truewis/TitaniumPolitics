package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class WorkRoutine(var workplace: String) : Routine() {
    var corruptionTimer = 0
    var try_prepare_info = 0
    val meetingsAttended = hashSetOf<String>()
    val failedRequests = hashSetOf<String>()

    init {
        priority = PRIORITY_WORK
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {

        //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest. (Which is checked earlier.)
        if (!isWorkHourWithETA(gState, name, place, workplace, IDTH)
            || gState.characters[name]!!.health <= ReadOnly.const("CriticalHealth")
            || gState.characters[name]!!.hunger >= ReadOnly.const("hungerThreshold")
            || gState.characters[name]!!.thirst >= ReadOnly.const("thirstThreshold")
        )
            return success()
        val character = gState.characters[name]!!

        //These routines will start even if the character is in a meeting./////////////////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        //I am forced into a meeting. Pick a meeting routine. Do not attend the meeting if it is already attended.
        if (character.currentMeeting != null) {
            if (subroutines.none {
                    it is MeetingRoutine && it.meetingName == gState.meetingName(character.currentMeeting!!)
                }
            //&& gState.meetingName(character.currentMeeting!!) !in meetingsAttended
            //I am already in the meeting, so no need to check if I have attended it already. In fact, I am obliged to create meeting routine again.
            )
                return pickMeetingRoutine(name, character.currentMeeting!!).apply {
                    priority = PRIORITY_MEETING //Higher priority than work.
                }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //1. If an accident happened in the place of my control, investigate and clear it.
        gState.places.values.firstOrNull {
            it.responsibleDivision != null && gState.parties[it.responsibleDivision]!!.members.contains(
                name
            ) && it.isAccidentScene
        }?.also { place ->
            if (subroutines.none { it is InvestigateAndClearAccidentRoutine && it.investigatePlace == place.name }) {
                //If there is no routine to investigate and clear the accident in this place, create a new one.
                return InvestigateAndClearAccidentRoutine(place.name).apply {
                    priority = PRIORITY_WORK + 10000 //Highest priority
                }
            }
        }

        //2. If missed a conference
        val missingMeeting = gState.ongoingMeetings.values
            .firstOrNull { it.scheduledCharacters.contains(name) && !it.currentCharacters.contains(name) }

        //Do not attend the meeting if it is already attended.
        if (missingMeeting != null && gState.meetingName(missingMeeting) !in meetingsAttended) {
            return pickMeetingRoutine(name, missingMeeting).apply {
                priority = PRIORITY_MEETING //Higher priority than work.
            }
        }

        //3. If a conference is scheduled
        gState.scheduledMeetings.values.firstOrNull {
            if (!it.scheduledCharacters.contains(name)) return@firstOrNull false //If I am not scheduled to attend this meeting, skip it.
            val eta = gState.places[it.place]!!.shortestPathAndTimeTo(place, name)?.second ?: return@firstOrNull false
            return@firstOrNull it.isValidTimeToStart(gState.time + eta) || it.isValidTimeToStart(gState.time + eta + 30)
        }?.also { conf ->
            //----------------------------------------------------------------------------------Move to the Meeting
            return pickMeetingRoutine(name, conf).apply {
                priority = PRIORITY_MEETING //Higher priority than work.
            }
        }

        //4. Corruption for power: If the character is the leader of a party, and a party member is short of resources, steal resources from workplace to party member's home
        //Only attempted once a day or once a work, whichever is shorter.
        if (gState.time - corruptionTimer > ReadOnly.constInt("CorruptionTau") / ReadOnly.DT)
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
                    corruptionTimer = gState.time
                    return StealRoutine(
                        wantedResource,
                        (gState.characters[member]!!.reliant + 1/*Numerically incorrect for anon agents, but ensure non zero value.*/) * ReadOnly.const(
                            "StealAmountMultiplier"
                        ),
                        member
                    )
                }
            }
        //5. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
        //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
        //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.

        gState.requests.values.firstOrNull {
            if (name !in it.issuedTo) return@firstOrNull false
            if (it.name in failedRequests) return@firstOrNull false //If I have already failed to execute this request, do not try again.
            val eta =
                gState.places[it.action.tgtPlace]!!.shortestPathAndTimeTo(place, name)?.second
                    ?: return@firstOrNull false
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
        //only if I am director
        if (character.type == Character.Type.DIRECTOR) {
            character.division?.divisionPlaces?.forEach { place1 ->
                place1.apparatuses.forEach { apparatus ->
                    val res = place1.resourceShortOfHourly(apparatus) //Type of resource that is short of.
                    if (res != null)
                    //if there is a place with the resource
                    {
                        val resplace =
                            gState.places.values.filter {
                                it.manager != null && it.shortestPathAndTimeTo(place, name) != null
                            }
                                .maxByOrNull { it.resources[res] }
                        if (resplace != null && place1.name != resplace.name && resplace.manager != name)
                        //start new routine if there is a place with all the conditions met.
                        //If the place with the resource has enough resource to supply apparatus for ten hours, and there is no existing transfer routine
                            if (resplace.resources[res] > apparatus.hourlyOperationResource[res] * 10) {
                                return AttendPrivateMeetingRoutine(
                                    resplace.manager!!,
                                    //To reduce the overhead, it is rational to transfer more resource than immediately needed if possible.
                                    MeetingAgenda(
                                        AgendaType.REQUEST, name, attachedRequest = Request(
                                            OfficialResourceTransfer(
                                                resplace.manager!!, resplace.name, place1.name, Resources(
                                                    res to max(
                                                        apparatus.hourlyOperationResource[res] * 10,
                                                        resplace.resources[res] * 0.3
                                                    )
                                                ), gState
                                            ),
                                            issuedTo = hashSetOf(resplace.manager!!),
                                            issuedBy = hashSetOf(name)
                                        )
                                    )
                                )
                            }

                    }
                }
            }
        }

        //6. Repair Apparatus
        //only if I am director
        if (character.type == Character.Type.DIRECTOR) {
            character.division?.divisionPlaces?.forEach { place1 ->
                place1.apparatuses.forEach { apparatus ->
                    if (apparatus.durability < 70f)
                    //If I am engineer myself, repair directly.
                        if ("engineer" in character.trait) {
                            return RepairApparatusRoutine(apparatus.ID)
                        } else
                        //Pick an engineer with the highest mutuality
                        {
                            val engineer = gState.characters.values.filter {
                                "engineer" in it.trait && it.name != name
                            }.maxByOrNull { gState.getMutuality(name, it.name) }?.run {
                                return AttendPrivateMeetingRoutine(
                                    this.name,
                                    MeetingAgenda(
                                        AgendaType.REQUEST, name, attachedRequest = Request(
                                            Repair(this.name, place1.name, apparatus.ID, gState),
                                            issuedTo = hashSetOf(this.name),
                                            issuedBy = hashSetOf(name)
                                        )
                                    )
                                )
                            }

                        }
                }
            }
        }

        //7. If there is some time, prepare information
        if (subroutines.none { it is PrepareInfoRoutine }) {
            if (gState.scheduledMeetings.none {
                    val eta =
                        gState.places[it.value.place]!!.shortestPathAndTimeTo(place, name)?.second ?: return@none false
                    it.value.scheduledCharacters.contains(name) &&
                            it.value.isValidTimeToStart(gState.time + eta)
                })//If a Meeting is not soon
            {
                //If we haven't prapared info recently
                if (gState.informations.none { (_, information) ->
                        information.author == character.name && information.type == InformationType.ACTION && information.action is PrepareInfo
                                && gState.time - information.creationTime > ReadOnly.constInt("lengthOfDay") * 2
                    }) {
                    //If we haven't tried this branch in the current routine
                    if (try_prepare_info == 0) {
                        try_prepare_info += 1
                        return PrepareInfoRoutine().apply {
                            priority = PRIORITY_WORK + 70 //Higher priority than work.
                        }
                    }
                }
            }
        }
        //8. Hire a new employee if there is a vacancy in the party.
        gState.parties.values.filter { party ->
            party.leader == name
        }.forEach { party ->
            when (party.type) {
                Party.Type.WORKPLACE -> {
                    party.vacancyRole()?.let { role ->
                        if (subroutines.none { it is HireRoutine }) {
                            return HireRoutine(party = party.name, role = role, null)
                        }
                    }
                }

                Party.Type.DIVISION -> {
                    party.divisionPlaces.firstOrNull {
                        it.manager == null
                    }?.let { place ->
                        if (subroutines.none { it is HireRoutine }) {
                            return HireRoutine(party = party.name, role = null, place.name)
                        }
                    }
                }
                //Cabinet members are not hired, they are elected within their division.
                else -> {}
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
    private fun pickMeetingRoutine(name: String, conf: Meeting): MeetingRoutine {
        meetingsAttended += gState.meetingName(conf)
        when (conf.type) {
            Meeting.MeetingType.DIVISION_DAILY_CONFERENCE -> {
                if (name != gState.parties[conf.involvedParty]!!.leader) {
                    return AttendDivisionMeetingRoutine(gState.meetingName(conf))
                } else {
                    return LeadDivisionMeetingRoutine(gState.meetingName(conf))
                }
            }

            Meeting.MeetingType.DIVISION_LEADER_ELECTION -> {
                if (name != "ctrler") {
                    return AttendDivisionElectionRoutine(gState.meetingName(conf))
                } else {
                    return LeadDivisionElectionRoutine(gState.meetingName(conf))
                }
            }

            Meeting.MeetingType.TALK -> {
                return AttendPrivateMeetingRoutine(scheduledMeetingName = gState.meetingName(conf))
            }

            Meeting.MeetingType.CABINET_DAILY_CONFERENCE -> {
                if (name != gState.parties["cabinet"]!!.leader) {
                    return AttendCabinetMeetingRoutine(gState.meetingName(conf))
                } else {
                    return LeadCabinetMeetingRoutine(gState.meetingName(conf))
                }
            }

            Meeting.MeetingType.TRIUMVIRATE_DAILY_CONFERENCE -> {
                return AttendTriumvirateRoutine(gState.meetingName(conf))
            }

            Meeting.MeetingType.BUDGET_PROPOSAL -> {
                return AttendDivisionBudgetProposalRoutine(gState.meetingName(conf))
            }

            Meeting.MeetingType.BUDGET_RESOLUTION -> {
                return AttendDivisionBudgetResolutionRoutine(gState.meetingName(conf))
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

    override fun onSubroutineFail(subroutine: Routine) {
        if (subroutine is ExecuteCommandRoutine) {
            val requestName = subroutine.variables["request"]!!
            failedRequests += requestName
        }
        //Never fail the work routine itself.
    }
}