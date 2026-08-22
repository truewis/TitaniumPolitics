package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Astinomis1 : EventObject(ReadOnly.questProp("Astinomis1-name"), true), IQuestEventObject {
    private fun recentSafetyEvidenceCount() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.count { info ->
            info.type in listOf(InformationType.ACCIDENT, InformationType.CASUALTY) &&
                info.creationTime > parent.time - 168 * ReadOnly.IDTH
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Astinomis1-title"),
        ReadOnly.questProp("Astinomis1-desc"),
        tgtPlace = "safetyHeadquarters",
        tgtCharacters = listOf("Astinomis"),
        getTooltip = {
            SimpleTextTooltipUI("Recent safety evidence: ${recentSafetyEvidenceCount()}/1")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "safetyHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Astinomis") == true &&
            parent.player.currentMeeting?.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE &&
            recentSafetyEvidenceCount() >= 1
        ) {
            onPlayDialogue("Astinomis1")
            parent.eventSystem.add(Event_Astinomis2(parent.time))
            deactivate()
        }
    }
}
