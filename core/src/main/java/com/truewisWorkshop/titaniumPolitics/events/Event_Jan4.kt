package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Jan4 : EventObject(ReadOnly.questProp("Jan4-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Jan4-title"),
        ReadOnly.questProp("Jan4-desc"),
        tgtPlace = "educationHeadquarters",
        tgtCharacters = listOf("Jan"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "educationHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Jan") == true &&
            parent.getMutuality("Jan", parent.playerName) >= 20.0
        ) {
            onPlayDialogue("Jan4")
            parent.progression.add("Examine")
            deactivate()
        }
    }
}
