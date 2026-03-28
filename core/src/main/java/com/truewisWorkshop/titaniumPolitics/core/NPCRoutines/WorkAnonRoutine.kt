package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class WorkAnonRoutine(var workplace: String) : Routine() {
    val workplaceObj get() = gState.places[this@WorkAnonRoutine.workplace]!!
    var triedGoingToWork = false
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (!isWorkCondition(name, place, workplace, gState)
        )
            return success()
        if (place != workplaceObj.name && !triedGoingToWork) {
            triedGoingToWork = true
            return MoveRoutine(workplaceObj.name)//Add a move routine with higher priority.
        }

        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return Wait(name, place)
    }
}
