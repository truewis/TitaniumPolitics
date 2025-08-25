package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import kotlinx.serialization.Serializable

@Serializable
//SetWorkers is performed by the workplace manager. It sets the number of unnamed workers per apparatus.
data class SetWorkers(
    override val sbjCharacter: String, override val tgtPlace: String,
    var workers: Int, var apparatusID: String
) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, workers: Int, apparatusID: String, gameState: GameState) : this(
        sbjCharacter,
        tgtPlace,
        workers,
        apparatusID
    ) {
        injectParent(gameState)
    }

    val agent
        get() = parent.getApparatusPlace(
            apparatusID
        ).workers!!.first()


    override fun chooseParams() {
    }

    override fun execute() {
        parent.getApparatus(apparatusID).plannedWorker = workers
        val current = agent.reliant
        agent.reliant = workers
        parent.idlePop += current - workers

    }

    override fun isValid(): Boolean {
        if (!reason(sbjCharacter == tgtPlaceObj.workplaceParty?.overseer, "setWorkers-notOverseer")) return false
        if (parent.getApparatusPlace(apparatusID).name != tgtPlace) return false
        if (parent.idlePop < workers - agent.reliant) return false

        (parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
            .flatMap { it.value.currentCharacters }).toHashSet()

        return parent.getApparatusPlace(apparatusID).manager == sbjCharacter
    }

    override fun deltaWill(): Double {
        return super.deltaWill() * sbjCharObj.stats.pScale
    }


    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is SetWorkers && (info.action as SetWorkers).let {
            it.workers == this.workers && it.apparatusID == this.apparatusID
        }) || (info.type == InformationType.HUMAN_RESOURCES && info.tgtPlace == this.tgtPlace) /*Do not check time for now, it is quite tricky.*/
    }

}