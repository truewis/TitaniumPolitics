package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DowntimeRoutine() : Routine() {
    init {
        priority = PRIORITY_REST
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (condition(name, place)) success()
        val char = gState.characters[name]!!
        if (char.trait.contains("extrovert")) {
            if (place !in Place.publicPlaces)
                if (subroutines.none { it is MoveRoutine })
                    return MoveRoutine(Place.publicPlaces.random())//Add a move routine with higher priority.

        }

        //Otherwise, go home
        if (place != "home_$name")
            if (subroutines.none { it is MoveRoutine })
                return MoveRoutine("home_$name")//Add a move routine with higher priority.
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return pickAction(name, place)
    }

    private fun condition(name: String, place: String): Boolean {
        //Pay attention to the condition checking order.
        if (gState.getMutuality(name) < const("DowntimeWill")) return false
        if (variables["workplace"] == null)
            return false //Jobless = downtime forever.
        else
            return isWorkHourWithETA(gState, place, variables["workplace"]!!)
    }

    @Transient
    override val availableActions = listOf("Eat", "Sleep", "Wait")
}