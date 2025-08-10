package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class SupportAgendaRoutine() : Routine(), IMeetingRoutine {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        return null
    }

    //TODO: Also check AttackAgendaRoutine.
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
            val supportingInfo = character.preparedInfoKeys.filter {
                conf.agendas[intVariables["agendaIndex"]!!].effectivity(
                    gState,
                    conf,
                    gState.informations[it]!!,
                    character
                ) > 0.0
            }.maxByOrNull {
                conf.agendas[intVariables["agendaIndex"]!!].effectivity(
                    gState,
                    conf,
                    gState.informations[it]!!,
                    character
                )
            }
            if (supportingInfo != null) {
                //If I have supporting information, add it to the agenda.
                return AddInfo(name, place).also {
                    it.injectParent(gState)
                    it.infoKey = supportingInfo
                    it.agendaIndex = intVariables["agendaIndex"]!!
                }
            }
            //If there is no supporting information, end speech.
            return EndSpeech(name, place).also {
                it.nextSpeaker = conf.currentCharacters.filter { it != name }
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            }
        }
    }

    //TODO: Also check AttackAgendaRoutine.
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