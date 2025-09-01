package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendTriumvirateRoutine(override val meetingName: String) : MeetingRoutine() {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newIMeetingRoutineCondition(
        name: String,
        place: String,
        subroutines: List<Routine>
    ): IMeetingRoutine? {
        val party = gState.parties[meeting.involvedParty]!!
        check(party.members.contains(name)) {
            "AttendTriumvirateRoutine can only be used for triumvirateDailyConference when the character is a member of the party, but got $name as a member of ${party.name}"
        }

        supportProofOfWork(name)?.let { return it }


        return null
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        val party = gState.parties[meeting.involvedParty]!!
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (meeting.currentSpeaker != name) {
            if (routineStartTime + 7200 / ReadOnly.DT <= gState.time || meeting.currentAttention < 10)
                return LeaveMeeting(name, place)
            return interceptCondition(name, place)
        } else //If it is my turn to speak
        {
            //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
            //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
            //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
            executeRequestInMeeting(name, place)?.let { return it }


            //1. No salary in Triumvirate meeting, so no need to support salary agenda.


            //2. request information about the commands issued today, by putting ProofOfWork agenda forward.
            proposeProofOfWork(name, place)?.let { return it }
            //3. Praise or criticize the members, if there is any relevant information.
            //It should be noted that the content of the information is not checked here. Think about this later.
            party.members.forEach { member ->
                if (member != name && gState.informations.values.any {
                        it.tgtCharacter == member && it.knownTo.contains(
                            name
                        )
                    }) {
                    //praise if the mutuality is high, criticize if the mutuality is low.
                    val mutuality = gState.getMutuality(name, member)
                    if (mutuality > 80) {
                        NewAgenda(name, place, gState).also {
                            it.agenda =
                                MeetingAgenda(
                                    AgendaType.PRAISE,
                                    name,
                                    subjectParams = hashMapOf("character" to member)
                                )
                            if (it.isValid()) return it
                        }
                    } else if (mutuality < 20) {
                        NewAgenda(name, place, gState).also {
                            it.agenda =
                                MeetingAgenda(
                                    AgendaType.DENOUNCE, name,
                                    subjectParams = hashMapOf("character" to member)
                                )
                            if (it.isValid()) return it
                        }
                    }
                }//TODO: there must be a cooldown, stored in party class.
            }
            //4. If it is not covered above, if the division is short of resources, share the information about the resource shortage.
            //However, right now, the resource information is available to everyone immediately, no need to share.

            //5. Criticize the common enemies of the division. It is determined by the party with the low mutuality with the division.
            val enemyParty = gState.parties.values.filter { it.name != meeting.involvedParty }
                .minBy { gState.getPartyMutuality(it.name, meeting.involvedParty!!) }.name
            if (gState.getPartyMutuality(
                    meeting.involvedParty!!,
                    enemyParty
                ) < ReadOnly.const("EnemyPartyMutualityThreshold")
            )
                NewAgenda(name, place, gState).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.DENOUNCE_PARTY, name).also {
                        it.subjectParams["party"] = enemyParty
                    }
                    if (action.isValid()) return action
                }
            //6. Triumvirate does not manage resources, so no need to adjust resource production.

            //7. Gossip
            AttendPrivateMeetingRoutine.gossip(gState, name, place)?.also { return it }
            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            val nextSpeaker = meeting.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }

}