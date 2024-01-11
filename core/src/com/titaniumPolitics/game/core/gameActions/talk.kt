package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Meeting

//Talk is considered as a on-the-fly meeting.
class talk(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var who = ""
    override fun chooseParams() {
        who =
            GameEngine.acquire(parent.places[tgtPlace]!!.characters.filter { it!=tgtCharacter }.toList())
        if(parent.characters[who]!!.frozen>1) println("Warning: $who is already busy.")
    }
    override fun execute() {
        parent.ongoingMeetings["meeting-$tgtPlace-$tgtCharacter-${parent.time}"] = Meeting(parent.time, tgtPlace, scheduledCharacters = hashSetOf(who, tgtCharacter), tgtPlace)
        parent.ongoingMeetings["meeting-$tgtPlace-$tgtCharacter-${parent.time}"]!!.currentCharacters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

}