package com.titaniumPolitics.game.core.gameActions

class leaveConference(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val meetingKey = parent.ongoingConferences.filter { it.value.currentCharacters.contains(tgtCharacter) }.keys.first()
        val meeting = parent.ongoingConferences[meetingKey]!!
        meeting.currentCharacters.remove(tgtCharacter)
        if (meeting.currentCharacters.isEmpty() || parent.parties[meeting.involvedParty]!!.leader == tgtCharacter)
        {
            parent.ongoingConferences.remove(meetingKey)//End the meeting if it has no participants, or if the leader leaves

        }
    }

    override fun isValid(): Boolean
    {
        return parent.ongoingConferences.any { it.value.currentCharacters.contains(tgtCharacter) }
    }

}