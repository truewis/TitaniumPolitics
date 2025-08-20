package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class StartVoting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    val meeting get() = parent.characters[sbjCharacter]!!.currentMeeting
    override fun execute() {


        meeting!!.startVoting(parent)
        super.execute()

    }

    override fun isValid(): Boolean {
        return meeting != null &&
                meeting!!.currentCharacters.contains(sbjCharacter) &&
                meeting!!.currentSpeaker == sbjCharacter &&
                reason(meeting!!.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION, "startVoting-election") &&
                sbjCharacter == "ctrler" && //Only the controller can finish the nomination in a division leader election meeting.
                meeting!!.nominationFinishedTime != null && meeting!!.nominationFinishedTime!! + 3600.0 / ReadOnly.DT < parent.time && //Voting can only start after the nomination is finished.
                meeting!!.voteResults.isEmpty() //Voting can only start if there are no vote results yet.

    }

}