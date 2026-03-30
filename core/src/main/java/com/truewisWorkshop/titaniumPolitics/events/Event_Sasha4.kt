package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Sasha4 : EventObject(ReadOnly.questProp("Sasha4-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Sasha4-title"),
        ReadOnly.questProp("Sasha4-desc"),
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Sasha"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Sasha") == true &&
            parent.getMutuality("Sasha", parent.playerName) >= 30.0
        ) {
            onPlayDialogue("Sasha4")
            parent.progression.add("Management")
            deactivate()
        }
    }
}
