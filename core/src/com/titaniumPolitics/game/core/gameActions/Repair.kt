package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Apparatus
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Repair(override val sbjCharacter: String, override val tgtPlace: String, var apparatusID: String) :
    GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, apparatusID: String, gameState: GameState) : this(
        sbjCharacter,
        tgtPlace,
        apparatusID
    ) {
        injectParent(gameState)
    }

    override fun execute() {


        parent.places[tgtPlace]!!.getApparatus(apparatusID).also {
            val level = checkRepairLevel(it)
            tgtPlaceObj.resources -= it.requiredResourcePerRepair[level.first]
            it.durability = level.second

        }
        super.execute()

    }

    override fun isValid(): Boolean {
        val app = tgtPlaceObj.getApparatus(apparatusID)
        return sbjCharObj.trait.contains("engineer")
                && reason(
            tgtPlaceObj.resources.contains(app.requiredResourcePerRepair[checkRepairLevel(app).first]),
            "repair-resources"
        )
    }

    override fun deltaWill(): Double {
        return super.deltaWill() * sbjCharObj.stats.lScale
    }

    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is Repair && (info.action as Repair).let {
            it.apparatusID == this.apparatusID
        })
    }

    companion object {
        fun checkRepairLevel(app: Apparatus): Pair<Int, Double> {
            return if (app.durability > 70) {
                Pair(0, 100.0)
            } else if (app.durability <= 70 && app.durability > 30) {
                Pair(1, 70.0)
            } else {
                Pair(2, 30.0)
            }
        }
    }

}