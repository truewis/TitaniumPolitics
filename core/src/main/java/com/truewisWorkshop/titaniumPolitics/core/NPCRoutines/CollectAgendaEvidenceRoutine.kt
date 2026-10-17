package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.gameActions.Examine
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class CollectAgendaEvidenceRoutine(
    val targetPlace: String,
    val informationType: InformationType
) : Routine() {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (place != targetPlace && subroutines.none { it is MoveRoutine }) {
            return MoveRoutine(targetPlace)
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        if (place == targetPlace) {
            Examine(name, targetPlace, informationType, gState).also {
                if (it.isValid()) {
                    success()
                    return it
                }
            }
        }
        failed()
        return Wait(name, place)
    }
}
