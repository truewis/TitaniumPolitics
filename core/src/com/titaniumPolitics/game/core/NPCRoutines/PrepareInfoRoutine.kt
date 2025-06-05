package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class PrepareInfoRoutine() : Routine() {
    var err = false

    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        if (place != "home_${name}") {
            if (routines.none { it is MoveRoutine })
                return MoveRoutine().apply {
                    variables["movePlace"] = "home_${name}"
                }//Add a move routine with higher priority.
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        if (place == "home_${name}") {
            executeDone = true
            return PrepareInfo(name, place).also {
                it.injectParent(gState)
                it.recommendKeys()
            }
        }
        Logger.warning("$name: Cannot move to home_${name}. Terminating the prepareInfoRoutine......")
        err = true
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        return executeDone || err
    }

}