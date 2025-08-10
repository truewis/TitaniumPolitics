package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class FinishNomination(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    val meeting get() = parent.characters[sbjCharacter]!!.currentMeeting
    override fun execute() {


        meeting!!.finishNomination()
        super.execute()

    }

    override fun isValid(): Boolean {
        return meeting != null &&
                meeting!!.currentCharacters.contains(sbjCharacter) &&
                meeting!!.currentSpeaker == sbjCharacter &&
                reason(meeting!!.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION, "endMeeting-election") &&
                sbjCharacter == "ctrler" //Only the controller can finish the nomination in a division leader election meeting.
    }

}