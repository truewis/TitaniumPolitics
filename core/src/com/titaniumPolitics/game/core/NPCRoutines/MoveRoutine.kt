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
        val livingBy = gState.characters[name]!!.livingBy
        if (variables["movePlace"] == "home_$name")
        {
            if (place != livingBy)
            {
                return Move(name, place).also {
                    it.placeTo = livingBy
                }//If player is far from the home, go outside the home.
            } else
            {
                return Move(name, place).also {
                    it.placeTo = "home_$name"
                }//If player is outside the home, go inside.
            }
        } else
        {
            if (place == "home")//If the character is at home, go outside.
                return Move(name, place).also { it.placeTo = livingBy }
            return Move(name, place).also { it.placeTo = variables["movePlace"]!! }
        }

        //TODO: implement pathfinding. For now, just teleport to the place
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return place == variables["movePlace"]
        //TODO: when pathfinding fails, return true.
    }
}