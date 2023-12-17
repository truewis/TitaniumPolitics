package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameEngine

@Deprecated("Use infoShare instead")
class criticize(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var who = ""
    override fun chooseParams() {
        who =
            GameEngine.acquire(parent.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }+parent.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters })
    }
    override fun execute() {
        //TODO: chat
        if (tgtCharacter == who) {println("You criticize yourself.");return}
        parent.setMutuality(tgtCharacter, who, -5.0)
        parent.characters[tgtCharacter]!!.frozen++
    }

}