package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_BoyFindingMom3 : EventObject(ReadOnly.questProp("BoyFindingMom3-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.hour in 20..23 || parent.hour in 0..4 && parent.player.currentMeeting == null && parent.player.place.name == "squareNorth"
        ) {
            onPlayDialogue("FindMom3")
            parent.unlockProgression("Management")
            deactivate()
        }
    }


}