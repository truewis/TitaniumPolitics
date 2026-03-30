package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Vaeme1 : EventObject(ReadOnly.questProp("Vaeme1-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Vaeme1-name"),
        "",
        tgtPlace = "infrastructureHeadquarters",
        tgtCharacters = listOf("Vaeme"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "infrastructureHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Vaeme") == true
        ) {
            onPlayDialogue("Vaeme1")
            parent.eventSystem.add(Event_Vaeme2())
            deactivate()
        }
    }
}
