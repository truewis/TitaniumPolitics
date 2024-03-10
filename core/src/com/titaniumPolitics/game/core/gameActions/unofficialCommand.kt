package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Command
import com.titaniumPolitics.game.core.GameEngine

@Deprecated("This class is deprecated. Use Trade instead.")
class unofficialCommand(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = ""
    var command: Command? = null
    override fun chooseParams()
    {


        val currentConf =
            parent.ongoingConferences.filter { it.value.currentCharacters.contains(tgtCharacter) }.values.first()
        if (tgtCharacter != parent.parties[currentConf.involvedParty]!!.leader)
            println("Warning: Only the leader of the party can issue commands. $tgtCharacter is not the leader of ${currentConf.involvedParty}")
        who = GameEngine.acquire(currentConf.currentCharacters.toList())
        command = GameEngine.acquire<Command>(
            "Command",
            hashMapOf("issuedBy" to tgtCharacter, "issuedTo" to who, "party" to currentConf.involvedParty)
        )
    }

    override fun execute()
    {
        parent.characters[who]!!.commands.add(command!!)
        parent.characters[tgtCharacter]!!.frozen++
    }

}