package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_YuhoaIntro : EventObject("Introduction of Yuhoa", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "tavern" && parent.player.currentMeeting?.currentCharacters?.containsAll(
                listOf("Yuhoa", "Rui")
            )
            ?: false
        ) {
            onPlayDialogue("YuhoaIntroduction1")
            parent.eventSystem.add(Event_BribeDoctor0())
            deactivate()
        }
    }

}
