package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import kotlinx.serialization.Serializable

@Serializable
data class InvestigateAccidentScene(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {
        if (parent.places[tgtPlace]!!.isAccidentScene)
            parent.places[tgtPlace]!!.accidentInformationKeys.forEach { entry ->
                parent.informations[entry]!!.knownTo.add(sbjCharacter)

            }//Add all accident information to the character.
        super.execute()
    }

    override fun isValid(): Boolean {
        return parent.places[tgtPlace]!!.isAccidentScene && reason(
            parent.parties[parent.places[tgtPlace]!!.responsibleDivision]!!.members.contains(
                sbjCharacter
            ), "investigateAccidentScene-division"
        )
    }

    override fun deltaWill(): Double {
        return super.deltaWill() * sbjCharObj.stats.lScale
    }

    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is InvestigateAccidentScene && (info.action as InvestigateAccidentScene).let {
            it.tgtPlace == this.tgtPlace
        }) || (info.type == InformationType.CASUALTY && info.tgtPlace == this.tgtPlace) /*Do not check time for now, it is quite tricky.*/
                || (info.type == InformationType.CASUALTY && info.tgtPlace == this.tgtPlace)
    }

}