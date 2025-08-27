package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.DTH
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class RestRoutine(var workplace: String? = null) : Routine() {
    init {
        priority = PRIORITY_LIFE_SUPPORT
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {

        if (place != "home_$name" && subroutines.none { it is MoveRoutine })
            return MoveRoutine("home_$name")//Add a move routine with higher priority.
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return pickAction(name, place)
    }

    override fun successCondition(name: String, place: String): Boolean {
        // Wake up based on eta to workplace and workplace work hours.
        if (gState.characters[name]!!.health < ReadOnly.const("CriticalHealth")) return false
        if (gState.characters[name]!!.hunger > ReadOnly.const("hungerThreshold")) return false
        if (gState.characters[name]!!.thirst > ReadOnly.const("thirstThreshold")) return false

        if (workplace == null)
            return (gState.hour in 8..18)
        else {
            return isWorkHourWithETA(
                gState,
                place,
                workplace!!,
                IDTH
            )//Allow waking up 1 hour before commuting to work.
        }
    }

    @Transient
    override val availableActions = listOf("Eat", "Sleep", "Wait")
}