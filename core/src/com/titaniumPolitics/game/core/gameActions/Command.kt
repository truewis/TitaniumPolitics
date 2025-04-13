package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.GameEngine

@Deprecated("This class is deprecated. Use Use request as meeting agenda instead.")
class Command(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = ""

    var request: Request? = null
    override fun chooseParams()
    {

        val currentConf =
            parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }.values.first()
        if (sbjCharacter != parent.parties[currentConf.involvedParty]!!.leader)
            println("Warning: Only the leader of the party can issue commands. $sbjCharacter is not the leader of ${currentConf.involvedParty}")
        who = GameEngine.acquire(currentConf.currentCharacters.toList())
        request = GameEngine.acquire<Request>(
            "Command",
            hashMapOf("issuedBy" to sbjCharacter, "issuedTo" to who, "party" to currentConf.involvedParty)
        )
    }

    override fun execute()
    {
        //parent.characters[who]!!.commands.add(command!!)
        parent.characters[sbjCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        if (!parent.ongoingMeetings.any { it.value.currentCharacters.contains(sbjCharacter) }) return false
        val currentConf =
            parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }.values.first()
        if (sbjCharacter != parent.parties[currentConf.involvedParty]!!.leader) return false
        return request != null
    }

}