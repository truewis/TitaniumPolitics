package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameEngine

class joinConference(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var meetingName = ""
    override fun chooseParams() {
        meetingName = GameEngine.acquire(parent.ongoingConferences.filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.toList())
    }

    override fun execute() {
        parent.ongoingConferences[meetingName]!!.currentCharacters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

}