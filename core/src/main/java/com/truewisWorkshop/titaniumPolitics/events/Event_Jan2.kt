package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Jan2 : EventObject(ReadOnly.questProp("Jan2-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Jan2-name"),
        "",
        tgtPlace = "educationHeadquarters",
        tgtCharacters = listOf("Jan"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "educationHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Jan") == true
        ) {
            onPlayDialogue("Jan2")
            parent.eventSystem.add(Event_Jan3())
            deactivate()
        }
    }
}
