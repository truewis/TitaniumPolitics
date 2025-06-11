package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor2 : EventObject("Talking with Dr Paik.", true), IQuestEventObject {
    override val quest = Quest(
        "Talking with Dr Paik.",
        description = "Talk to Dr Paik in the Welfare Station East.",
        tgtCharacter = "DrPaik",
        tgtPlace = "WelfareStationEast",
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("DrPaik") && parent.player.place.name == "WelfareStationEast"
        ) {
            onPlayDialogue("BribeDoctor2")
            parent.eventSystem.add(Event_BribeDoctor3(searchFrom = parent.time))
            deactivate()
        }
    }

    override fun displayEmoji(who: String): Boolean {
        return who == "DrPaik"
    }


}