package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class WorkAnonRoutine() : Routine() {
    val workPlace get() = gState.places[this@WorkAnonRoutine.variables["workPlace"]!!]!!
    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        if (place != workPlace.name)
            if (routines.none { it is MoveRoutine })
                return MoveRoutine().apply {
                    variables["movePlace"] =
                        workPlace.name
                }//Add a move routine with higher priority.
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        return (gState.hour !in workPlace.workHoursStart..workPlace.workHoursEnd)
    }

    @Transient
    override val availableActions = listOf("Move", "Wait")
}