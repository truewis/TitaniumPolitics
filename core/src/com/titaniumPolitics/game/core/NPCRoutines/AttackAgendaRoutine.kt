package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttackAgendaRoutine() : Routine(), IMeetingRoutine
{
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        executeDone = true
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting!!
        when (variables["agenda"])
        {

            "proofOfWork" ->
            {
                //if there is any attacking information, add it.
            }

            "nomination", "praise" ->
            {
                //if there is any attacking information, add it.
                character.preparedInfoKeys.filter { key ->
                    gState.informations[key]!!.tgtCharacter == conf.agendas[intVariables["agendaIndex"]!!].subjectParams["character"]
                            && gState.characters[gState.informations[key]!!.tgtCharacter]!!.infoPreference(
                        gState.informations[key]!!
                    ) < 0
                }.forEach { key ->
                    val action = AddInfo(name, place).also {
                        it.infoKey = key
                        it.agendaIndex = intVariables["agendaIndex"]!!
                    }
                    if (action.isValid())//In particular, if this information is not already presented in the meeting.
                        return action
                }
            }

            "denounce" ->
            {
                //if there is any attacking information, add it.
                character.preparedInfoKeys.filter { key ->
                    gState.informations[key]!!.tgtCharacter == conf.agendas[intVariables["agendaIndex"]!!].subjectParams["character"]
                            && gState.characters[gState.informations[key]!!.tgtCharacter]!!.infoPreference(
                        gState.informations[key]!!
                    ) > 0
                }.forEach { key ->
                    val action = AddInfo(name, place).also {
                        it.infoKey = key
                        it.agendaIndex = intVariables["agendaIndex"]!!
                    }
                    if (action.isValid())//In particular, if this information is not already presented in the meeting.
                        return action
                }
            }
        }
        //If there is no supporting information, end speech.
        return EndSpeech(name, place).apply { chooseParams() }
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return executeDone
        //TODO: when pathfinding fails, return true.
    }
}