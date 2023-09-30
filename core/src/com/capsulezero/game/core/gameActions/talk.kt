package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.Meeting

//Talk is considered as a on-the-fly meeting.
class talk(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var who = ""
    override fun chooseParams() {
        who =
            GameEngine.acquire(tgtState.places[tgtPlace]!!.characters.toList())
        if(tgtState.characters[who]!!.frozen>1) println("Warning: $who is already busy.")
    }
    override fun execute() {
        tgtState.ongoingMeetings["meeting-$tgtPlace-$tgtCharacter-${tgtState.time}"] = Meeting(tgtState.time, tgtPlace, hashSetOf(who, tgtCharacter), tgtPlace)
        tgtState.ongoingMeetings["meeting-$tgtPlace-$tgtCharacter-${tgtState.time}"]!!.currentCharacters.add(tgtCharacter)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}