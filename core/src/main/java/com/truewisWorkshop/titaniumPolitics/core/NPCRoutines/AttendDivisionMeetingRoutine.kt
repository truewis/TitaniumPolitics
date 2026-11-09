package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendDivisionMeetingRoutine(override val meetingName: String) : MeetingRoutine() {
    var try_support_salary = 0
    private val triedPolicyResponses = hashSetOf<String>()
    private val triedPolicyInterceptions = hashSetOf<String>()
    private val triedRequestResponses = hashSetOf<String>()

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

        val policyAgenda = meeting.currentAgenda?.takeIf {
            it.type == AgendaType.POLICY_CHANGE && it.author != name
        }
        if (meeting.currentSpeaker == name && policyAgenda != null) {
            val policy = policyAgenda.subjectParams["policy"] ?: return null
            val stance = PolicyCatalog.preferenceScore(party, policy, name, policyAgenda.author)
            val agendaKey = policyAgendaKey(policyAgenda)
            if (kotlin.math.abs(stance) >= PolicyCatalog.POLICY_STANCE_THRESHOLD &&
                triedPolicyResponses.add(agendaKey)
            ) {
                return AddInfoToAgendaRoutine(
                    support = stance > 0.0,
                    meetingName = gState.meetingName(meeting)
                )
            }
        }

        val bossRequestAgenda = meeting.currentAgenda?.takeIf {
            it.type == AgendaType.REQUEST &&
                it.author == party.leader &&
                it.attachedRequest?.action !is Salary
        }
        if (meeting.currentSpeaker == name && bossRequestAgenda != null) {
            val relationship = gState.getMutNorm(name, party.leader!!)
            val agendaKey =
                "${meeting.ID}:${meeting.agendas.indexOf(bossRequestAgenda)}:${bossRequestAgenda.author}"
            if (kotlin.math.abs(relationship) > 0.1 && triedRequestResponses.add(agendaKey)) {
                return AddInfoToAgendaRoutine(
                    support = relationship > 0.1,
                    meetingName = gState.meetingName(meeting)
                )
            }
        }

        //Try supporting salary request.
        if (meeting.currentSpeaker == name && !party.isSalaryPaid) {
            //Check if there is already a salary request.
            if (meeting.currentAgenda?.let { it.type == AgendaType.REQUEST && it.attachedRequest!!.action is Salary } != true) {

            } else //If the agenda already exists, support it.
            {
                //If we haven't tried this branch in the current routine
                if (try_support_salary == 0) {
                    try_support_salary += 1
                    //If the agenda is already proposed, and we have a supporting information, support it.
                    return AddInfoToAgendaRoutine(
                        support = true
                    )//Add a routine, priority higher than work.
                }

            }
        }


        return null
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val party = gState.parties[meeting.involvedParty]!!
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (meeting.currentSpeaker != name) {
            //If the meeting is not boring, but the mutuality to the speaker is low, intercept the speaker.
            if (sharedMeetingEndCondition())
                return LeaveMeeting(name, place)
            val policyAgenda = meeting.currentAgenda?.takeIf {
                it.type == AgendaType.POLICY_CHANGE && it.author != name
            }
            if (policyAgenda != null) {
                val policy = policyAgenda.subjectParams["policy"]
                val stance = policy?.let {
                    PolicyCatalog.preferenceScore(party, it, name, policyAgenda.author)
                } ?: 0.0
                val agendaKey = policyAgendaKey(policyAgenda)
                if (stance <= -PolicyCatalog.POLICY_STANCE_THRESHOLD &&
                    agendaKey !in triedPolicyInterceptions
                ) {
                    Intercept(name, place, gState).also {
                        if (it.isValid()) {
                            triedPolicyInterceptions += agendaKey
                            return it
                        }
                    }
                }
            }
            return interceptCondition(name, place)
        } else {
            //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
            //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
            //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
            executeRequestInMeeting(name, place)?.let { return it }

            //1. Propose proof of work if there is no proof of work agenda.
            proposeProofOfWork(name, place)?.let { return it }

            //If not division leader and salary is not paid, request salary.
            if (meeting.currentSpeaker == name && !party.isSalaryPaid) {
                //Check if there is already a salary request.
                if (meeting.agendas.none { it.type == AgendaType.REQUEST && it.attachedRequest?.action is Salary }) {
                    //Check if the division leader is present in the meeting.
                    if (party.leader in meeting.currentCharacters) {
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
                                executeTime = gState.time,
                                origin = "routine.AttendDivisionMeetingRoutine.salary"
                            )
                        }
                        NewAgenda(name, place, gState).also {
                            it.agenda = agenda
                            if (it.isValid()) {
                                return it
                            }
                        }
                    }
                }
            }
            endMeetingIfLowAttention(name, place)?.let { return it }
            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            val nextSpeaker = meeting.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //If everything else, wait.
        return Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }

    private fun policyAgendaKey(agenda: MeetingAgenda): String =
        "${meeting.ID}:${meeting.agendas.indexOf(agenda)}:${agenda.author}:${agenda.subjectParams["policy"]}"
}