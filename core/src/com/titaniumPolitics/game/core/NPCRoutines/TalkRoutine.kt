package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class TalkRoutine(val toWho: String? = null, val agenda: MeetingAgenda? = null) : Routine(), IMeetingRoutine {


    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.TALK) { "TalkRoutine can only be used in a meeting of type 'talk'." }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return Talk(name, place, toWho!!)
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name) {
            return interceptCondition(conf, name, place)
        } else {
            //If it is my turn to speak
            //Check if I had an intention
            if (agenda != null) {
                return NewAgenda(name, place).also {
                    it.agenda = agenda

                }
            } else {
                //No particular intention
                gossip(this.gState, name, place)?.also { return it }
            }

            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            return EndSpeech(
                name, place, conf.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            )

        }


    }

    override fun endCondition(name: String, place: String): Boolean {
        return meetingRoutineEndCondition(name, Meeting.MeetingType.TALK)
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