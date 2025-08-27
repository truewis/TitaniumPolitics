package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class StealRoutine(
    val stealResource: String,
    val stealAmount: Double,
    val stealFor: String? = null /*If null, steal for myself.*/
) : Routine() {
    fun findResource(name: String): Place? {
        return gState.publicPlaces.values.filter {
            it.workplaceParty?.treasurer == null ||
                    it.workplaceParty?.treasurer == name //If the character is the treasurer of the party, they can steal from any place.
        }.maxByOrNull { it.resources[stealResource] }
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {

        val resplace = findResource(name)?.name
        if (resplace == null) {
            failed()
            return null
        }
        if (place != resplace) {
            if (subroutines.none { it is MoveRoutine })
                return MoveRoutine(resplace)//Add a move routine with higher priority.
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        success()
        val resplace = gState.places[place]!!
        gState.characters[name]!!
        if (resplace.resources[stealResource] < stealAmount) {
            //Not enough resource to steal, the routine ends.
            failed()
            return Wait(name, place)
        }
        UnofficialResourceTransfer(
            name, place,
            stealFor?.let { "home_$it" } ?: "home_$name", false, Resources(
                stealResource to stealAmount
            )
        ).also {
            if (it.isValid()) {
                Logger.write("$name is stealing ${it.resources} from ${resplace.name}!", Logger.LogLevel.INFO)
                return it
            }
        }
        //UnofficialResourceTransfer is invalid.
        failed()
        return Wait(name, place)

    }
}