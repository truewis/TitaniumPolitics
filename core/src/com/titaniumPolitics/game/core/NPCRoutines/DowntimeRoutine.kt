package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.JoinMeeting
import com.titaniumPolitics.game.core.gameActions.Talk
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DowntimeRoutine() : Routine()
{
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        val char = gState.characters[name]!!
        if (char.trait.contains("extrovert"))
        {
            if (place != "squareSouth")
                return MoveRoutine().apply {
                    variables["movePlace"] = "squareSouth"
                }//Add a move routine with higher priority.
            else
                return AttendMeetingRoutine().apply {
                    actionDelegated = Talk(name, place)
                }

        }

        //Otherwise, go home
        if (place != "home_$name")
            return MoveRoutine().apply {
                variables["movePlace"] = "home_$name"
            }//Add a move routine with higher priority.
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        return pickAction(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return (gState.hour in 8..18)
    }

    @Transient
    override val availableActions = listOf("Eat", "Sleep", "Wait")
}