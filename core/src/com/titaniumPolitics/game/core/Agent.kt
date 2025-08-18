package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.NPCRoutines.IMeetingRoutine
import com.titaniumPolitics.game.core.NPCRoutines.Routine
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.LeaveMeeting
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
sealed class Agent : GameStateElement() {
    override val name: String
        get() = parent.nonPlayerAgents.filter { it.value == this }.keys.first()
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
        if ((routines[0] !is IMeetingRoutine && character.currentMeeting != null)) {
            return LeaveMeeting(name, place)
        }
        return null
    }

}