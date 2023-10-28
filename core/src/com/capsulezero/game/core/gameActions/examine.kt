package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.Information

class examine(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var what = ""
    override fun chooseParams() {
        what = GameEngine.acquire(arrayListOf("HR", "apparatus", "resources"))
    }
    override fun execute() {
        when(what){
            "HR" -> {
                //Acquire HR information is not planned.
                println("HR: ${tgtState.places[tgtPlace]!!.currentWorker}/${tgtState.places[tgtPlace]!!.plannedWorker}, ${tgtState.places[tgtPlace]!!.workHoursStart}-${tgtState.places[tgtPlace]!!.workHoursEnd}, ${tgtState.places[tgtPlace]!!.responsibleParty}")
            }
            "apparatus" -> {
                println("Apparatus: ${tgtState.places[tgtPlace]!!.apparatuses}")

                //Acquire apparatus information.
                tgtState.places[tgtPlace]!!.apparatuses.forEach { entry ->
                    Information(author = tgtCharacter, creationTime = tgtState.time, type = "apparatusDurability", tgtTime = tgtState.time, tgtPlace = tgtPlace, tgtApparatus = entry.name, amount = entry.durability ).also {it.knownTo.add(tgtCharacter);it.credibility=100;tgtState.informations[it.generateName()] = it }

                }
            }
            "resources" -> {
                if(tgtPlace=="home") {//Home is the exception; character's resources are shown instead.
                    println("Resources: ${tgtState.characters[tgtCharacter]!!.resources}")
                    //Acquire resources information of this character.
                    tgtState.characters[tgtCharacter]!!.resources.forEach { entry ->
                        Information(author = tgtCharacter, creationTime = tgtState.time, type = "resources", tgtTime = tgtState.time, tgtCharacter = tgtCharacter, tgtResource = entry.key, amount = entry.value ).also {it.knownTo.add(tgtCharacter);it.credibility=100;tgtState.informations[it.generateName()] = it }

                    }
                }
                else {
                    println("Resources: ${tgtState.places[tgtPlace]!!.resources}")
                    //Acquire resources information of this place.
                    tgtState.places[tgtPlace]!!.resources.forEach { entry ->
                        Information(author = tgtCharacter, creationTime = tgtState.time, type = "resources", tgtTime = tgtState.time, tgtPlace = tgtPlace, tgtResource = entry.key, amount = entry.value ).also {it.knownTo.add(tgtCharacter);it.credibility=100;tgtState.informations[it.generateName()] = it }

                    }

                }

            }
        }
        tgtState.characters[tgtCharacter]!!.frozen+=2
    }

}