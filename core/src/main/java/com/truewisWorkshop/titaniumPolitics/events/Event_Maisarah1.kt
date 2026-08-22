package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Maisarah1 : EventObject(ReadOnly.questProp("Maisarah1-name"), true), IQuestEventObject {
    private fun recentMiningEvidenceCount() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.count { info ->
            info.type in listOf(InformationType.ACCIDENT, InformationType.CASUALTY) &&
                info.creationTime > parent.time - 168 * ReadOnly.IDTH &&
                (info.tgtPlace == "miningHeadquarters" || info.tgtPlace.startsWith("mine"))
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Maisarah1-title"),
        ReadOnly.questProp("Maisarah1-desc"),
        tgtPlace = "miningHeadquarters",
        tgtCharacters = listOf("Maisarah"),
        getTooltip = {
            SimpleTextTooltipUI("Recent mining evidence: ${recentMiningEvidenceCount()}/1")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "miningHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Maisarah") == true &&
            parent.player.currentMeeting?.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE &&
            recentMiningEvidenceCount() >= 1
        ) {
            onPlayDialogue("Maisarah1")
            parent.eventSystem.add(Event_Maisarah2(parent.time))
            deactivate()
        }
    }
}
