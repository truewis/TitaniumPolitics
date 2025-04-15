package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class PrepareInfoRoutine() : Routine()
{
    var err = false;

    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        if (place != "home_${name}")
        {
            return MoveRoutine().apply {
                variables["movePlace"] = "home_${name}"
            }//Add a move routine with higher priority.
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        if (place == "home_${name}")
        {
            executeDone = true
            return PrepareInfo(name, place).also { it.recommendKeys()}
        }
        println("$name: Cannot move to home_${name} to prepare info ${variables["infoKey"]!!}. Terminating the routine......")
        err = true
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return executeDone || err
    }

}