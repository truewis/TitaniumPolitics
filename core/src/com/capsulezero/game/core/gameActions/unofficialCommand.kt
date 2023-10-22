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


        who = GameEngine.acquire(tgtState.parties.filter { it.value.leader==tgtCharacter }.values.flatMap { it.members })
        val party = tgtState.parties.values.first { it.leader == tgtCharacter && it.members.contains(who)}.name
        command.place = GameEngine.acquire(tgtState.places.filter { it.value.responsibleParty==party }.keys.toList())
        command.action = GameEngine.acquire(tgtState.parties[party]!!.commands.toList())
        command.amount = 0
    }
    override fun execute() {
        tgtState.nonPlayerAgents[who]!!.command = command
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}