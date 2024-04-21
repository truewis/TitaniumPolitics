package com.titaniumPolitics.game.core.gameActions

class LeaveMeeting(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        val meetingName = parent.ongoingMeetings.filter { it.value == meeting }.keys.firstOrNull()
            ?: parent.ongoingConferences.filter { it.value == meeting }.keys.first()
        meeting.currentCharacters.remove(tgtCharacter)

        println("$tgtCharacter left the meeting $meetingName")

        if (meeting.currentCharacters.count() <= 1)
        {
            println("Ending meeting $meetingName")
            //End meeting if there is only one character left.
            meeting.endMeeting(parent)

        } else
        //If you were the speaker, the next random character will be the speaker.
            if (meeting.currentSpeaker == tgtCharacter)
            {
                meeting.currentSpeaker = meeting.currentCharacters.random()
                println("Speaker is now ${meeting.currentSpeaker}")
            }
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return parent.characters[tgtCharacter]!!.currentMeeting != null
    }

}