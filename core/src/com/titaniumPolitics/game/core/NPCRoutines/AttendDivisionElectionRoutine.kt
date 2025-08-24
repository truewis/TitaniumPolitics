package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendDivisionElectionRoutine : Routine(), IMeetingRoutine {
    var try_support_nomination = 0

    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION) {
            "LeadDivisionElectionRoutine can only be used in divisionLeaderElection , but got ${conf.type}"
        }
        val party = gState.parties[conf.involvedParty]!!
        check(party.members.contains(name)) {
            "AttendDivisionElectionRoutine can only be used for divisionLeaderElection when the character is a member of the party, but got $name not in ${party.name}"
        }


        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name) {
        } else {
            //1. Support the nominee with the highest mutuality.
            val nominee = gState.characters.keys.filter { it != name && party.members.contains(it) }
                .maxByOrNull { gState.getMutuality(name, it) }!!
            if (conf.agendas.none { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee } && conf.time == gState.time) {
            }
            //otherwise, support the nominee.
            else {
                //If we haven't tried this branch in the current routine
                if (try_support_nomination == 0) {
                    //If the agenda is already proposed, and we have a supporting information, support it.
                    try_support_nomination += 1
                    return SupportAgendaRoutine(conf.agendas.indexOfFirst { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee })
                }
                //After you support the nominee, attack the other nominees.
                val otherNominees =
                    gState.characters.keys.filter { it != name && it != nominee && conf.agendas.any { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee } }
                if (otherNominees.isNotEmpty()) {
                    return (
                            AttackAgendaRoutine(conf.agendas.indexOfFirst { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee }))//Add a routine, priority higher than work.

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
                    gState.ongoingMeetings.filter { it.value.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && it.value.place == place }
                        .keys.firstOrNull()
                        ?: return@apply
                if (isValid())
                    return this
            }
            return Wait(name, place).also {
            } //If no meeting found, wait. Note that this action is only executed once because the routine will end after this action.
            //This happens if the number of people condition of the meeting is not met.
        }
        val party = gState.parties[conf.involvedParty]!!
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name) {
            return interceptCondition(conf, name, place)
        } else //If it is my turn to speak
        {

            //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
            //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
            //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
            executeRequestInMeeting(name, place)?.let { return it }

            //1.Nominate the person with the highest mutuality, if not nominated yet.
            //Note that nomination is only valid at the beginning of the conference.
            val nominee = gState.characters.keys.filter { it != name && party.members.contains(it) }
                .maxByOrNull { gState.getMutuality(name, it) }!!
            if (conf.agendas.none { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee } && conf.time == gState.time) {
                return NewAgenda(name, place).also {
                    it.agenda =
                        MeetingAgenda(
                            AgendaType.NOMINATE,
                            author = name,
                            subjectParams = hashMapOf("character" to nominee)
                        )
                }
            }
            //otherwise, support the nominee.


//If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            return EndSpeech(name, place).also {
                it.nextSpeaker = conf.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            }
        }

        //TODO: do something in the meeting. Leave the meeting if nothing to do.

    }

    override fun endCondition(name: String, place: String): Boolean {
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //Don't end the routine until the election is over.
        return gState.parties[gState.characters[name]!!.currentMeeting!!.involvedParty]!!.leader != null
    }
}