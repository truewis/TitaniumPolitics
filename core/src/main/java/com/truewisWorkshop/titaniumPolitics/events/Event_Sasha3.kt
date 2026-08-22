package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Sasha3 : EventObject(ReadOnly.questProp("Sasha3-name"), true), IQuestEventObject {
    private fun pendingRequestCount() =
        parent.requests.values.count { !it.completed && parent.playerName in it.issuedTo }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Sasha3-title"),
        ReadOnly.questProp("Sasha3-desc"),
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Sasha"),
        getTooltip = {
            SimpleTextTooltipUI("Pending petitions: ${pendingRequestCount()}")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Sasha") == true &&
            pendingRequestCount() >= 20 &&
            parent.getMutuality("Sasha", parent.playerName) >= 10.0
        ) {
            onPlayDialogue("Sasha3")
            parent.eventSystem.add(Event_Sasha4())
            deactivate()
        }
    }
}
