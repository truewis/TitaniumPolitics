package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_FirstWorkplaceMeeting : EventObject(ReadOnly.questProp("FirstWorkplaceMeeting-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" && parent.player.currentMeeting != null) {
            onPlayDialogue("FirstWorkplaceMeeting")
            deactivate()
        }
    }

}
