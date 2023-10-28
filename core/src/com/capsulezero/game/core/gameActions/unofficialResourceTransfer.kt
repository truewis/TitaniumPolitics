package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.Information

class unofficialResourceTransfer(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var amount = 10
    var what = ""
    override fun chooseParams() {
        what = GameEngine.acquire(tgtState.places[tgtPlace]!!.resources.keys.toList())
    }
    override fun execute() {

        if(
            (tgtState.places[tgtPlace]!!.resources[what]?:0)>=amount
        ) {
            tgtState.places[tgtPlace]!!.resources[what] = (tgtState.places[tgtPlace]!!.resources[what]?:0) - amount
            tgtState.characters[tgtCharacter]!!.resources[what] =
                (tgtState.characters[tgtCharacter]!!.resources[what]?:0) + amount
            tgtState.characters[tgtCharacter]!!.frozen++
            //Spread rumor TODO: only when someone sees it
            Information(
                author = "",
                creationTime = tgtState.time,
                type = "action",
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