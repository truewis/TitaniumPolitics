package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameState
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
    val triedPlaces = arrayListOf<String>()
    var currentTargetPlace: String = ""
    fun findResource(name: String, currentPlace: String): Place? {
        return gState.publicPlaces.values.filter {
            !triedPlaces.contains(it.name) && (
                it.workplaceParty?.treasurer == null ||
                    it.workplaceParty?.treasurer == name) //If the character is the treasurer of the party, they can steal from any place.
        }.maxByOrNull {
            it.resources[stealResource] / ((it.distanceTo(currentPlace) ?: (Int.MAX_VALUE / 2)) + 1)
            // Prefer places with more resources and closer distance. Add 1 to avoid division by zero.

        }?.also {
            triedPlaces.add(it.name)
        }
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (triedPlaces.size > 5) {
            // Tried too many times.
            return failed()
        }

        if (place != currentTargetPlace) {
            if (subroutines.none { it is MoveRoutine }) {
                currentTargetPlace = findResource(name, place)?.name ?: return failed()
                return MoveRoutine(currentTargetPlace)//Add a move routine with higher priority.
            }
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val placeObj = gState.places[place]!!
        if (placeObj.resources[stealResource] < stealAmount) {
            //Not enough resource to steal.
            currentTargetPlace = findResource(name, place)?.name ?: run {
                //No place to steal from.
                failed()
                return Wait(name, place)
            }
            //Wait and move on to next place.
            return Wait(name, place)
        }
        UnofficialResourceTransfer(
            name, place,
            stealFor?.let { "home_$it" } ?: "home_$name", false, Resources(
                stealResource to stealAmount
            ),
            gState
        ).also {
            if (it.isValid()) {
                success()
                Logger.write("$name is stealing ${it.resources} from ${placeObj.name}!", Logger.LogLevel.INFO)
                return it
            }
        }
        //UnofficialResourceTransfer is invalid.
        currentTargetPlace = findResource(name, place)?.name ?: run {
            //No place to steal from.
            failed()
            return Wait(name, place)
        }
        //Wait and move on to next place.
        return Wait(name, place)

    }
}
