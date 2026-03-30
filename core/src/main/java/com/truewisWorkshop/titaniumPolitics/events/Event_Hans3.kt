package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Hans3 : EventObject(ReadOnly.questProp("Hans3-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Hans3-name"),
        "",
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Hans"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Hans") == true
        ) {
            onPlayDialogue("Hans3")
            parent.eventSystem.add(Event_Hans4())
            deactivate()
        }
    }
}
