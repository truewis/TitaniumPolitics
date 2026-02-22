package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor2 : EventObject(ReadOnly.questProp("BribeDoctor2-name"), true), IQuestEventObject {
    @Transient
    override val quest = Quest(
        ReadOnly.questProp("BribeDoctor2-title"),
        description = ReadOnly.questProp("BribeDoctor2-desc"),
        tgtCharacters = listOf("DrPaik"),
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

    override fun displayEmoji(who: String): SpeechUI.EmojiType {
        return if (who == "DrPaik") SpeechUI.EmojiType.TALK else SpeechUI.EmojiType.NONE
    }


}