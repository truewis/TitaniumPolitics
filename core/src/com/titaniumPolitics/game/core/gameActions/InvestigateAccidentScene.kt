package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class InvestigateAccidentScene(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        if (parent.places[tgtPlace]!!.isAccidentScene)
            parent.places[tgtPlace]!!.accidentInformationKeys.forEach { entry ->
                parent.informations[entry]!!.knownTo.add(sbjCharacter)

            }//Add all accident information to the character.
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return parent.places[tgtPlace]!!.isAccidentScene && parent.parties[parent.places[tgtPlace]!!.responsibleDivision]!!.members.contains(
            sbjCharacter
        )
    }

}