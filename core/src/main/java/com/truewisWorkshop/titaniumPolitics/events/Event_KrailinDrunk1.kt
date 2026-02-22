package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_KrailinDrunk1 : EventObject(ReadOnly.questProp("KrailinDrunk1-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "tavern" && parent.player.currentMeeting?.currentCharacters?.containsAll(
                listOf("Krailin", "Rui")
            )
            ?: false
        ) {
            onPlayDialogue("KrailinDrunk")
            parent.eventSystem.add(Event_KrailinHobby1())
            deactivate()
        }
    }

}
