package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_ObserverIntroAfterMeeting2 : EventObject("Mysterious orders from the Observer.", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "spacePort" && parent.player.currentMeeting?.currentCharacters?.contains("observer") == true) {
            onPlayDialogue("ObserverIntroAfterMeeting2")
            deactivate()
        }
    }
}