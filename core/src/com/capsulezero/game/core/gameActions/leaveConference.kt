package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class leaveConference(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {

    override fun execute() {
        val meetingName = tgtState.ongoingConferences.filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.first()
        tgtState.ongoingConferences[meetingName]!!.currentCharacters.remove(tgtCharacter)
        if(tgtState.ongoingConferences[meetingName]!!.currentCharacters.count()==0 || tgtState.characters[tgtCharacter]!!.trait.contains("mechanic")) {
            tgtState.ongoingConferences.remove(meetingName)//End the meeting if it has no participants, or if the mechanic leaves

        }
    }

}