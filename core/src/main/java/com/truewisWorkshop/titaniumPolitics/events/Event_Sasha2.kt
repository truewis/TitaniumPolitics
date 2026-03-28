package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Sasha2 : EventObject(ReadOnly.questProp("Sasha2-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Sasha2-name"),
        "",
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Sasha"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Sasha") == true
        ) {
            onPlayDialogue("Sasha2")
            parent.eventSystem.add(Event_Sasha3())
            deactivate()
        }
    }
}
