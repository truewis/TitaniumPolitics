package com.capsulezero.game.core.gameActions

class leaveMeeting(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {

    override fun execute() {
        val meetingName = parent.ongoingMeetings.filter { it.value.currentCharacters.contains(tgtCharacter) }.keys.first()
        parent.ongoingMeetings[meetingName]!!.currentCharacters.remove(tgtCharacter)
        if(parent.ongoingMeetings[meetingName]!!.currentCharacters.count()<=1) {
            println("Ending meeting $meetingName")
            parent.ongoingMeetings.remove(meetingName//End the meeting if it has less than 2 participants
            )
        }
    }

    override fun isValid(): Boolean {
        return parent.ongoingMeetings.any { it.value.currentCharacters.contains(tgtCharacter) }
    }

}