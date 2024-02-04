package com.titaniumPolitics.game.core.gameActions

class InvestigateAccidentScene(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        if (parent.places[tgtPlace]!!.isAccidentScene)
            parent.places[tgtPlace]!!.accidentInformations.forEach { entry ->
                entry.value.also {
                    it.author = tgtCharacter;it.knownTo.add(tgtCharacter);it.credibility =
                    100;parent.informations[it.generateName()] = it
                }
            }//Add all accident information to the character.
        parent.characters[tgtCharacter]!!.frozen += 3
    }

    override fun isValid(): Boolean
    {
        return parent.places[tgtPlace]!!.isAccidentScene && parent.parties[parent.places[tgtPlace]!!.responsibleParty]!!.members.contains(
            tgtCharacter
        )
    }

}