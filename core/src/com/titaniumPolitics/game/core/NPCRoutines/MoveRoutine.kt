package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Move
import kotlinx.serialization.Serializable

@Serializable
class MoveRoutine() : Routine()
{
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        return Move(name, place).also {
            it.placeTo = gState.places[place]!!.shortestPathAndTimeTo(variables["movePlace"]!!)!!.first[1]
        }
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        if (place == variables["movePlace"])
        {
            executeDone = true
            return true
        } else if (gState.places[place]!!.shortestPathAndTimeTo(variables["movePlace"]!!) == null)
        {
            executeDone = false
            return true
        }
        return false
    }
}