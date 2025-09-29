package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class WaitRoutine() : Routine() {
    var until = 0
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (gState.hour >= until) {
            Logger.write("$name: Wait routine ended at hour $until.", Logger.LogLevel.INFO)
            return success()
        }
        if (gState.characters[name]!!.currentMeeting != null)
            return failed()
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return Wait(name, place)
    }
}