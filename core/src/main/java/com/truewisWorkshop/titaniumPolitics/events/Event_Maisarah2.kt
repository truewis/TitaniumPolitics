package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Maisarah2 : EventObject(ReadOnly.questProp("Maisarah2-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Maisarah2-name"),
        "",
        tgtPlace = "miningHeadquarters",
        tgtCharacters = listOf("Maisarah"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "miningHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Maisarah") == true
        ) {
            onPlayDialogue("Maisarah2")
            parent.eventSystem.add(Event_Maisarah3())
            deactivate()
        }
    }
}
