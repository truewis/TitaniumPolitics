package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable

@Serializable
class Event_ExpiredWorkplaceInformation(val place: String) : EventObject(ReadOnly.questProp("ExpiredWorkplaceInformation-name").format(place), true),
    IQuestEventObject {
    val unPreparedRelevantInfos
        get() =
            parent.informations.values.filter { info ->
                parent.time - info.tgtTime < 168 * IDTH //Has to be recent enough
            }
    val relevantInfos
        get() = parent.player.preparedInfoKeys.map {
            parent.informations[it]!!
        }.filter { info ->
            parent.time - info.tgtTime < 168 * IDTH //Has to be recent enough
        }

    override val quest by lazy {
        Quest(
            ReadOnly.questProp("ExpiredWorkplaceInformation-title").format(ReadOnly.placeProp(place)),
            description = ReadOnly.questProp("ExpiredWorkplaceInformation-desc"),
            tgtPlace = place,
            getTooltip = {
                SimpleTextTooltipUI(
                    "You have HR Information: ${unPreparedRelevantInfos.any { it.type == InformationType.HUMAN_RESOURCES && it.tgtPlace == place }}.\n" +
                            "You Prepared It: ${relevantInfos.any { it.type == InformationType.HUMAN_RESOURCES && it.tgtPlace == place }}.\n" +
                            "You have Apparatus Information: ${unPreparedRelevantInfos.any { it.type == InformationType.APPARATUS && it.tgtPlace == place }}.\n" +
                            "You Prepared It: ${relevantInfos.any { it.type == InformationType.APPARATUS && it.tgtPlace == place }}.\n" +
                            "You have Resource Information: ${unPreparedRelevantInfos.any { it.type == InformationType.RESOURCES && it.tgtPlace == place }}.\n" +
                            "You Prepared It: ${relevantInfos.any { it.type == InformationType.RESOURCES && it.tgtPlace == place }}."

                )
            }
        )
    }

    override fun exec(a: Int, b: Int) {

        if (relevantInfos.any { it.type == InformationType.HUMAN_RESOURCES && it.tgtPlace == place }
            && relevantInfos.any { it.type == InformationType.APPARATUS && it.tgtPlace == place }
            && relevantInfos.any { it.type == InformationType.RESOURCES && it.tgtPlace == place })
            deactivate()
    }


}