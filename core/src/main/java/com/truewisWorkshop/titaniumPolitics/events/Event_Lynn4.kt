package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Lynn4 : EventObject(ReadOnly.questProp("Lynn4-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Lynn4-title"),
        ReadOnly.questProp("Lynn4-desc"),
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Lynn"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Lynn") == true
        ) {
            onPlayDialogue("Lynn4")
            deactivate()
        }
    }
}
