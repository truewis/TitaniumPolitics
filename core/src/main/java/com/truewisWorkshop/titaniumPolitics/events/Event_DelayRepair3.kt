package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_DelayRepair3(
    val targetApparatusID: String,
    val hospitalApparatusID: String,
) : EventObject(ReadOnly.questProp("DelayRepair3-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("DelayRepair3-title"),
        ReadOnly.questProp("DelayRepair3-desc"),
        tgtPlace = "welfareStationEast",
        tgtCharacters = listOf("DrPaik"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "welfareStationEast" &&
            parent.player.currentMeeting?.let { "DrPaik" in it.currentCharacters && it.type == Meeting.MeetingType.TALK } == true
        ) {
            onPlayDialogue("DelayRepair3")
            parent.eventSystem.add(
                Event_DelayRepair4(
                    targetApparatusID = targetApparatusID,
                    hospitalApparatusID = hospitalApparatusID,
                    searchFrom = parent.time
                )
            )
            deactivate()
        }
    }

    override fun displayEmoji(who: String): SpeechUI.EmojiType {
        return if (who == "DrPaik") SpeechUI.EmojiType.TALK else SpeechUI.EmojiType.NONE
    }
}
