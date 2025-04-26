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
            when (conf.agendas[intVariables["agendaIndex"]!!].type)
            {

                AgendaType.PROOF_OF_WORK ->
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

                    //If there are any interesting (to this character) news about the division, share it.

                    val gossip = gState.informations.filter {
                        character.preparedInfoKeys.contains(
                            it.key
                        ) && it.value.tgtTime in gState.day * ReadOnly.constInt("lengthOfDay")..((gState.day+1) * ReadOnly.constInt(
                            "lengthOfDay"
                        ) - 1)
                                && (conf.currentCharacters - it.value.knownTo).isNotEmpty() //In order to present the info, someone must not know it. This also prevents sharing an information that is already shared in this meeting.
                    }.maxByOrNull { character.infoPreference(it.value) } //Share the most interesting news.

                    if (gossip != null && conf.agendas.any { it.type == AgendaType.PROOF_OF_WORK })//TODO: Attach the information to an appropriate agenda if proof of work does not exist.
                        return AddInfo(name, place).also {
                            it.infoKey = gossip.key
                            it.agendaIndex =
                                conf.agendas.indexOf(conf.agendas.find { it.type == AgendaType.PROOF_OF_WORK }!!)
                        }


                }

                AgendaType.NOMINATE, AgendaType.PRAISE ->
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

                AgendaType.DENOUNCE ->
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

                AgendaType.REQUEST -> TODO()
                AgendaType.PRAISE_PARTY -> TODO()
                AgendaType.DENOUNCE_PARTY -> TODO()
                AgendaType.BUDGET_PROPOSAL -> TODO()
                AgendaType.BUDGET_RESOLUTION -> TODO()
                AgendaType.APPOINT_MEETING -> TODO()
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