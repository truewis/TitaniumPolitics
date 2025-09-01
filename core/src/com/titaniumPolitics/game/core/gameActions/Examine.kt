package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Examine(override val sbjCharacter: String, override val tgtPlace: String, var what: InformationType) :
    GameAction() {
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
                    "HR: ${parent.places[tgtPlace]!!.currentWorker}/${parent.places[tgtPlace]!!.plannedWorker}, ${parent.places[tgtPlace]!!.workHoursStart}-${parent.places[tgtPlace]!!.workHoursEnd}, ${parent.places[tgtPlace]!!.responsibleDivision}",
                    Logger.LogLevel.INFO
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
                Logger.write("Apparatus: ${parent.places[tgtPlace]!!.apparatuses}", Logger.LogLevel.INFO)

                //Acquire apparatus information.
                parent.places[tgtPlace]!!.apparatuses.forEach { entry ->
                    entry.getInformation(sbjCharacter, tgtPlace, parent.time).also {
                        it.knownTo.add(sbjCharacter); parent.addInformation(it)
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
                        tgtPlace = tgtPlace,
                        resources = parent.characters[sbjCharacter]!!.resources
                    ).also {
                        it.knownTo.add(sbjCharacter); parent.addInformation(it)
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
        if (sbjCharObj.currentMeeting != null) return false //Cannot examine when in a meeting.
        tgtPlaceObj.responsibleDivision?.let {
            if (!reason(
                    it == (sbjCharObj.division?.name
                        ?: false),
                    "examine-division"
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

    override fun deltaWill(): Double {
        var w = super.deltaWill()
        if (parent.characters[sbjCharacter]!!.trait.contains("investigator"))
            w += 10
        return w * sbjCharObj.stats.lScale
    }

    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is Examine && (info.action as Examine).let {
            it.what == this.what && it.tgtPlace == this.tgtPlace
        }) || (info.type == what && info.tgtPlace == this.tgtPlace) /*Do not check time for now, it is quite tricky.*/
    }

}