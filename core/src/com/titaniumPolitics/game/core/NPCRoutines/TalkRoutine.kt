package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Apparatus
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class TalkRoutine : Routine(), IMeetingRoutine {
    var toWho = "" //The character to whom I want to talk
    var intention = "" //The intention of the character. It can be "requestResource" or "" (no particular intention).
    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.TALK) { "TalkRoutine can only be used in a meeting of type 'talk'." }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return Talk(name, place).also {
                it.who = toWho
            }
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name) {
            if (gState.getMutuality(
                    name,
                    conf.currentSpeaker
                ) > ReadOnly.const("SpeakerInterceptMutualityThreshold")
            )
                return Wait(name, place)
            else {
                val action = Intercept(name, place).also { it.injectParent(gState) }
                if (action.isValid())
                    return action
                return Wait(name, place)
            }
        } else {
            //If it is my turn to speak
            //Check if I had an intention
            when (intention) {
                "requestResource" -> {
                    intention = "" //The intention is resolved.
                    return NewAgenda(name, place).also {
                        it.agenda =
                            MeetingAgenda(
                                AgendaType.REQUEST, name, attachedRequest = Request(
                                    UnofficialResourceTransfer(
                                        variables["requestTo"]!!,
                                        tgtPlace = place
                                    ).apply {
                                        toWhere = "home_$name"
                                        fromHome =
                                            true//Transfer the {variables["requestTo"]!!}'s private resources to me.
                                        resources = Resources(
                                            variables["requestResourceType"]!! to
                                                    doubleVariables["requestResourceAmount"]!!
                                        )
                                    }//Created a command to transfer the resource.
                                    ,
                                    issuedTo = hashSetOf(variables["requestTo"]!!)
                                ).apply {

                                    executeTime = gState.time
                                    issuedBy = hashSetOf(name)
                                })
                    }
                }

                else -> {
                    //No particular intention
                    gossip(this.gState, name, place)?.also { return it }
                }
            }
            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            return EndSpeech(name, place).also {
                it.nextSpeaker = conf.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            }

        }


    }

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun endCondition(name: String, place: String): Boolean {
        val character = gState.characters[name]!!
        //If the conference is over, leave the routine.
        if (character.currentMeeting == null) {
            return true
        }
        character.currentMeeting!!
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        return routineStartTime + 7200 / ReadOnly.dt <= gState.time
    }

    companion object {
        fun gossip(gState: GameState, name: String, place: String): GameAction? {
            //Criticize the enemy. It is determined by individual mutuality.
            val enemy = gState.characters.minBy { ch ->
                gState.getMutuality(
                    name,
                    ch.key
                )
            }
            if (gState.getMutuality(
                    name,
                    enemy.key
                ) < ReadOnly.const("EnemyMutualityThreshold")
            )
                return NewAgenda(name, place).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.DENOUNCE, name).also {
                        it.subjectParams["character"] = enemy.key
                    }
                }

            //Praise the friend.
            //Criticize the enemy. It is determined by individual mutuality.
            val friend = gState.characters.maxBy { ch ->
                gState.getMutuality(
                    name,
                    ch.key
                )
            }
            if (gState.getMutuality(
                    name,
                    friend.key
                ) > ReadOnly.const("FriendMutualityThreshold")
            )
                return NewAgenda(name, place).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.PRAISE, name).also {
                        it.subjectParams["character"] = friend.key
                    }
                }
            return null
        }
    }
}