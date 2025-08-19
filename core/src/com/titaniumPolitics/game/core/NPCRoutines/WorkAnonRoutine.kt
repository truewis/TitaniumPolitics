package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class WorkAnonRoutine(var workplace: String? = null) : Routine() {
    val workplaceObj get() = gState.places[this@WorkAnonRoutine.workplace]!!
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (place != workplaceObj.name)
            if (subroutines.none { it is MoveRoutine })
                return MoveRoutine(workplaceObj.name)//Add a move routine with higher priority.
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        return (gState.hour !in workplaceObj.workHoursStart..workplaceObj.workHoursEnd)
    }

    @Transient
    override val availableActions = listOf("Move", "Wait")
}