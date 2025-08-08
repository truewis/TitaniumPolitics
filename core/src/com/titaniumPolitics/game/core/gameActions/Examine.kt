package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class Examine(override val sbjCharacter: String, override val tgtPlace: String, var what: InformationType) :
    GameAction() {
    override fun chooseParams() {
        what = when (GameEngine.acquire(arrayListOf("HR", "apparatus", "resources"))) {
            "HR" -> InformationType.HUMAN_RESOURCES
            "apparatus" -> InformationType.APPARATUS_DURABILITY
            "resources" -> InformationType.RESOURCES
            else -> throw Exception("")
        }
    }

    override fun execute() {
        when (what) {
            InformationType.HUMAN_RESOURCES -> {
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

            InformationType.APPARATUS_DURABILITY -> {
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

            InformationType.RESOURCES -> {
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

            else -> TODO("")
        }
        super.execute()
    }

    override fun isValid(): Boolean {
        tgtPlaceObj.responsibleDivision?.let {
            if (!reason(
                    it == (sbjCharObj.division?.name
                        ?: false),
                    "examine-division"
                )
            ) return false//If there is a responsible division which I am not in, they will prevent me from examining.
        }

        when (what) {
            InformationType.APPARATUS_DURABILITY -> if (!reason(
                    "engineer" in sbjCharObj.trait,
                    "examine-engineer"
                )
            ) return false

            InformationType.HUMAN_RESOURCES -> if (!reason(
                    sbjCharacter == tgtPlaceObj.workplaceParty?.overseer,
                    "examine-HR-notOverseer"
                )
            ) return false

            InformationType.RESOURCES -> if (!reason(
                    sbjCharacter == tgtPlaceObj.workplaceParty?.treasurer,
                    "examine-resources-notTreasurer"
                )
            ) return false

            else -> {}
        }
        return true
    }

    override fun deltaWill(): Double {
        var w = super.deltaWill()
        if (parent.characters[sbjCharacter]!!.trait.contains("investigator"))
            w += 10
        return w * sbjCharObj.stats.lScale
    }

}