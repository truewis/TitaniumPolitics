package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class WanderRoutine() : Routine() {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (gState.hour !in 8..18) success()
        if (subroutines.none { it is MoveRoutine })
            return MoveRoutine(
                Place.publicPlaces/*Should not wander into other people's homes.*/
                    .random()
            )//Add a move routine with higher priority.
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        return pickAction(name, place)
    }

    @Transient
    override val availableActions = listOf("Move", "Wait")
}