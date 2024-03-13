package com.titaniumPolitics.game.core.gameActions

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
        //Should have at least one dominant agenda. The threshold for dominant agenda is different based on meeting type.
        when (meeting.type)
        {
            "divisionDailyConference" -> return meeting.agendas.maxOf { it.agreement } >= 34
            "cabinetMeeting" -> return meeting.agendas.maxOf { it.agreement } >= 34
            "hearing" -> return meeting.agendas.maxOf { it.agreement } >= 50
            "impeachment" -> return meeting.agendas.maxOf { it.agreement } >= 50
        }
        return false
    }

}