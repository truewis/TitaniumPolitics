package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
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

}