package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Sasha2 : EventObject(ReadOnly.questProp("Sasha2-name"), true), IQuestEventObject {
    private fun pendingRequestCount() =
        parent.requests.values.count { !it.completed && parent.playerName in it.issuedTo }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Sasha2-title"),
        ReadOnly.questProp("Sasha2-desc"),
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Sasha"),
        getTooltip = {
            SimpleTextTooltipUI("Pending petitions: ${pendingRequestCount()}")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Sasha") == true &&
            (parent.player.currentMeeting?.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE ||
                parent.player.currentMeeting?.type == Meeting.MeetingType.TALK) &&
            pendingRequestCount() >= 10
        ) {
            onPlayDialogue("Sasha2")
            parent.eventSystem.add(Event_Sasha3())
            deactivate()
        }
    }
}
