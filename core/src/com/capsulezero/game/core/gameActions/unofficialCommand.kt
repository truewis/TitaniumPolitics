package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.Command
import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class unofficialCommand(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var who = ""
    val command:Command = Command("","",0)
    override fun chooseParams() {


        val currentConf = tgtState.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.values.first()
        if(tgtCharacter!=tgtState.parties[currentConf.involvedParty]!!.leader)
            println("Warning: Only the leader of the party can issue commands. $tgtCharacter is not the leader of ${currentConf.involvedParty}")
        who = GameEngine.acquire(currentConf.currentCharacters.toList())
        command.place = GameEngine.acquire(tgtState.places.filter { currentConf.involvedParty!="" && it.value.responsibleParty==currentConf.involvedParty }.keys.toList())
        command.action = GameEngine.acquire(tgtState.parties[currentConf.involvedParty]!!.commands.toList())
        command.amount = 0
    }
    override fun execute() {
        tgtState.nonPlayerAgents[who]!!.commands.add(command)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}