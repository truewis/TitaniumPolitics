package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Resources
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
data class UnofficialResourceTransfer(
    override val sbjCharacter: String, override val tgtPlace: String,
    var toWhere: String,
    var fromHome: Boolean,
    var resources: Resources,
) : GameAction() {
    constructor(
        sbjCharacter: String, tgtPlace: String, toWhere: String,
        fromHome: Boolean,
        resources: Resources, gameState: GameState
    ) : this(sbjCharacter, tgtPlace, toWhere, fromHome, resources) {
        injectParent(gameState)
    }


    override fun execute() {
        if (fromHome) {
            //Transfer resources.
            parent.characters[sbjCharacter]!!.resources -= resources
            parent.places[toWhere]!!.resources += resources

        } else {
            parent.places[tgtPlace]!!.resources -= resources
            parent.places[toWhere]!!.resources += resources
        }
        super.execute()
        // Add logistics overhead: moving more resources takes longer unless the place has enough logistics capacity.
        val logisticsCapacity = if (fromHome) ReadOnly.const("LogisticsBaseCapacityPerWorker")
        else tgtPlaceObj.logisticsCapacity
        val logisticsOverhead = logisticsOverhead(logisticsCapacity)
        if (logisticsOverhead > 0) sbjCharObj.frozen += logisticsOverhead

    }

    private fun logisticsOverhead(logisticsCapacity: Double): Int {
        val totalAmount = resources.keys.sumOf { resources[it] }
        val capacity = max(1.0, logisticsCapacity)
        return (max(0.0, totalAmount - capacity) / capacity * ReadOnly.constInt("UnofficialResourceTransferDuration")).toInt()
    }

    override fun deltaWill(): MutualityMatrix {
        val w = MutualityMatrix()
        //If you have sent someone resources
        if (toWhere.contains("home"))
        //The mutuality from the recipient increases.
            Place.whoseHome(toWhere)
                ?.also {
                    w.addMutuality(
                        it,
                        sbjCharacter,
                        parent.characters[it]!!.itemValue(resources),
                        "UnofficialResourceTransfer"
                    )
                }
        return w
    }

    override fun isValid(): Boolean {
        if (!reason(
                sbjCharacter == tgtPlaceObj.workplaceParty?.treasurer || null == tgtPlaceObj.workplaceParty?.treasurer || fromHome,
                "officialResourceTransfer-notTreasurer"
            )
        ) return false
        //Can't send to the same place
        if (toWhere == tgtPlace) return false
        if (parent.places[toWhere] == null) return false
        /*
         * If transferring from home, must have the resources at home.
         * If transferring from workplace, must have the resources at the workplace.
         */
        return if (fromHome)
            parent.characters[sbjCharacter]!!.resources.contains(
                resources
            )
        else
            parent.places[tgtPlace]!!.resources.contains(
                resources
            )

    }

    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is UnofficialResourceTransfer && (info.action as UnofficialResourceTransfer).let {
            it.toWhere == this.toWhere && it.resources == this.resources //Compare by reference.
        })
    }

}