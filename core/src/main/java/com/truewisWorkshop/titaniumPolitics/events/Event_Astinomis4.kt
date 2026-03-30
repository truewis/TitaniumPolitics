package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Astinomis4 : EventObject(ReadOnly.questProp("Astinomis4-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Astinomis4-title"),
        ReadOnly.questProp("Astinomis4-desc"),
        tgtPlace = "safetyHeadquarters",
        tgtCharacters = listOf("Astinomis"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "safetyHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Astinomis") == true &&
            parent.getMutuality("Astinomis", parent.playerName) >= 50.0
        ) {
            onPlayDialogue("Astinomis4")
            parent.progression.add("InvestigateAccident")
            deactivate()
        }
    }
}
