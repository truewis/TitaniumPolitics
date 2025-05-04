package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
class StealRoutine() : Routine()
{
    fun findResource(name: String): Place?
    {
        return gState.places.values.filter {
            it.responsibleParty != "" && gState.parties[it.responsibleParty]!!.members.contains(
                name
            )
        }.maxByOrNull { it.resources[variables["stealResource"]!!] }
    }

    override fun newRoutineCondition(name: String, place: String): Routine?
    {

        val resplace = findResource(name)?.name ?: return null
        if (place != resplace)
        {
            return MoveRoutine().apply {
                variables["movePlace"] = resplace
            }//Add a move routine with higher priority.
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        executeDone = true
        val resplace = gState.places[place]!!
        val character = gState.characters[name]!!
        return UnofficialResourceTransfer(name, place).apply {
            resources = Resources(
                variables["stealResource"]!! to min(
                    resplace.resources[variables["stealResource"]!!] / 2,
                    (character.reliant) * ReadOnly.const("StealAmountMultiplier")
                )
            )
            toWhere = "home_$name"
            println("$name is stealing $resources from ${resplace.name}!")
        }

    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return executeDone || findResource(name) == null
    }
}