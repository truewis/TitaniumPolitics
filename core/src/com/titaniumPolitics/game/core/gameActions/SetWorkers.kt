package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable

@Serializable
//SetWorkers is performed by the workplace manager. It sets the number of unnamed workers per apparatus.
class SetWorkers(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    var workers = 0
    var apparatusID = ""
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

}