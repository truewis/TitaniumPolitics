package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.ClearAccidentScene
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.InvestigateAccidentScene
import kotlinx.serialization.Serializable

@Serializable
class InvestigateAndClearAccidentRoutine() : Routine() {
    var investigated = false
    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        if (place != variables["place"]!!) {
            if (routines.none { it is MoveRoutine })
                return MoveRoutine().also {
                    it.variables["movePlace"] = variables["place"]!!
                }
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        executeDone = true
        if (!investigated) {
            investigated = true
            return InvestigateAccidentScene(name, place)
        }
        return ClearAccidentScene(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        return executeDone || !gState.places[place]!!.isAccidentScene
    }
}