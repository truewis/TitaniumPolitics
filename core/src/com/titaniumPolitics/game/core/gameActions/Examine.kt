package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class Examine(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    var what = ""
    override fun chooseParams() {
        what = GameEngine.acquire(arrayListOf("HR", "apparatus", "resources"))
    }

    override fun execute() {
        when (what) {
            "HR" -> {
                //Acquire HR information
                Logger.write(
                    "HR: ${parent.places[tgtPlace]!!.currentWorker}/${parent.places[tgtPlace]!!.plannedWorker}, ${parent.places[tgtPlace]!!.workHoursStart}-${parent.places[tgtPlace]!!.workHoursEnd}, ${parent.places[tgtPlace]!!.responsibleDivision}",
                    Logger.LogLevel.INFO
                )

                //Acquire apparatus information.
                with(parent.places[tgtPlace]!!) {
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.HUMAN_RESOURCES,
                        tgtTime = parent.time,
                        tgtPlace = tgtPlace,
                        amount = currentWorker
                    ).also {
                        it.knownTo.add(sbjCharacter);parent.addInformation(it)
                    }

                }
            }

            "apparatus" -> {
                Logger.write("Apparatus: ${parent.places[tgtPlace]!!.apparatuses}", Logger.LogLevel.INFO)

                //Acquire apparatus information.
                parent.places[tgtPlace]!!.apparatuses.forEach { entry ->
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.APPARATUS_DURABILITY,
                        tgtTime = parent.time,
                        tgtPlace = tgtPlace,
                        tgtApparatus = entry.name,
                        amount = entry.durability.toInt()
                    ).also {
                        it.knownTo.add(sbjCharacter);parent.addInformation(it)
                    }

                }
            }

            "resources" -> {
                if (tgtPlace.contains("home")) {//Home is the exception; character's resources are shown instead.
                    Logger.write("Resources: ${parent.characters[sbjCharacter]!!.resources}", Logger.LogLevel.INFO)
                    //Acquire resources information of this character.
                    parent.characters[sbjCharacter]!!.resources
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.RESOURCES,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        resources = parent.characters[sbjCharacter]!!.resources
                    ).also {
                        it.knownTo.add(sbjCharacter);parent.addInformation(it)
                    }

                } else {
                    Logger.write("Resources: ${parent.places[tgtPlace]!!.resources}", Logger.LogLevel.INFO)
                    //Acquire resources information of this place.
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.RESOURCES,
                        tgtTime = parent.time,
                        tgtPlace = tgtPlace,
                        resources = parent.places[tgtPlace]!!.resources
                    ).also {
                        it.knownTo.add(sbjCharacter)
                        parent.addInformation(it)
                    }

                }

            }
        }
        super.execute()
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun deltaWill(): Double {
        var w = super.deltaWill()
        if (parent.characters[sbjCharacter]!!.trait.contains("investigator"))
            w += 10
        return w
    }

}