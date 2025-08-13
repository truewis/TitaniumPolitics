package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable

@Serializable
class Event_ObserverIntroAfterMeeting1 : EventObject("Mysterious orders from the Observer.", true), IQuestEventObject {
    override val quest = Quest(
        "Mysterious orders from the Observer",
        "The Observer has told you to pay a visit to the Observatory.",
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