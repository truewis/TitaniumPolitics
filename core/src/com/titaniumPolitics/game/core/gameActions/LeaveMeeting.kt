package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class LeaveMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {

    override fun execute() {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        val meetingName = parent.ongoingMeetings.filter { it.value == meeting }.keys.firstOrNull()
        meeting.currentCharacters.remove(sbjCharacter)

        Logger.write("$sbjCharacter left the meeting $meetingName", Logger.LogLevel.INFO)

        if (meeting.currentCharacters.count() <= 1) {
            Logger.write("Ending meeting $meetingName", Logger.LogLevel.INFO)
            //End meeting if there is only one character left.
            meeting.endMeeting(parent)

        } else
        //If you were the speaker, the next random character will be the speaker.
            if (meeting.currentSpeaker == sbjCharacter) {
                meeting.currentSpeaker = meeting.currentCharacters.random()
                Logger.write("Speaker is now ${meeting.currentSpeaker}", Logger.LogLevel.INFO)
            }
        super.execute()
    }

    override fun isValid(): Boolean {
        //TODO: Should not be able to leave meeting freely if there is a voting at the end.
        return parent.characters[sbjCharacter]!!.currentMeeting != null
    }

}