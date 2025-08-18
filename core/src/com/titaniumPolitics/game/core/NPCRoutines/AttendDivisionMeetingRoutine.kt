package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendDivisionMeetingRoutine : Routine(), IMeetingRoutine {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE) {
            "AttendDivisionMeetingRoutine can only be used for divisionDailyConference, but got ${conf.type}"
        }

        val party = gState.parties[conf.involvedParty]!!
        check(party.leader != name) {
            "AttendDivisionMeetingRoutine can only be used for divisionDailyConference when not the leader, but got $name as the leader of ${party.name}"
        }

        supportProofOfWork(conf, name)?.let { return it }


        //Try supporting salary request.
        if (conf.currentSpeaker == name && !party.isSalaryPaid) {
            //Check if there is already a salary request.
            if (conf.agendas.none { it.type == AgendaType.REQUEST && it.attachedRequest!!.action is Salary }) {

            } else //If the agenda already exists, support it.
            {
                //If we haven't tried this branch in the current routine
                if (intVariables["try_support_salary"] != 1) {
                    //If the agenda is already proposed, and we have a supporting information, support it.
                    intVariables["try_support_salary"] = 1
                    return SupportAgendaRoutine(conf.agendas.indexOfFirst { it.type == AgendaType.REQUEST && it.attachedRequest!!.action is Salary })//Add a routine, priority higher than work.
                }

            }
        }


        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting
        if (conf == null) {
            JoinMeeting(name, place).apply {
                injectParent(gState)
                meetingName =
                    gState.ongoingMeetings.filter { it.value.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE && it.value.place == place }
                        .keys.firstOrNull()
                        ?: return@apply
                if (isValid())
                    return this
            }
            StartMeeting(name, place).apply {
                injectParent(gState)
                if (isValid())
                    return this
            }
            return Wait(name, place).also {
            } //If no meeting found, wait. Note that this action is only executed once because the routine will end after this action.
            //This happens if the number of people condition of the meeting is not met.
        }
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name) {
            //If the meeting is not boring, but the mutuality to the speaker is low, intercept the speaker.
            return interceptCondition(conf, name, place)
        } else {
            val party = gState.parties[conf.involvedParty]!!

            //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
            //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
            //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
            executeRequestInMeeting(name, place)?.let { return it }

            //1. Propose proof of work if there is no proof of work agenda.
            proposeProofOfWork(conf, name, place)?.let { return it }

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
                                issuedTo = hashSetOf(party.leader!!), issuedBy = hashSetOf(name)
                            ).apply {
                                executeTime = gState.time

                            }
                        }
                        return NewAgenda(name, place).also {
                            it.agenda = agenda
                        }
                    }
                }
            }

            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            val nextSpeaker = conf.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place).also {
                it.nextSpeaker = nextSpeaker
            }
        }

        //If everything else, wait.
        return Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun endCondition(name: String, place: String): Boolean {
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //See NonPlayerAgent.selectRoutine()
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        return meetingRoutineEndCondition(name, Meeting.MeetingType.DIVISION_DAILY_CONFERENCE)
    }
}