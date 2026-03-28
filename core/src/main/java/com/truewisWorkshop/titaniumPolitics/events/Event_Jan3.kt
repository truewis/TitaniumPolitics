package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Jan3 : EventObject(ReadOnly.questProp("Jan3-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Jan3-name"),
        "",
        tgtPlace = "educationHeadquarters",
        tgtCharacters = listOf("Jan"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "educationHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Jan") == true
        ) {
            onPlayDialogue("Jan3")
            parent.eventSystem.add(Event_Jan4())
            deactivate()
        }
    }
}
