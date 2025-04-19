package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Request

@Deprecated("This class is deprecated. Use meeting agendas to request actions instead.")
class unofficialCommand(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
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

        parent.requests[request!!.name] = request!!
        parent.characters[sbjCharacter]!!.frozen++
    }


}