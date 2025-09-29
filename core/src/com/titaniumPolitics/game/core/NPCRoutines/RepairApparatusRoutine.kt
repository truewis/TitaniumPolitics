package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class RepairApparatusRoutine(
    val targetApparatusID: String
) : Routine() {
    val source get() = gState.places.filter { it.value.apparatuses.any { app -> app.ID == targetApparatusID } }.keys.first()
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {

        if (place != source) {
            if (subroutines.none { it is MoveRoutine })
                return MoveRoutine(source)
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        if (place == source)//TODO: Must get resources to repair first.
            Repair(name, place, targetApparatusID, gState).also {
                if (it.isValid()) {
                    success()
                    return it
                }
            }
        failed()
        return Wait(name, place)
    }
}