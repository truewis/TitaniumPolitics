package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Examine(override val sbjCharacter: String, override val tgtPlace: String, var what: InformationType) :
    GameAction() {
    constructor(
        sbjCharacter: String,
        tgtPlace: String,
        what: InformationType,
        gameState: GameState
    ) : this(sbjCharacter, tgtPlace, what) {
        injectParent(gameState)
    }

    override fun chooseParams() {
        what = when (GameEngine.acquire(arrayListOf("HR", "apparatus", "resources"))) {
            "HR" -> InformationType.HUMAN_RESOURCES
            "apparatus" -> InformationType.APPARATUS
            "resources" -> InformationType.RESOURCES
            else -> throw Exception("")
        }
    }

    override fun execute() {
        when (what) {
            InformationType.HUMAN_RESOURCES -> {
                //Acquire HR information
                Logger.write(
                    "$sbjCharacter examined HR of $tgtPlace: ${parent.places[tgtPlace]!!.currentWorker}/${parent.places[tgtPlace]!!.plannedWorker}, ${parent.places[tgtPlace]!!.workHoursStart}-${parent.places[tgtPlace]!!.workHoursEnd}, ${parent.places[tgtPlace]!!.responsibleDivision}",
                    Logger.LogLevel.ACTION_VERBOSE
                )
                with(parent.places[tgtPlace]!!) {
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.HUMAN_RESOURCES,
                        tgtTime = parent.time,
                        tgtPlace = tgtPlace,
                        amount = currentWorker
                    ).also {
                        it.knownTo.add(sbjCharacter); parent.addInformation(it)
                    }

                }
            }

            InformationType.APPARATUS -> {
                Logger.write(
                    "$sbjCharacter examined Apparatus of $tgtPlace: ${parent.places[tgtPlace]!!.apparatuses}",
                    Logger.LogLevel.ACTION_VERBOSE
                )

                //Acquire apparatus information.
                parent.places[tgtPlace]!!.apparatuses.forEach { entry ->
                    entry.getInformation(sbjCharacter, tgtPlace, parent.time).also {
                        it.knownTo.add(sbjCharacter); parent.addInformation(it)
                    }

                }
            }

            InformationType.RESOURCES -> {
                if (tgtPlace.contains("home")) {//Home is the exception; character's resources are shown instead.
                    Logger.write(
                        "$sbjCharacter examined Resources of $sbjCharacter: ${parent.characters[sbjCharacter]!!.resources}",
                        Logger.LogLevel.ACTION_VERBOSE
                    )
                    //Acquire resources information of this character.
                    parent.characters[sbjCharacter]!!.resources
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.RESOURCES,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        tgtPlace = tgtPlace,
                        resources = parent.characters[sbjCharacter]!!.resources
                    ).also {
                        it.knownTo.add(sbjCharacter); parent.addInformation(it)
                    }

                } else {
                    Logger.write(
                        "$sbjCharacter examined Resources of $tgtPlace: ${parent.places[tgtPlace]!!.resources}",
                        Logger.LogLevel.ACTION_VERBOSE
                    )
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
        if (sbjCharObj.currentMeeting != null) return false //Cannot examine when in a meeting.
        if (tgtPlace.contains("corridor")) return false //Cannot examine corridor.
        tgtPlaceObj.responsibleDivision?.let {
            if (!reason(
                    it == (sbjCharObj.division?.name
                        ?: false),
                    "examine-]"
                )
            ) return false//If there is a responsible division which I am not in, they will prevent me from examining.
        }

        when (what) {
            InformationType.APPARATUS -> if (!reason(
                    "engineer" in sbjCharObj.trait,
                    "examine-engineer"
                )
            ) return false

            InformationType.HUMAN_RESOURCES -> if (!reason(
                    tgtPlaceObj.workplaceParty?.let { sbjCharacter == (it.overseer ?: true) }
                        ?: true, //If there is an overseer, I must be the overseer to examine HR.
                    "examine-HR-notOverseer"
                )
            ) return false

            InformationType.RESOURCES -> if (!reason(
                    tgtPlaceObj.workplaceParty?.let { sbjCharacter == (it.treasurer ?: true) }
                        ?: true, //If there is a treasurer, I must be the treasurer to examine resources.
                    "examine-resources-notTreasurer"
                )
            ) return false

            else -> {}
        }
        return true
    }


    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is Examine && (info.action as Examine).let {
            it.what == this.what && it.tgtPlace == this.tgtPlace
        }) || (info.type == what && info.tgtPlace == this.tgtPlace) /*Do not check time for now, it is quite tricky.*/
    }

    override fun deltaWill(): MutualityMatrix {
        val w = MutualityMatrix()
        val amount = -10.0
        w.addWill(sbjCharacter, amount, "Examine")
        return w
    }

}
