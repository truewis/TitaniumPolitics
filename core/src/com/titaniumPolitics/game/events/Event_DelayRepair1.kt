package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.Resign
import com.titaniumPolitics.game.core.gameActions.Talk
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable

@Serializable
class Event_DelayRepair1 :
    EventObject("Agros comes by and asks to delay repairing the ammonia production system.", true) {
    var targetApparatusID: String? = null
    var requestIssued = false
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
            deactivate()
        }
    }

    override fun displayEmoji(who: String): SpeechUI.EmojiType {
        return if (who == "Agros") SpeechUI.EmojiType.TALK else SpeechUI.EmojiType.NONE
    }


}