package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.Talk
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_DelayRepair1 :
    EventObject(ReadOnly.questProp("DelayRepair1-name"), true), IQuestEventObject {
    var targetApparatusID: String? = null
    var requestIssued = false

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("DelayRepair1-title"),
        ReadOnly.questProp("DelayRepair1-desc"),
        tgtPlace = "squareSouth",
        tgtCharacters = listOf("Agros"),
    )

    override fun exec(a: Int, b: Int) {
        if (targetApparatusID == null)
            parent.places.forEach { pl ->
                pl.value.apparatuses.firstOrNull {
                    it.name == "ammoniaProduction" && it.durability < 70
                }?.let { apparatus -> targetApparatusID = apparatus.ID }
            }
        if (targetApparatusID != null && parent.playerName == "Rui" && !requestIssued && "Rui" in parent.parties["infrastructure"]!!.members) {
            Request(
                action = Talk("Agros", "squareSouth", "Rui"),
                issuedTo = hashSetOf("Agros"),
            ).apply {
                parent.requests[generateName()] = this
            }
            requestIssued = true
        }
        if (parent.player.currentMeeting?.let { "Agros" in it.currentCharacters && it.type == Meeting.MeetingType.TALK }
                ?: false
        ) {
            onPlayDialogue("DelayRepair1")
            targetApparatusID?.let {
                parent.eventSystem.add(Event_DelayRepair2(it, parent.time))
            }
            deactivate()
        }
    }

    override fun displayEmoji(who: String): SpeechUI.EmojiType {
        return if (who == "Agros") SpeechUI.EmojiType.TALK else SpeechUI.EmojiType.NONE
    }


}