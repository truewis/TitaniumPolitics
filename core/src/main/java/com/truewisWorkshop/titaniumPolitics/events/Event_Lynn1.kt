package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Lynn1 : EventObject(ReadOnly.questProp("Lynn1-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Lynn1-name"),
        "",
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Lynn"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Lynn") == true
        ) {
            onPlayDialogue("Lynn1")
            parent.eventSystem.add(Event_Lynn2())
            deactivate()
        }
    }
}
