package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_BoyFindingMom3 : EventObject("A boy with a box.", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.hour in 20..23 || parent.hour in 0..4 && parent.player.currentMeeting == null && parent.player.place.name == "squareNorth"
        ) {
            onPlayDialogue("FindMom3")
            deactivate()
        }
    }


}