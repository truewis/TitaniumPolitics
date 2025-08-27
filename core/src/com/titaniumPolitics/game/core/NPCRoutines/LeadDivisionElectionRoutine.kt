package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class LeadDivisionElectionRoutine(override val meetingName: String) : MeetingRoutine() {
    init {
        priority = PRIORITY_MEETING
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        val party = gState.parties[meeting.involvedParty]!!
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (meeting.currentSpeaker != name) {
            return interceptCondition(name, place)
        } else //If it is my turn to speak
        {
            //finish nomination if there are three candidates or more
            //or the nomination time is over.
            if (meeting.agendas.count { it.type == AgendaType.NOMINATE } >= 3 || gState.time - meeting.time >= ReadOnly.constInt(
                    "maxNominationDuration"
                )) {
                FinishNomination(name, place).let {
                    it.injectParent(gState)
                    if (it.isValid()) return it
                }
            }
            //Start voting if it is valid to do so.
            StartVoting(name, place).let {
                it.injectParent(gState)
                if (it.isValid()) return it
            }

            //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
            //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
            //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
            executeRequestInMeeting(name, place)?.let { return it }


            //1. Nominate the person with the highest mutuality, if not nominated yet.
            //Note that nomination is only valid at the beginning of the conference.
            val nominee = gState.characters.keys.filter { it != name && party.members.contains(it) }
                .maxByOrNull { gState.getMutuality(name, it) }!!
            if (meeting.agendas.none { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee } && meeting.time == gState.time) {
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
            val nextSpeaker = meeting.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //TODO: do something in the meeting. Leave the meeting if nothing to do.

    }
}