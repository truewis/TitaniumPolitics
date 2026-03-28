package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Vaeme2 : EventObject(ReadOnly.questProp("Vaeme2-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Vaeme2-name"),
        "",
        tgtPlace = "infrastructureHeadquarters",
        tgtCharacters = listOf("Vaeme"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "infrastructureHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Vaeme") == true
        ) {
            onPlayDialogue("Vaeme2")
            parent.eventSystem.add(Event_Vaeme3())
            deactivate()
        }
    }
}
