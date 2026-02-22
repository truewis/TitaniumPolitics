package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ElectionApproaching(val tgtMeeting: String) : EventObject(ReadOnly.questProp("ElectionApproaching-name"), true),
    IQuestEventObject {

    override val quest
            by lazy {
                Quest(
                    ReadOnly.questProp("ElectionApproaching-title"),
                    description = ReadOnly.questProp("ElectionApproaching-desc"),
                    tgtMeeting = tgtMeeting
                )
            }

    override fun exec(a: Int, b: Int) {
        if (parent.ongoingMeetings[tgtMeeting] != null)
            deactivate()
    }


}