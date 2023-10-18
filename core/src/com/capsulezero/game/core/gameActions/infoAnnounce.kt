package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class infoAnnounce(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var who = hashSetOf<String>()
    var what = ""
    override fun chooseParams() {
        //TODO: ability to fabricate information
        what =
            GameEngine.acquire(tgtState.informations.filter { it.value.knownTo.contains(tgtCharacter) }.map { it.key })
        who = tgtState.places[tgtPlace]!!.characters
    }
    override fun execute() {
        tgtState.informations[what]!!.knownTo+=who
        //tgtState.informations[what]!!.publicity = min(tgtState.informations[what]!!.publicity+30, 100)//TODO: Increase publicity in the announced party
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}