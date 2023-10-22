package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class repair(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var amount = 30
    override fun execute() {


        tgtState.places[tgtPlace]!!.apparatuses.forEach {

            if(it.durability<100-amount) {//Don't repair if it's too good
                it.durability += amount
                tgtState.characters[tgtCharacter]!!.frozen++
            }
            else
                println("$tgtCharacter tried to repair $it, but it's already too good.")
        }


    }

}