package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_KrailinHobby1 : EventObject(ReadOnly.questProp("KrailinHobby1-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "reservoirWest" && parent.player.currentMeeting?.currentCharacters?.containsAll(
                listOf("Krailin", "Rui")
            )
            ?: false
        ) {
            onPlayDialogue("KrailinHobby1")
            parent.eventSystem.add(Event_KrailinHobby2())
            deactivate()
        }
    }

}
