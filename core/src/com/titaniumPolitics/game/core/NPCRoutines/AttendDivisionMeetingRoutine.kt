package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendDivisionMeetingRoutine(override val meetingName: String) : MeetingRoutine() {
    var try_support_salary = 0

    init {
        priority = PRIORITY_MEETING
    }

    override fun newIMeetingRoutineCondition(
        name: String,
        place: String,
        subroutines: List<Routine>
    ): IMeetingRoutine? {
        val party = gState.parties[meeting.involvedParty]!!
        supportProofOfWork(name)?.let { return it }


        //Try supporting salary request.
        if (meeting.currentSpeaker == name && !party.isSalaryPaid) {
            //Check if there is already a salary request.
            if (meeting.agendas.none { it.type == AgendaType.REQUEST && it.attachedRequest!!.action is Salary }) {

            } else //If the agenda already exists, support it.
            {
                //If we haven't tried this branch in the current routine
                if (try_support_salary == 0) {
                    try_support_salary += 1
                    //If the agenda is already proposed, and we have a supporting information, support it.
                    return AddInfoToAgendaRoutine(
                        meeting.agendas.indexOfFirst { it.type == AgendaType.REQUEST && it.attachedRequest!!.action is Salary },
                        support = true
                    )//Add a routine, priority higher than work.
                }

            }
        }


        return null
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting!!
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name) {
            //If the meeting is not boring, but the mutuality to the speaker is low, intercept the speaker.
            if (routineStartTime + 7200 / ReadOnly.DT <= gState.time || meeting.currentAttention < 10)
                return LeaveMeeting(name, place)
            return interceptCondition(name, place)
        } else {
            val party = gState.parties[conf.involvedParty]!!

            //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
            //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
            //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
            executeRequestInMeeting(name, place)?.let { return it }

            //1. Propose proof of work if there is no proof of work agenda.
            proposeProofOfWork(name, place)?.let { return it }

            //If not division leader and salary is not paid, request salary.
            if (conf.currentSpeaker == name && !party.isSalaryPaid) {
                //Check if there is already a salary request.
                if (conf.agendas.none { it.type == AgendaType.REQUEST && it.attachedRequest?.action is Salary }) {
                    //Check if the division leader is present in the meeting.
                    if (party.leader in conf.currentCharacters) {
                        //Fill in the agenda based on variables in the routine, resource and character.
                        val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                            attachedRequest = Request(
                                Salary(
                                    party.leader!!,
                                    tgtPlace = party.home!!
                                ).apply {
                                    //TODO: adjust the salary, it.resources.
                                }//Created a command to transfer the resource.
                                ,
                                issuedTo = hashSetOf(party.leader!!), issuedBy = hashSetOf(name),
                                executeTime = gState.time
                            )
                        }
                        return NewAgenda(name, place, gState).also {
                            it.agenda = agenda
                            if (it.isValid()) {
                                return it
                            }
                        }
                    }
                }
            }

            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            val nextSpeaker = conf.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //If everything else, wait.
        return Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }
}