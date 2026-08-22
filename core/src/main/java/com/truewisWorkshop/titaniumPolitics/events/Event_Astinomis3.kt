package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Astinomis3(val searchFrom: Int) : EventObject(ReadOnly.questProp("Astinomis3-name"), true), IQuestEventObject {
    private fun recentSafetyEvidenceCount() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.count { info ->
            info.type in listOf(InformationType.ACCIDENT, InformationType.CASUALTY) &&
                info.creationTime > searchFrom &&
                info.tgtPlace != "safetyHeadquarters"
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Astinomis3-title"),
        ReadOnly.questProp("Astinomis3-desc"),
        tgtPlace = "safetyHeadquarters",
        tgtCharacters = listOf("Astinomis"),
        getTooltip = {
            SimpleTextTooltipUI("Recent safety evidence: ${recentSafetyEvidenceCount()}/2")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "safetyHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Astinomis") == true &&
            recentSafetyEvidenceCount() >= 2 &&
            parent.getMutuality("Astinomis", parent.playerName) >= 25.0
        ) {
            onPlayDialogue("Astinomis3")
            parent.eventSystem.add(Event_Astinomis4())
            deactivate()
        }
    }
}
