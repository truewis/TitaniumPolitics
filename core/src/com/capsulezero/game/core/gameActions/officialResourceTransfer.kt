package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class officialResourceTransfer(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var toWhere = ""
    var what = ""
    var amount = 10
    override fun chooseParams() {
        toWhere = GameEngine.acquire(tgtState.places.keys.toList())
        what = GameEngine.acquire(tgtState.places[tgtPlace]!!.resources.keys.toList())
    }
    override fun execute() {

        if(
            (tgtState.places[tgtPlace]!!.resources[what]?:0)>=amount
        ) {
            tgtState.places[tgtPlace]!!.resources[what] = (tgtState.places[tgtPlace]!!.resources[what]?:0) - amount
            tgtState.places[toWhere]!!.resources[what] =
                (tgtState.places[toWhere]!!.resources[what]?:0) + amount
            tgtState.characters[tgtCharacter]!!.frozen++

        }
        else{
            println("Not enough resources: $tgtPlace, $what, ${tgtState.places[tgtPlace]!!.resources[what]}")
        }

    }

}