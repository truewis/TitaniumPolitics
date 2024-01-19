package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import kotlin.math.min

class infoAnnounce(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = hashSetOf<String>()
    var what = ""
    override fun chooseParams()
    {
        //TODO: ability to fabricate information
        what =
                GameEngine.acquire(parent.informations.filter { it.value.knownTo.contains(tgtCharacter) }.map { it.key })
        who = parent.places[tgtPlace]!!.characters
    }

    override fun execute()
    {
        parent.informations[what]!!.knownTo += who
        val party = parent.places[tgtPlace]!!.responsibleParty
        parent.informations[what]!!.publicity[party] = min((parent.informations[what]!!.publicity[party]
                ?: 0) + 30, 100)//TODO: match unit of publicity to number of people in the party
        parent.characters[tgtCharacter]!!.frozen++
    }

}