package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Astinomis4 : EventObject(ReadOnly.questProp("Astinomis4-name"), true), IQuestEventObject {
    private fun recentSafetyEvidenceCount() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.count { info ->
            info.type in listOf(InformationType.ACCIDENT, InformationType.CASUALTY) &&
                info.creationTime > parent.time - 168 * ReadOnly.IDTH
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Astinomis4-title"),
        ReadOnly.questProp("Astinomis4-desc"),
        tgtPlace = "safetyHeadquarters",
        tgtCharacters = listOf("Astinomis"),
        getTooltip = {
            SimpleTextTooltipUI("Recent safety evidence: ${recentSafetyEvidenceCount()}/2")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "safetyHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Astinomis") == true &&
            parent.getMutuality("Astinomis", parent.playerName) >= 50.0 &&
            recentSafetyEvidenceCount() >= 2
        ) {
            onPlayDialogue("Astinomis4")
            parent.progression.add("InvestigateAccident")
            deactivate()
        }
    }
}
