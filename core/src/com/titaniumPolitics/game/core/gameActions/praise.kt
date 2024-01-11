package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

@Deprecated("Use infoShare instead")
class praise(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var who = ""
    override fun chooseParams() {
        who =
            GameEngine.acquire(parent.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }+parent.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters })
    }
    override fun execute() {
        //TODO: praise
        if (tgtCharacter == who){ println("You praise yourself.");return}
        parent.setMutuality(tgtCharacter, who, 2.0)
        parent.characters[tgtCharacter]!!.frozen++
    }

}