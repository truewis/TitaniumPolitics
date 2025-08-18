package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
class StealRoutine(val stealResource: String, val stealFor: String? = null /*If null, steal for myself.*/) : Routine() {
    fun findResource(name: String): Place? {
        return gState.publicPlaces.values.filter {
            it.workplaceParty?.treasurer == null ||
                    it.workplaceParty?.treasurer == name //If the character is the treasurer of the party, they can steal from any place.
        }.maxByOrNull { it.resources[stealResource] }
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {

        val resplace = findResource(name)?.name ?: return null
        if (place != resplace) {
            if (subroutines.none { it is MoveRoutine })
                return MoveRoutine().apply {
                    variables["movePlace"] = resplace
                }//Add a move routine with higher priority.
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        executeDone = true
        val resplace = gState.places[place]!!
        val character = gState.characters[name]!!
        return UnofficialResourceTransfer(name, place).apply {
            resources = Resources(
                stealResource to min(
                    resplace.resources[stealResource] / 2,
                    (character.reliant) * ReadOnly.const("StealAmountMultiplier")
                )
            )
            toWhere = "home_$name"
            Logger.write("$name is stealing $resources from ${resplace.name}!", Logger.LogLevel.INFO)
        }

    }

    override fun endCondition(name: String, place: String): Boolean {
        return executeDone || findResource(name) == null
    }
}