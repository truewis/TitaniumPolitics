package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

//TODO: party integrity affects the chances. Party integrity is affected.
@Deprecated("This class is deprecated. Info requests are done naturally through agendas.")
class InfoRequest(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = hashSetOf<String>()
    var what = ""
    override fun chooseParams()
    {
        //TODO: ability to request information that does not exist
        //Request information that this character only knows the existence.
        what =
            GameEngine.acquire(parent.informations.filter { !it.value.knownTo.contains(sbjCharacter) }.map { it.key })

    }

    override fun execute()
    {
        who = parent.characters[sbjCharacter]!!.currentMeeting!!.currentCharacters
        val party = parent.parties.values.find { it.members.containsAll(who + sbjCharacter) }!!.name
        //TODO: Ability to request information that does not exist
        //If someone knows about the information, then everyone in the meeting/conference knows about it.
        if (parent.informations[what]!!.knownTo.intersect(who).isNotEmpty())
        {
            parent.informations[what]!!.knownTo += who
            //Party integrity increases
            parent.setPartyMutuality(party, party, 1.0)
        } else
            println("$sbjCharacter requested information, but no one knows about $what.")


        parent.characters[sbjCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        return parent.characters[sbjCharacter]!!.currentMeeting != null

    }

}