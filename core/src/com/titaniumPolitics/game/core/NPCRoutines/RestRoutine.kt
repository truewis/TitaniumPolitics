package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class RestRoutine() : Routine()
{
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
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