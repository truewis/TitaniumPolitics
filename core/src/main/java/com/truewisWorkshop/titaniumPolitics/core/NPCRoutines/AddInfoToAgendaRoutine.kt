package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.NonPlayerAgent
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.EndMeeting
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class AddInfoToAgendaRoutine(val support: Boolean, val meetingName: String = "") : Routine(),
    IMeetingRoutine {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        try {
            val conf =
                character.currentMeeting!!
            conf.currentAgenda ?: return failed()
        } catch (e: Exception) {
            //Not in a meeting or agenda index is out of range.
            val agent = gState.nonPlayerAgents[name]!! as NonPlayerAgent
            println(name)
            println(agent.routines)
            println(meetingName)
            println(character.currentMeeting!!)
            println(character.currentMeeting!!.ID)
            println((agent.routines.first { it is AttendPrivateMeetingRoutine } as AttendPrivateMeetingRoutine).meetingName)
            println((agent.routines.first { it is AttendPrivateMeetingRoutine } as AttendPrivateMeetingRoutine).scheduledMeetingName)
            throw e
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting!!
        if (conf.currentSpeaker != name) {
            return Wait(name, place)
        } else //If it is my turn to speak
        {
            val currentAgenda = conf.currentAgenda ?: run {
                failed()
                val nextSpeaker = conf.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }
                    ?: return EndMeeting(name, place)
                return EndSpeech(name, place, nextSpeaker, gState)
            }
            //Check if I have any information to support the agenda.
            val addingInfo = gState.informations.filter { (key, value) -> name in value.knownTo }.keys.filter {
                currentAgenda.effectivity(
                    gState,
                    conf,
                    gState.informations[it]!!,
                    character
                ).first * (if (support) 1 else -1) > 0.0
            }.minByOrNull {
                currentAgenda.effectivity(
                    gState,
                    conf,
                    gState.informations[it]!!,
                    character
                ).first
            }
            if (addingInfo != null) {
                AddInfo(name, place, addingInfo, gState).also {
                    if (it.isValid()) {
                        success()
                        return it
                    }
                }
            }
            //If there is no supporting information, end speech.
            failed()
            val nextSpeaker = conf.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }
    }
}
