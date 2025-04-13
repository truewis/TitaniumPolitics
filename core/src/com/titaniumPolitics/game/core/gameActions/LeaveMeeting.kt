package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class LeaveMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        val meetingName = parent.ongoingMeetings.filter { it.value == meeting }.keys.firstOrNull()
        meeting.currentCharacters.remove(sbjCharacter)

        println("$sbjCharacter left the meeting $meetingName")

        if (meeting.currentCharacters.count() <= 1)
        {
            println("Ending meeting $meetingName")
            //End meeting if there is only one character left.
            meeting.endMeeting(parent)

        } else
        //If you were the speaker, the next random character will be the speaker.
            if (meeting.currentSpeaker == sbjCharacter)
            {
                meeting.currentSpeaker = meeting.currentCharacters.random()
                println("Speaker is now ${meeting.currentSpeaker}")
            }
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return parent.characters[sbjCharacter]!!.currentMeeting != null
    }

}