package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Astinomis2(val searchFrom: Int) : EventObject(ReadOnly.questProp("Astinomis2-name"), true), IQuestEventObject {
    private fun recentSafetyEvidenceCount() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.count { info ->
            info.type in listOf(InformationType.ACCIDENT, InformationType.CASUALTY) &&
                info.creationTime > searchFrom
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Astinomis2-title"),
        ReadOnly.questProp("Astinomis2-desc"),
        tgtPlace = "safetyHeadquarters",
        tgtCharacters = listOf("Astinomis"),
        getTooltip = {
            SimpleTextTooltipUI("Recent safety evidence: ${recentSafetyEvidenceCount()}/1")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "safetyHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Astinomis") == true &&
            recentSafetyEvidenceCount() >= 1 &&
            parent.getMutuality("Astinomis", parent.playerName) >= 10.0
        ) {
            onPlayDialogue("Astinomis2")
            parent.eventSystem.add(Event_Astinomis3(parent.time))
            deactivate()
        }
    }
}
