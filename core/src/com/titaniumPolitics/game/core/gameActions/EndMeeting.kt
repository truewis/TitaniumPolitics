package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class EndMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    val meeting get() = parent.characters[sbjCharacter]!!.currentMeeting
    override fun execute() {

        val meetingName = parent.ongoingMeetings.filter { it.value == meeting }.keys.firstOrNull()

        Logger.write("$sbjCharacter ending meeting $meetingName", Logger.LogLevel.INFO)
        meeting!!.endMeeting(parent)
        //We don't have to remove participants one by one because they don't count once the meeting is not kept tracked in the gameState.
        super.execute()

    }

    override fun isValid(): Boolean {
        return meeting != null &&
                meeting!!.currentCharacters.contains(sbjCharacter) &&
                meeting!!.currentSpeaker == sbjCharacter &&
                reason(meeting!!.type != Meeting.MeetingType.DIVISION_LEADER_ELECTION, "endMeeting-election")
    }

}