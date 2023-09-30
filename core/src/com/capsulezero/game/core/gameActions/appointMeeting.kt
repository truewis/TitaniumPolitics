package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.Meeting

class appointMeeting(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var meetingTime =0
    var where = ""
    var who = hashSetOf<String>()
    override fun chooseParams() {

        meetingTime = tgtState.time+GameEngine.acquire(arrayListOf("3","6","9","12","18","21","24")).toInt()
        where = GameEngine.acquire(tgtState.places.map { it.value.name })
        who.add(GameEngine.acquire(tgtState.characters.map { it.value.name }))//TODO: meeting with multiple people
    }

    override fun execute() {
        tgtState.scheduledMeetings["meeting-${where}-${tgtCharacter}-${meetingTime}"] = Meeting(meetingTime, "subjectTBD", who, where)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}