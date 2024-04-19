package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.GameEngine

@Deprecated("This class is deprecated. Use meeting agendas to request actions instead.")
class unofficialCommand(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = ""
    var request: Request? = null
    override fun chooseParams()
    {


        val currentConf =
            parent.ongoingConferences.filter { it.value.currentCharacters.contains(tgtCharacter) }.values.first()
        if (tgtCharacter != parent.parties[currentConf.involvedParty]!!.leader)
            println("Warning: Only the leader of the party can issue commands. $tgtCharacter is not the leader of ${currentConf.involvedParty}")
        who = GameEngine.acquire(currentConf.currentCharacters.toList())
        request = GameEngine.acquire<Request>(
            "Command",
            hashMapOf("issuedBy" to tgtCharacter, "issuedTo" to who, "party" to currentConf.involvedParty)
        )
    }

    override fun execute()
    {

        parent.commands[request!!.name] = request!!
        parent.characters[tgtCharacter]!!.frozen++
    }


}