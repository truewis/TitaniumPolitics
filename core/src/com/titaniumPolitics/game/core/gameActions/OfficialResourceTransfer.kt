package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class OfficialResourceTransfer(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    var toWhere = ""
    var resources = Resources()

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
                parent.setPartyMutuality(this, it.name, (partyLeader?.itemValue(resources) ?: .0))
            }
        }
        super.execute()

    }

    override fun isValid(): Boolean {
        //Can't send to the same place
        if (toWhere == tgtPlace) return false
        return parent.places[tgtPlace]!!.responsibleDivision == sbjCharObj.division?.name && parent.places[tgtPlace]!!.resources.contains(
            resources
        )
    }

}