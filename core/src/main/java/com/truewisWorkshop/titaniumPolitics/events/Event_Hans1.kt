package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Hans1 : EventObject(ReadOnly.questProp("Hans1-name"), true), IQuestEventObject {
    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Hans1-title"),
        ReadOnly.questProp("Hans1-desc"),
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Hans"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.places["outerBarrierEast"]!!.isAccidentScene &&
            parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Hans") == true
        ) {
            onPlayDialogue("Hans1")
            parent.eventSystem.add(Event_Hans2(parent.time))
            deactivate()
        }
    }
}
