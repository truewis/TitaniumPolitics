package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Vaeme4 : EventObject(ReadOnly.questProp("Vaeme4-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Vaeme4-title"),
        ReadOnly.questProp("Vaeme4-desc"),
        tgtPlace = "infrastructureHeadquarters",
        tgtCharacters = listOf("Vaeme"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "infrastructureHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Vaeme") == true &&
            parent.getMutuality("Vaeme", parent.playerName) >= 50.0
        ) {
            onPlayDialogue("Vaeme4")
            parent.progression.add("OfficialResourceTransfer")
            deactivate()
        }
    }
}
