package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.HireDirector
import com.titaniumPolitics.game.core.gameActions.HireManager
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class HireRoutine() : Routine() {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        //If not in market, move to market.
        if (place != "market") {
            if (subroutines.none { it is MoveRoutine }) {
                return MoveRoutine().apply {
                    variables["movePlace"] = "market"
                } //Add a move routine with higher priority.
            }
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        executeDone = true
        //If in market, hire a character based on role variable.
        if (variables["role"]!!.contains("director")) {
            val placeForDirector = variables["role"]!!.split('_')[1]
            HireDirector(name, place).also {
                it.injectParent(gState)
                it.workplace = placeForDirector
                it.pickBestEmployee()
                if (it.isValid())
                    return it
            }
        } else {
            HireManager(name, place).also {
                it.injectParent(gState)
                it.role = variables["role"]!!
                it.pickBestEmployee()
                if (it.isValid())
                    return it
            }
        }
        return Wait(name, place).also {
            it.injectParent(gState)
        } //If no hiring action is valid, wait.
    }

    override fun endCondition(name: String, place: String): Boolean {
        return executeDone //In case the role is filled already, wait one turn, then end the routine.
    }
}