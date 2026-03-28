package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_EugeneIntro : EventObject(ReadOnly.questProp("EugeneIntro-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("EugeneIntro-title"),
        ReadOnly.questProp("EugeneIntro-desc"),
        tgtPlace = "educationHeadquarters",
        tgtCharacters = listOf("Eugene"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "educationHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Eugene") == true
        ) {
            onPlayDialogue("EugeneTheReason")
            parent.eventSystem.add(Event_EugeneLesson1())
            deactivate()
        }
    }

}
