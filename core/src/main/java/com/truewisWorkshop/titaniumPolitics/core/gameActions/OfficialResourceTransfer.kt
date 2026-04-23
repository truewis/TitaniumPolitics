package com.titaniumPolitics.game.core.gameActions

import com.badlogic.gdx.math.MathUtils.clamp
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
data class OfficialResourceTransfer(
    override val sbjCharacter: String, override val tgtPlace: String,
    var toWhere: String,
    var resources: Resources
) : GameAction() {
    constructor(
        sbjCharacter: String, tgtPlace: String, toWhere: String,
        resources: Resources, gameState: GameState
    ) : this(sbjCharacter, tgtPlace, toWhere, resources) {
        injectParent(gameState)
    }

    override fun execute() {

        if (
            resources.all { parent.places[tgtPlace]!!.resources[it.key] >= it.value }
        ) {
            //Transfer resources.
            resources.forEach { (key, value) ->
                parent.places[tgtPlace]!!.resources[key] -= value
                parent.places[toWhere]!!.resources[key] += value
            }


        } else {
            Logger.write("Not enough resources: $tgtPlace, $resources", Logger.LogLevel.INFO)
        }
        //The mutuality from the recipient party to my party increases. It depends on how the recipient party leader thinks of it.
        parent.places[toWhere]!!.responsibleDivision?.run {
            sbjCharObj.division?.also {
                val partyLeader = parent.characters[parent.parties[this]!!.leader]
                parent.setPartyMutuality(
                    this,
                    it.name,
                    clamp((partyLeader?.itemValue(resources) ?: .0), 0.0, 10.0),
                    "OfficialResourceTransfer"
                )
            }
        }
        // Move the subject character to the destination.
        moveCharacterTo(tgtPlace, toWhere)
        // Time = max(routeBasedOverhead, travelTime from tgtPlace to toWhere).
        val logisticsOverhead = routeBasedOverhead()
        val travelTime = parent.places[tgtPlace]!!.shortestPathAndTimeTo(toWhere, sbjCharacter)?.second ?: 0
        sbjCharObj.frozen += max(logisticsOverhead, travelTime)

    }

    /**
     * Computes logistics overhead (in minutes) based on the best available transport route.
     * For each resource type, the bottleneck throughput of the optimal route determines how
     * long it takes to physically move that amount of material.
     * Overhead = Σ (amount / bottleneckThroughput) × OfficialResourceTransferDuration
     */
    private fun routeBasedOverhead(): Int {
        val baseDuration = ReadOnly.constInt("OfficialResourceTransferDuration")
        var totalOverhead = 0.0
        resources.forEach { (key, amount) ->
            if (amount <= 0.0) return@forEach
            val route = parent.findOptimalTransportRoute(tgtPlace, toWhere, key)
            val throughput = max(1.0, route?.bottleneckThroughput ?: tgtPlaceObj.logisticsCapacity)
            totalOverhead += (amount / throughput) * baseDuration
        }
        return totalOverhead.toInt()
    }

    override fun isValid(): Boolean {
        if (!reason(
                sbjCharacter == tgtPlaceObj.workplaceParty?.treasurer,
                "officialResourceTransfer-notTreasurer"
            )
        ) return false
        //Can't send to the same place
        if (toWhere == tgtPlace) return false
        if (parent.places[toWhere] == null) return false
        return parent.places[tgtPlace]!!.responsibleDivision == sbjCharObj.division?.name && parent.places[tgtPlace]!!.resources.contains(
            resources
        )
    }

    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is OfficialResourceTransfer && (info.action as OfficialResourceTransfer).let {
            it.toWhere == this.toWhere && (this.resources * 0.7).contains(it.resources)  //Compare by amount (70% or more)
        })
    }

}
