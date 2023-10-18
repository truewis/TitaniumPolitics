package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.Information

class unofficialResourceTransfer(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var amount = 10
    override fun execute() {

        if(
        tgtState.places[tgtPlace]!!.resources["water"]!!>=amount
        ) {
            tgtState.places[tgtPlace]!!.resources["water"] = tgtState.places[tgtPlace]!!.resources["water"]!! - amount
            tgtState.characters[tgtCharacter]!!.resources["water"] =
                tgtState.characters[tgtCharacter]!!.resources["water"]!! + amount
            tgtState.characters[tgtCharacter]!!.frozen++
            //Spread rumor
            Information(
                "",
                tgtState.time,
                "action",
                tgtPlace = tgtPlace,
                amount = amount,
                action = "unofficialResourceTransfer"
            )/*spread rumor in the responsible party*/.also { tgtState.informations[it.generateName()] = it; it.publicity[tgtState.places[tgtPlace]!!.responsibleParty] = 1 }
        }
        else{
            println("Not enough resources: $tgtPlace, ${tgtState.places[tgtPlace]!!.resources["water"]}")
        }

    }

}