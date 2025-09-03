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
    val meetingsAttended = hashSetOf<String>()

    init {
        priority = PRIORITY_WORK
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {

        //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest. (Which is checked earlier.)
        if (!isWorkCondition(name, place, workplace, gState))
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

        return WorkNonMeetingRoutine(workplace)
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
        //Never fail the work routine itself.
    }
}