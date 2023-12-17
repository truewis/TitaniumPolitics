package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.Information

class unofficialResourceTransfer(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var amount = 10
    var what = ""
    override fun chooseParams() {
        what = GameEngine.acquire(parent.places[tgtPlace]!!.resources.keys.toList())
    }
    override fun execute() {

        if(
            (parent.places[tgtPlace]!!.resources[what]?:0)>=amount
        ) {
            parent.places[tgtPlace]!!.resources[what] = (parent.places[tgtPlace]!!.resources[what]?:0) - amount
            parent.characters[tgtCharacter]!!.resources[what] =
                (parent.characters[tgtCharacter]!!.resources[what]?:0) + amount
            parent.characters[tgtCharacter]!!.frozen++
            //Spread rumor only when someone sees it
            if (parent.places[tgtPlace]!!.currentWorker!=0) {
                Information(
                    author = "",
                    creationTime = parent.time,
                    type = "action",
                    tgtPlace = tgtPlace,
                    amount = amount,
                    action = "unofficialResourceTransfer"
                )/*spread rumor in the responsible party*/.also {
                    parent.informations[it.generateName()] =
                        it; 
                    it.publicity[parent.places[tgtPlace]!!.responsibleParty] = 1
                }
            }
        }
        else{
            println("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
        }

    }

}