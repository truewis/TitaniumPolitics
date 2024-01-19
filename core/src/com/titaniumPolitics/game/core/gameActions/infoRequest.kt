package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

//TODO: party integrity affects the chances. Party integrity is affected.
class infoRequest(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = hashSetOf<String>()
    var what = ""
    override fun chooseParams()
    {
        //TODO: ability to request information that does not exist
        //Request information that this character only knows the existence.
        what =
                GameEngine.acquire(parent.informations.filter { !it.value.knownTo.contains(tgtCharacter) }.map { it.key })

    }

    override fun execute()
    {
        who =
                (parent.ongoingMeetings.filter { it.value.currentCharacters.contains(tgtCharacter) }.flatMap { it.value.currentCharacters } + parent.ongoingConferences.filter { it.value.currentCharacters.contains(tgtCharacter) }.flatMap { it.value.currentCharacters }).toHashSet()
        val party = parent.parties.values.find { it.members.containsAll(who + tgtCharacter) }!!.name

        //If someone knows about the information, then everyone in the meeting/conference knows about it.
        if (parent.informations[what]!!.knownTo.intersect(who).isNotEmpty())
        {
            parent.informations[what]!!.knownTo += who
            //Party integrity increases
            parent.setPartyMutuality(party, party, 1.0)
        } else
            println("$tgtCharacter requested information, but no one knows about $what.")


        parent.characters[tgtCharacter]!!.frozen++
    }

}