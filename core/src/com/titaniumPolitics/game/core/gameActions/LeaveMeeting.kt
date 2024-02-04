package com.titaniumPolitics.game.core.gameActions

class LeaveMeeting(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        val meetingName = parent.ongoingMeetings.filter { it.value == meeting }.keys.firstOrNull()
            ?: parent.ongoingConferences.filter { it.value == meeting }.keys.first()
        meeting.currentCharacters.remove(tgtCharacter)
        if (meeting.currentCharacters.count() <= 1)
        {
            println("Ending meeting $meetingName")
            parent.ongoingMeetings.remove(
                meetingName//End the meeting if it has less than 2 participants
            )
        }
    }

    override fun isValid(): Boolean
    {
        return parent.characters[tgtCharacter]!!.currentMeeting != null
    }

}