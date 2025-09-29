package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ElectionApproaching(val tgtMeeting: String) : EventObject("The election is approaching.", true),
    IQuestEventObject {

    override val quest
            by lazy {
                Quest(
                    "The election is approaching.",
                    description = "",
                    tgtMeeting = tgtMeeting
                )
            }

    override fun exec(a: Int, b: Int) {
        if (parent.ongoingMeetings[tgtMeeting] != null)
            deactivate()
    }


}