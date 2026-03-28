package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Astinomis3 : EventObject(ReadOnly.questProp("Astinomis3-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Astinomis3-name"),
        "",
        tgtPlace = "safetyHeadquarters",
        tgtCharacters = listOf("Astinomis"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "safetyHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Astinomis") == true
        ) {
            onPlayDialogue("Astinomis3")
            parent.eventSystem.add(Event_Astinomis4())
            deactivate()
        }
    }
}
