package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Astinomis2 : EventObject(ReadOnly.questProp("Astinomis2-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Astinomis2-name"),
        "",
        tgtPlace = "safetyHeadquarters",
        tgtCharacters = listOf("Astinomis"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "safetyHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Astinomis") == true
        ) {
            onPlayDialogue("Astinomis2")
            parent.eventSystem.add(Event_Astinomis3())
            deactivate()
        }
    }
}
