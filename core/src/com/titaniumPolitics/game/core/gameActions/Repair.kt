package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class Repair(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var amount = 30
    var apparatusID = ""
    override fun execute()
    {


        parent.places[tgtPlace]!!.getApparatus(apparatusID).also {

            if (it.durability > 70)
            {//TODO: set resource cost.
                if (tgtPlaceObj.resources.contains(it.requiredResourcePerRepair[0]))
                {
                    tgtPlaceObj.resources -= it.requiredResourcePerRepair[0]
                    it.durability = 100.0
                } else
                    println("$sbjCharacter tried to repair $it, but don't have the proper resource.")
            } else if (it.durability <= 70 && it.durability > 30)
            {
                if (tgtPlaceObj.resources.contains(it.requiredResourcePerRepair[1]))
                {
                    tgtPlaceObj.resources -= it.requiredResourcePerRepair[1]
                    it.durability = 70.0
                } else
                    println("$sbjCharacter tried to repair $it, but don't have the proper resource.")
            } else if (it.durability <= 30)
            {
                if (tgtPlaceObj.resources.contains(it.requiredResourcePerRepair[2]))
                {
                    tgtPlaceObj.resources -= it.requiredResourcePerRepair[2]
                    it.durability = 30.0
                } else
                    println("$sbjCharacter tried to repair $it, but don't have the proper resource.")
            }

        }
        super.execute()

    }

    override fun isValid(): Boolean
    {
        return parent.places[tgtPlace]!!.apparatuses.isNotEmpty() && parent.characters[sbjCharacter]!!.trait.contains("technician")
    }

}