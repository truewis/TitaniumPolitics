package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Move
import kotlinx.serialization.Serializable

@Serializable
class MoveRoutine() : Routine()
{
    var nextStop = ""
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        return Move(name, place).also {
            it.placeTo = nextStop
        }
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        if (place == variables["movePlace"])
        {
            executeDone = true
            return true
        } else
        {
            if (gState.places[place]!!.shortestPathAndTimeTo(variables["movePlace"]!!)?.also {
                    nextStop = it.first[1]
                } == null)
            {

                println("There is no path from $place to ${variables["movePlace"]}! Terminating moveRoutine...")
                executeDone = false
                return true

            }

        }

        return false
    }
}