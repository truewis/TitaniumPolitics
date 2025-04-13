package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import kotlinx.serialization.Serializable

@Serializable
class TransferResourceRoutine() : Routine()
{
    var res = ""
    var transferTo = ""
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        gState.places.values.forEach fe@{ place1 -> //TODO: right now, supply resource to any place regardless of the division. In the future, agents will not supply resources to hostile divisions.
            place1.apparatuses.forEach { apparatus ->
                res = GameEngine.resourceShortOf(apparatus, place1) //Type of resource that is short of.
                if (res != "")
                {
                    transferTo = place1.name
                    //Find a place within my division with maximum res.
                    val resplace =
                        gState.places.values.filter {
                            it.responsibleParty != "" && gState.parties[it.responsibleParty]!!.members.contains(
                                name
                            )
                        }
                            .maxByOrNull { it.resources[res] ?: 0 }
                            ?: return@fe
                    if (place != resplace.name)
                    {
                        return MoveRoutine().apply { variables["movePlace"] = resplace.name }
                    }

                }
            }
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        executeDone = true
        OfficialResourceTransfer(name, place).also {
            it.resources = hashMapOf(res to (gState.places[place]!!.resources[res] ?: 0) / 2)
            it.toWhere = transferTo
            return it
        }
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return executeDone
        //TODO: when pathfinding fails, return true.
    }
}