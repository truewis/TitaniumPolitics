package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.ClearAccidentScene
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.InvestigateAccidentScene
import kotlinx.serialization.Serializable

@Serializable
class InvestigateAndClearAccidentRoutine(var investigatePlace: String) : Routine() {
    private var investigated = false
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (place != investigatePlace) {
            return MoveRoutine(investigatePlace)
        } else if (!gState.places[place]!!.isAccidentScene) return failed() //I arrived at the scene, but is no longer accident scene.
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        if (!investigated) {
            investigated = true
            return InvestigateAccidentScene(name, place)
        }
        success()
        return ClearAccidentScene(name, place)
    }
}