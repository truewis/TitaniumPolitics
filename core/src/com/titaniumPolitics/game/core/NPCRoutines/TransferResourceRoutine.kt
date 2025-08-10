package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class TransferResourceRoutine() : Routine() {
    var res = ""
    var source = ""
    var dest = ""
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {

        if (place != source) {
            if (subroutines.none { it is MoveRoutine })
                return MoveRoutine().apply { variables["movePlace"] = source }
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        executeDone = true
        if (place == source)//TODO: do not transfer if the amount of resource is not enough for the destination place. Either here or in workRoutine.
            OfficialResourceTransfer(name, place).also {
                it.injectParent(gState)
                it.resources = Resources(res to gState.places[place]!!.resources[res] / 2)
                it.toWhere = dest
                if (it.isValid())
                    return it
            }
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        return executeDone
        //TODO: when pathfinding fails, return true.
    }
}