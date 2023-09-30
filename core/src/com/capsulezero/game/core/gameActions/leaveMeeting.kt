package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class leaveMeeting(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {

    override fun execute() {
        val meetingName = tgtState.ongoingMeetings.filter { it.value.currentCharacters.contains(tgtCharacter) }.keys.first()
        tgtState.ongoingMeetings[meetingName]!!.currentCharacters.remove(tgtCharacter)
        if(tgtState.ongoingMeetings[meetingName]!!.currentCharacters.count()<=1) {
            println("Ending meeting $meetingName")
            tgtState.ongoingMeetings.remove(meetingName//End the meeting if it has less than 2 participants
            )
        }
    }

}