package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Hans2 : EventObject(ReadOnly.questProp("Hans2-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Hans2-name"),
        "",
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Hans"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Hans") == true
        ) {
            onPlayDialogue("Hans2")
            parent.eventSystem.add(Event_Hans3())
            deactivate()
        }
    }
}
