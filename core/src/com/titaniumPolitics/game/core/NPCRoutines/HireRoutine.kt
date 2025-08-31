package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.HireDirector
import com.titaniumPolitics.game.core.gameActions.HireManager
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

/**
 * A routine for hiring a character from the market.
 * If placeForDirector is not null, hire a director for the specified place.
 * Otherwise, hire a manager for the specified role in the party.
 * If not in market, move to market first.
 */
@Serializable
class HireRoutine(val party: String, val role: Party.Role?, val placeForDirector: String?) : Routine() {
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        //If not in market, move to market.
        if (place != "market") {
            if (subroutines.none { it is MoveRoutine }) {
                return MoveRoutine("market")//Add a move routine with higher priority.
            }
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        //If in market, hire a character based on role variable.
        if (placeForDirector != null) {
            HireDirector(name, place).also {
                it.injectParent(gState)
                it.workplace = placeForDirector
                it.pickBestEmployee()
                if (it.isValid()) {
                    success()
                    return it
                }
            }
        } else {
            HireManager(name, place).also {
                it.injectParent(gState)
                it.role = role!!
                it.pickBestEmployee()
                if (it.isValid()) {
                    success()
                    return it
                }
            }
        }
        //If no hiring action is valid,
        failed() //In case the role is filled already, wait one turn, then end the routine.
        return Wait(name, place).also {
            it.injectParent(gState)
        }
    }
}