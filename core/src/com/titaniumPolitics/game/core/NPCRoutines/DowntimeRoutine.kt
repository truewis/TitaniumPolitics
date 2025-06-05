package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Talk
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DowntimeRoutine() : Routine() {
    init {
        priority = PRIORITY_REST
    }

    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        val char = gState.characters[name]!!
        if (char.trait.contains("extrovert")) {
            if (place !in Place.publicPlaces)
                if (routines.none { it is MoveRoutine })
                    return MoveRoutine().apply {
                        variables["movePlace"] = Place.publicPlaces.random()
                    }//Add a move routine with higher priority.

        }

        //Otherwise, go home
        if (place != "home_$name")
            if (routines.none { it is MoveRoutine })
                return MoveRoutine().apply {
                    variables["movePlace"] = "home_$name"
                }//Add a move routine with higher priority.
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return pickAction(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        //Pay attention to the condition checking order.
        //return false must be checked first, otherwise the routine will be created again.
        if (gState.getMutuality(name) < const("DowntimeWill")) return false
        if (variables["workPlace"] == null)
            return (gState.hour in 8..18)
        else
            return (gState.hour in gState.places[variables["workPlace"]!!]!!.workHours)
    }

    @Transient
    override val availableActions = listOf("Eat", "Sleep", "Wait")
}