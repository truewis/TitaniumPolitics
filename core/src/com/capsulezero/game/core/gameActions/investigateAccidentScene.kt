package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class investigateAccidentScene(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {

    override fun execute() {
        if(tgtState.places[tgtPlace]!!.isAccidentScene)
            tgtState.places[tgtPlace]!!.accidentInformations.forEach { entry -> entry.value.also { it.author = tgtCharacter;it.knownTo.add(tgtCharacter);it.credibility=100;tgtState.informations[it.generateName()] = it }}//Add all accident information to the character.
        tgtState.characters[tgtCharacter]!!.frozen+=3
    }

}