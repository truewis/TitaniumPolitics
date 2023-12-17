package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.Meeting

class appointMeeting(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var meetingTime =0
    var where = ""
    var who = hashSetOf<String>()
    override fun chooseParams() {

        meetingTime = parent.time+GameEngine.acquire(arrayListOf("3","6","9","12","18","21","24")).toInt()
        where = GameEngine.acquire(parent.places.map { it.value.name })
        who.add(GameEngine.acquire(parent.characters.map { it.value.name }))//TODO: meeting with multiple people
    }

    override fun execute() {
        parent.scheduledMeetings["meeting-${where}-${tgtCharacter}-${meetingTime}"] = Meeting(meetingTime, "subjectTBD", who, where)
        parent.characters[tgtCharacter]!!.frozen++
    }

}