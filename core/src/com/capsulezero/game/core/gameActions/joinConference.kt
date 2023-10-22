package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class joinConference(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var meetingName = ""
    override fun chooseParams() {
        meetingName = GameEngine.acquire(tgtState.ongoingConferences.filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.toList())
    }

    override fun execute() {
        tgtState.ongoingConferences[meetingName]!!.currentCharacters.add(tgtCharacter)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}