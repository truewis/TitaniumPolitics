package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Maisarah4 : EventObject(ReadOnly.questProp("Maisarah4-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Maisarah4-title"),
        ReadOnly.questProp("Maisarah4-desc"),
        tgtPlace = "miningHeadquarters",
        tgtCharacters = listOf("Maisarah"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "miningHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Maisarah") == true
        ) {
            onPlayDialogue("Maisarah4")
            deactivate()
        }
    }
}
