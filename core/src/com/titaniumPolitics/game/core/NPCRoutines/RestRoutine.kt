package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class RestRoutine() : Routine() {
    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {

        if (place != "home_$name")
            return MoveRoutine().apply {
                variables["movePlace"] = "home_$name"
            }//Add a move routine with higher priority.
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return pickAction(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        if (gState.characters[name]!!.health < ReadOnly.const("CriticalHealth")) return false
        if (variables["workPlace"] == null)
            return (gState.hour in 8..18)
        else
            return (gState.hour in gState.places[variables["workPlace"]!!]!!.workHours)
    }

    @Transient
    override val availableActions = listOf("Eat", "Sleep", "Wait")
}