package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class joinMeeting(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var meetingName = ""
    override fun chooseParams() {
        meetingName = GameEngine.acquire(tgtState.ongoingMeetings.filter { it.value.characters.contains(tgtCharacter) && it.value.place==tgtPlace}.keys.toList())
    }

    override fun execute() {
        tgtState.ongoingMeetings[meetingName]!!.currentCharacters.add(tgtCharacter)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}