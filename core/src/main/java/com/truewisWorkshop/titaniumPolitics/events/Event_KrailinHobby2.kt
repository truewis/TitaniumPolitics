package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_KrailinHobby2 : EventObject(ReadOnly.questProp("KrailinHobby2-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "reservoirEast" && parent.player.currentMeeting?.currentCharacters?.containsAll(
                listOf("Krailin", "Rui")
            )
            ?: false
        ) {
            onPlayDialogue("KrailinHobby2")
            deactivate()
        }
    }

}
