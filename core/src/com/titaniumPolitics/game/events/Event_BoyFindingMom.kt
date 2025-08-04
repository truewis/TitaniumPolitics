package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_BoyFindingMom : EventObject("A boy with a box.", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.hour in 9..12 && parent.player.currentMeeting == null && parent.player.place.name == "squareNorth"
        ) {
            onPlayDialogue("FindMom")
            parent.eventSystem.add(Event_BoyFindingMom2())
            deactivate()
        }
    }


}