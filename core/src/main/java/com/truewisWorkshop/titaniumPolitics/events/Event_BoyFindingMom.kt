package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.Talk
import kotlinx.serialization.Serializable

@Serializable
class Event_BoyFindingMom : EventObject(ReadOnly.questProp("BoyFindingMom-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.hour in 9..12 && parent.player.currentMeeting == null && parent.player.place.name == "market" &&
            "Yuri" in parent.player.place.characters
        ) {
            onPlayDialogue("FindMom")
            Request(
                action = Talk("Yuri", "market", "Rui"),
                issuedTo = hashSetOf("Yuri"),
            ).apply {
                parent.requests[generateName()] = this
            }
            parent.eventSystem.add(Event_BoyFindingMom2())
            deactivate()
        }
    }


}