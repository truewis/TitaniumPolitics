package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.Command
import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class command(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var who = ""

    var command:Command = Command("","",0)
    override fun chooseParams() {

        val currentConf = tgtState.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.values.first()
        if(tgtCharacter!=tgtState.parties[currentConf.involvedParty]!!.leader)
            println("Warning: Only the leader of the party can issue commands. $tgtCharacter is not the leader of ${currentConf.involvedParty}")
        who = GameEngine.acquire(currentConf.currentCharacters.toList())
        command = GameEngine.acquire<Command>("Command", hashMapOf("issuedBy" to tgtCharacter, "issuedTo" to who, "party" to currentConf.involvedParty))
    }
    override fun execute() {
        tgtState.nonPlayerAgents[who]!!.commands.add(command)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}