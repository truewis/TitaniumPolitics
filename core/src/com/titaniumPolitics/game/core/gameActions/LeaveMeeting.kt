package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class LeaveMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        val meetingName = parent.ongoingMeetings.filter { it.value == meeting }.keys.firstOrNull()
        meeting.currentCharacters.remove(sbjCharacter)

        Logger.write("$sbjCharacter left the meeting $meetingName", Logger.LogLevel.INFO)

        if (meeting.currentCharacters.count() <= 1) {
            Logger.write("Ending meeting $meetingName", Logger.LogLevel.INFO)
            //End meeting if there is only one character left.
            meeting.endMeeting(parent)

        }
        //If you were the speaker, the next random character will be the speaker.
        else if (meeting.currentSpeaker == sbjCharacter) {
            meeting.currentSpeaker = meeting.currentCharacters.random()
            Logger.write("Speaker is now ${meeting.currentSpeaker}", Logger.LogLevel.INFO)
        }
        super.execute()
    }

    override fun isValid(): Boolean {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting
        return meeting != null && reason(
            meeting.type != Meeting.MeetingType.DIVISION_LEADER_ELECTION,
            "leaveMeeting-election"
        )
    }

}