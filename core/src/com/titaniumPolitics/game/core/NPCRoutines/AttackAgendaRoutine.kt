package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.EndMeeting
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class AttackAgendaRoutine(val agendaIndex: Int) : Routine(), IMeetingRoutine {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        return null
    }

    //TODO: Also check SupportAgendaRoutine.
    override fun execute(name: String, place: String): GameAction {
        executeDone = true
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting!!
        if (conf.currentSpeaker != name) {
            return Wait(name, place)
        } else //If it is my turn to speak
        {
            //Check if I have any information to support the agenda.
            val attackingInfo = character.preparedInfoKeys.filter {
                conf.agendas[agendaIndex].effectivity(
                    gState,
                    conf,
                    gState.informations[it]!!,
                    character
                ) < 0.0
            }.minByOrNull {
                conf.agendas[agendaIndex].effectivity(
                    gState,
                    conf,
                    gState.informations[it]!!,
                    character
                )
            }
            if (attackingInfo != null) {
                //If I have supporting information, add it to the agenda.
                return AddInfo(name, place, attackingInfo, this@AttackAgendaRoutine.agendaIndex, gState)
            }
            //If there is no supporting information, end speech.
            val nextSpeaker = conf.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }
    }

    //TODO: Also check SupportAgendaRoutine.
    override fun endCondition(name: String, place: String): Boolean {
        // If I have no prepared information not presented in the meeting, end the routine.
        val character = gState.characters[name]!!
        val conf = character.currentMeeting!!
        if (character.preparedInfoKeys.none { key ->
                (conf.currentCharacters - gState.informations[key]!!.knownTo).isNotEmpty()
            })
            return true
        return executeDone
    }
}