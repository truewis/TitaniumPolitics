package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class PrepareInfoRoutine() : Routine() {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (place != "home_${name}") {
            if (subroutines.none { it is MoveRoutine })
                return MoveRoutine("home_${name}")//Add a move routine with higher priority.
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
        Logger.write("$name: Cannot move to home_${name}. Terminating the prepareInfoRoutine......")
        failed = true
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        return executeDone
    }

}