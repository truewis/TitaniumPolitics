package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ExpiredWorkplaceInformation(val place: String) : EventObject("Expired Workplace Information: $place", true),
    IQuestEventObject {
    val relevantInfos
        get() = parent.player.preparedInfoKeys.map {
            parent.informations[it]!!
        }.filter { info ->
            parent.time - info.tgtTime < 168 * IDTH //Has to be recent enough
        }

    override val quest
        get() = Quest(
            "You must be kept updated on situations of %s".format(ReadOnly.placeProp(place)),
            description = "Examine it yourself or request your employees to do so.",
            tgtPlace = place,
            tooltip = SimpleTextTooltipUI(
                "You have HR Information: ${relevantInfos.any { it.type == InformationType.HUMAN_RESOURCES && it.tgtPlace == place }}\n" +
                        "You have Apparatus Information: ${relevantInfos.any { it.type == InformationType.APPARATUS && it.tgtPlace == place }}\n" +
                        "You have Resource Information: ${relevantInfos.any { it.type == InformationType.RESOURCES && it.tgtPlace == place }}"

            )
        )

    override fun exec(a: Int, b: Int) {

        if (relevantInfos.any { it.type == InformationType.HUMAN_RESOURCES && it.tgtPlace == place }
            && relevantInfos.any { it.type == InformationType.APPARATUS && it.tgtPlace == place }
            && relevantInfos.any { it.type == InformationType.RESOURCES && it.tgtPlace == place })
            deactivate()
    }


}