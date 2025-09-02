package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ExpiredWorkplaceInformation(val place: String) : EventObject("Pay Salary", true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        "You must be kept updated on situations of %s".format(ReadOnly.placeProp(place)),
        description = "Examine it yourself or request your employees to do so.",
        tgtPlace = place
    )

    override fun exec(a: Int, b: Int) {
        val relevantInfos = parent.player.preparedInfoKeys.map {
            parent.informations[it]!!
        }.filter { info ->
            parent.time - info.tgtTime < 168 * IDTH //Has to be recent enough
        }
        if (relevantInfos.any { it.type == InformationType.HUMAN_RESOURCES && it.tgtPlace == place }
            && relevantInfos.any { it.type == InformationType.APPARATUS && it.tgtPlace == place }
            && relevantInfos.any { it.type == InformationType.RESOURCES && it.tgtPlace == place })
            deactivate()
    }


}