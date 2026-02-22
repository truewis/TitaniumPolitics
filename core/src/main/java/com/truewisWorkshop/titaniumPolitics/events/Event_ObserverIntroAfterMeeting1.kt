package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ObserverIntroAfterMeeting1 : EventObject(ReadOnly.questProp("ObserverIntroAfterMeeting1-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("ObserverIntroAfterMeeting1-title"),
        ReadOnly.questProp("ObserverIntroAfterMeeting1-desc"),
        "observatory"
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting == null) {
            onPlayDialogue("ObserverIntroAfterMeeting1")
            parent.eventSystem.add(Event_ObserverIntroAfterMeeting2())
            deactivate()
        }
    }

}