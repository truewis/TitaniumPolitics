package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class EndMeeting(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        val meetingName = parent.ongoingMeetings.filter { it.value == meeting }.keys.firstOrNull()
            ?: parent.ongoingConferences.filter { it.value == meeting }.keys.first()

        println("Ending meeting $meetingName")
        meeting.endMeeting(parent)
        //We don't have to remove participants one by one because they don't count once the meeting is not kept tracked in the gameState.
        super.execute()

    }

    override fun isValid(): Boolean
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        return false
    }

}