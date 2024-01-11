package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

class startConference(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var meetingName = ""
    override fun chooseParams() {
        meetingName = GameEngine.acquire(parent.scheduledConferences.filter { it.value.time+2 > parent.time && parent.time+2>it.value.time }.filter { !parent.ongoingMeetings.containsKey(it.key) }.filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.toList())
    }

    override fun execute() {
        parent.ongoingConferences[meetingName] = parent.scheduledConferences[meetingName]!!
        parent.scheduledConferences.remove(meetingName)
        parent.ongoingConferences[meetingName]!!.currentCharacters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

}