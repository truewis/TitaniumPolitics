package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Maisarah1 : EventObject(ReadOnly.questProp("Maisarah1-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Maisarah1-name"),
        "",
        tgtPlace = "miningHeadquarters",
        tgtCharacters = listOf("Maisarah"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "miningHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Maisarah") == true
        ) {
            onPlayDialogue("Maisarah1")
            parent.eventSystem.add(Event_Maisarah2())
            deactivate()
        }
    }
}
