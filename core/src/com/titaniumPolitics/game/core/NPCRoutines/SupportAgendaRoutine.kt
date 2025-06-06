package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class SupportAgendaRoutine() : Routine(), IMeetingRoutine
{
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        return null
    }

    //TODO: Also check AttackAgendaRoutine.
    override fun execute(name: String, place: String): GameAction
    {
        executeDone = true
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting!!
        if (conf.currentSpeaker != name)
        {
            return Wait(name, place)
        } else //If it is my turn to speak
        {
            when (variables["agenda"])
            {

                "proofOfWork" ->
                {
                    //if there is any supporting information, add it.
                    character.preparedInfoKeys.filter { key ->
                        gState.informations[key]!!.type == InformationType.ACTION
                                && character.finishedRequests.any {
                            gState.requests[it]!!.action == gState.informations[key]!!.action &&
                                    gState.requests[it]!!.issuedBy.any {
                                        character.currentMeeting!!.currentCharacters.contains(
                                            it
                                        )
                                    }
                        }
                    }.forEach { key ->
                        val action = AddInfo(name, place).also {
                            it.infoKey = key
                            it.agendaIndex = intVariables["agendaIndex"]!!
                        }
                        if (action.isValid())//In particular, if this information is not already presented in the meeting.
                            return action
                    }

                }

                "nomination", "praise" ->
                {
                    //if there is any supporting information, add it.
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

                "denounce" ->
                {
                    //if there is any supporting information, add it.
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
            }
            //If there is no supporting information, end speech.
            return EndSpeech(name, place).also {
                it.nextSpeaker = conf.currentCharacters.filter { it != name }
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            }
        }
    }

    //TODO: Also check AttackAgendaRoutine.
    override fun endCondition(name: String, place: String): Boolean
    {
        return executeDone
        //TODO: when pathfinding fails, return true.
    }
}