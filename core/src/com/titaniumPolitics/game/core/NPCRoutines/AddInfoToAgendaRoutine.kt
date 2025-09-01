package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.EndMeeting
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class AddInfoToAgendaRoutine(val agendaIndex: Int, val support: Boolean) : Routine(),
    IMeetingRoutine {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        // If I have no prepared information not presented in the meeting, end the routine.
        val character = gState.characters[name]!!
        val conf = character.currentMeeting ?: return failed()
        if (character.preparedInfoKeys.none { key ->
                (conf.currentCharacters - gState.informations[key]!!.knownTo).isNotEmpty()
            })
            return failed()
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
            //Check if I have any information to support the agenda.
            val addingInfo = character.preparedInfoKeys.filter {
                conf.agendas[agendaIndex].effectivity(
                    gState,
                    conf,
                    gState.informations[it]!!,
                    character
                ) * (if (support) 1 else -1) > 0.0
            }.minByOrNull {
                conf.agendas[agendaIndex].effectivity(
                    gState,
                    conf,
                    gState.informations[it]!!,
                    character
                )
            }
            if (addingInfo != null) {

                success()
                //If I have supporting information, add it to the agenda.
                return AddInfo(name, place, addingInfo, this@AddInfoToAgendaRoutine.agendaIndex, gState)
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