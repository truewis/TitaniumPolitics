package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class LeadDivisionElectionRoutine(override val meetingName: String) : Routine(), IMeetingRoutine {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        gState.characters[name]!!
        val conf =
            gState.ongoingMeetings[meetingName] ?: gState.scheduledMeetings[meetingName]
        meetingStartMethod(conf, place)?.let { return it }
        if (conf == null) return null
        check(name == "ctrler") {
            "LeadDivisionElectionRoutine can only be used by the ctrler, but got $name"
        }

        //Don't do anything because the controller is not a division member.


        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting
        if (conf == null) {
            StartMeeting(name, place).apply {
                injectParent(gState)
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
            //finish nomination if there are three candidates or more
            //or the nomination time is over.
            if (conf.agendas.count { it.type == AgendaType.NOMINATE } >= 3 || gState.time - conf.time >= ReadOnly.constInt(
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
            val nextSpeaker = conf.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //TODO: do something in the meeting. Leave the meeting if nothing to do.

    }

    override fun successCondition(name: String, place: String): Boolean {
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //Don't end the routine until the election is over.
        return gState.parties[gState.characters[name]!!.currentMeeting!!.involvedParty]!!.leader != null
    }
}