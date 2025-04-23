package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

@Deprecated("This class is deprecated. Only internal division leader can announce.")
class InfoAnnounce(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = hashSetOf<String>()
    var what = ""
    override fun chooseParams()
    {
        //TODO: ability to fabricate information
        what =
            GameEngine.acquire(parent.informations.filter { it.value.knownTo.contains(sbjCharacter) }.map { it.key })
        who = parent.places[tgtPlace]!!.characters
    }

    override fun execute()
    {
        parent.informations[what]!!.knownTo += who
        parent.characters[sbjCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        return listOf("mainControlRoom", "market", "squareNorth", "squareSouth").contains(tgtPlace)
    }

}