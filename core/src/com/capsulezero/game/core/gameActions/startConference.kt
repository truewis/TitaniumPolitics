package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class startConference(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var meetingName = ""
    override fun chooseParams() {
        meetingName = GameEngine.acquire(tgtState.scheduledConferences.filter { it.value.time+2 > tgtState.time && tgtState.time+2>it.value.time }.filter { !tgtState.ongoingMeetings.containsKey(it.key) }.filter { it.value.characters.contains(tgtCharacter) }.keys.toList())
    }

    override fun execute() {
        tgtState.ongoingConferences[meetingName] = tgtState.scheduledConferences[meetingName]!!
        tgtState.scheduledConferences.remove(meetingName)
        tgtState.ongoingConferences[meetingName]!!.currentCharacters.add(tgtCharacter)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}