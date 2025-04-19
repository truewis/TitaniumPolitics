package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class TransferResourceRoutine() : Routine()
{
    var res = ""
    var source = ""
    var dest = ""
    override fun newRoutineCondition(name: String, place: String): Routine?
    {

        if (place != source)
        {
            return MoveRoutine().apply { variables["movePlace"] = source }
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        executeDone = true
        if (place == source)
            OfficialResourceTransfer(name, place).also {
                it.resources = hashMapOf(res to gState.places[place]!!.resources[res] / 2)
                it.toWhere = dest
                return it
            }
        else
            return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return executeDone
        //TODO: when pathfinding fails, return true.
    }
}