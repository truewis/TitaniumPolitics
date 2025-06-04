package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class WanderRoutine() : Routine() {
    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        return MoveRoutine().apply {
            variables["movePlace"] =
                Place.publicPlaces//Should not wander into other people's homes.
                    .random()
        }//Add a move routine with higher priority.
    }

    override fun execute(name: String, place: String): GameAction {
        return pickAction(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        return (gState.hour !in 8..18)
    }

    @Transient
    override val availableActions = listOf("Move", "Wait")
}