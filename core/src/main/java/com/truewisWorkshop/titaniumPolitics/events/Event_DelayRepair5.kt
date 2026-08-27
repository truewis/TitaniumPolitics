package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_DelayRepair5(
    val targetApparatusID: String,
) : EventObject(ReadOnly.questProp("DelayRepair5-name"), true), IQuestEventObject {
    var confrontationTime = -1

    @Transient
    override val quest by lazy {
        Quest(
            ReadOnly.questProp("DelayRepair5-title"),
            ReadOnly.questProp("DelayRepair5-desc"),
            tgtPlace = parent.getApparatusPlace(targetApparatusID).name,
            tgtCharacters = listOf("Alina", "Salvor"),
        )
    }

    override fun exec(a: Int, b: Int) {
        if (confrontationTime < 0) {
            if (parent.parties["infrastructure"]!!.leader == "Alina" &&
                parent.player.currentMeeting?.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE &&
                parent.player.currentMeeting?.currentSpeaker == "Alina" &&
                parent.player.currentMeeting?.currentCharacters?.containsAll(listOf("Alina", "Salvor", parent.playerName)) == true
            ) {
                onPlayDialogue("DelayRepair2")
                confrontationTime = parent.time
            }
            return
        }

        if (parent.informations.values.any { info ->
                info.creationTime > confrontationTime &&
                    info.type == InformationType.ACTION &&
                    info.action is Repair &&
                    (info.action as Repair).apparatusID == targetApparatusID
            }
        ) {
            deactivate()
            return
        }

        if (parent.time - confrontationTime > 48 * IDTH) {
            deactivate(false)
        }
    }
}
