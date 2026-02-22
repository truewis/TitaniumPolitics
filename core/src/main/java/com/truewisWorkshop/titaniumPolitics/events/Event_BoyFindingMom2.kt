package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_BoyFindingMom2 : EventObject(ReadOnly.questProp("BoyFindingMom2-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("Mom")
        ) {
            onPlayDialogue("FindMom2")
            parent.eventSystem.add(Event_BoyFindingMom3())
            deactivate()
        }
    }


}