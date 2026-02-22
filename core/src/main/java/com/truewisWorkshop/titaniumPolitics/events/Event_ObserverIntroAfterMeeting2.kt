package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_ObserverIntroAfterMeeting2 : EventObject(ReadOnly.questProp("ObserverIntroAfterMeeting2-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "observatory" && parent.player.currentMeeting?.currentCharacters?.contains("observer") == true) {
            onPlayDialogue("ObserverIntroAfterMeeting2")
            deactivate()
        }
    }
}