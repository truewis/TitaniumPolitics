package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Apparatus
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class Repair(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    var amount = 30
    var apparatusID = ""
    override fun execute() {


        parent.places[tgtPlace]!!.getApparatus(apparatusID).also {
            val level = checkRepairLevel(it)
            tgtPlaceObj.resources -= it.requiredResourcePerRepair[level.first]
            it.durability = level.second

        }
        super.execute()

    }

    fun checkRepairLevel(app: Apparatus): Pair<Int, Double> {
        return if (app.durability > 70) {//TODO: set resource cost.
            Pair(0, 100.0)
        } else if (app.durability <= 70 && app.durability > 30) {
            Pair(1, 70.0)
        } else {
            Pair(2, 30.0)
        }
    }

    override fun isValid(): Boolean {
        val app = parent.places[tgtPlace]!!.getApparatus(apparatusID)
        return parent.characters[sbjCharacter]!!.trait.contains("engineer")
                && reason(
            tgtPlaceObj.resources.contains(app.requiredResourcePerRepair[checkRepairLevel(app).first]),
            "repair-resources"
        )
    }

    override fun deltaWill(): Double {
        return super.deltaWill() * sbjCharObj.stats.lScale
    }

}