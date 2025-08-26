package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.NPCRoutines.IMeetingRoutine
import com.titaniumPolitics.game.core.NPCRoutines.Routine
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.LeaveMeeting
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
sealed class Agent : GameStateElement() {
    private var _name: String? = null
    override val name: String
        get() = _name ?: parent.nonPlayerAgents.filter { it.value == this }.keys.first().also { _name = it }
    val character: Character
        get() = parent.characters[name]!!
    val place
        get() = parent.places.values.find { it.characters.contains(name) }!!.name

    open fun chooseAction(): GameAction {
        return Wait(character.name, place)
    }

    //Any action that has to be executed before executing the current routine.
    fun blockExecution(routines: List<Routine>): GameAction? {
        //Leave meeting or conference if the routine was changed.
        //This allows the character to leave the meeting if it has a higher priority routine.
        //In this case, attendMeetingRoutine is still alive in the queue,
        //but it will be removed immediately when it becomes the current routine, as the character is not in a meeting.
        if (routines.isEmpty()) return null
        if (character.currentMeeting != null && routines[0].let {
                it !is IMeetingRoutine || it.meetingName != parent.meetingName(
                    character.currentMeeting!!
                )
            }) {
            LeaveMeeting(name, place).also {
                it.injectParent(parent)
                if (it.isValid()) return it
            }
            //If leaving meeting is not possible, try ending speech.
            EndSpeech(name, place, character.currentMeeting!!.currentCharacters.first { it != name }).also {
                it.injectParent(parent)
                if (it.isValid()) return it
            }

            //If neither leaving meeting nor ending speech is possible, wait.
            return Wait(name, place)
        }
        return null
    }

    open fun printStatus(): String {
        return ("Agent $name at $place")
    }

}