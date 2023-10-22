package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class startMeeting(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var meetingName = ""
    override fun chooseParams() {
        meetingName = GameEngine.acquire(tgtState.scheduledMeetings.filter { it.value.time+2 > tgtState.time && tgtState.time+2>it.value.time && it.value.place==tgtPlace}.filter { !tgtState.ongoingMeetings.containsKey(it.key) }.filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.toList())
    }

    override fun execute() {
        tgtState.ongoingMeetings[meetingName] = tgtState.scheduledMeetings[meetingName]!!
        tgtState.scheduledMeetings.remove(meetingName)
        tgtState.ongoingMeetings[meetingName]!!.currentCharacters.add(tgtCharacter)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}