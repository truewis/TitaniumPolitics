package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_DelayRepair2(
    val targetApparatusID: String,
    val searchFrom: Int,
) : EventObject(ReadOnly.questProp("DelayRepair2-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("DelayRepair2-title"),
        ReadOnly.questProp("DelayRepair2-desc"),
        tgtPlace = "welfareStationEast",
    )

    private fun getHospitalDamageInfo() =
        parent.informations.values.firstOrNull { info ->
            info.creationTime > searchFrom &&
                info.type == InformationType.APPARATUS &&
                info.author == parent.playerName &&
                info.tgtPlace == "welfareStationEast" &&
                info.tgtApparatusName == "waterStorage" &&
                info.amount <= 30
        }

    override fun exec(a: Int, b: Int) {
        getHospitalDamageInfo()?.let { info ->
            parent.eventSystem.add(
                Event_DelayRepair3(
                    targetApparatusID = targetApparatusID,
                    hospitalApparatusID = info.tgtApparatusID ?: ""
                )
            )
            deactivate()
        }
    }

    override fun displayEmoji(who: String): SpeechUI.EmojiType {
        return if (who == "DrPaik") SpeechUI.EmojiType.TALK else SpeechUI.EmojiType.NONE
    }
}
