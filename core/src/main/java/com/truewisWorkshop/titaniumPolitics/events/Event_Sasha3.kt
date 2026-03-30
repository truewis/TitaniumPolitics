package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Sasha3 : EventObject(ReadOnly.questProp("Sasha3-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Sasha3-name"),
        "",
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Sasha"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Sasha") == true
        ) {
            onPlayDialogue("Sasha3")
            parent.eventSystem.add(Event_Sasha4())
            deactivate()
        }
    }
}
