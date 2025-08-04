package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable

@Serializable
class Event_ObserverIntroAfterMeeting1 : EventObject("Mysterious orders from the Observer.", true), IQuestEventObject {
    override val quest = Quest(
        "ObserverIntroAfterMeeting1",
        "Alina's speech",
        "Alina is giving a speech to the Infrastructure Division."
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting == null) {
            onPlayDialogue("ObserverIntroAfterMeeting1")
            parent.eventSystem.add(Event_ObserverIntroAfterMeeting2())
            deactivate()
        }
    }

}