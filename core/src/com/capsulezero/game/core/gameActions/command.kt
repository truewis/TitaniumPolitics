package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.Command
import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class command(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var who = ""
    val command:Command = Command("","",0)
    override fun chooseParams() {
        who = GameEngine.acquire(tgtState.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters })
        command.place = GameEngine.acquire(tgtState.places.filter { it.value.responsibleParty==tgtState.characters[who]!!.division }.keys.toList())
        command.action = GameEngine.acquire(listOf("investigateAccidentScene","clearAccidentScene"))
        command.amount = 0
    }
    override fun execute() {
        tgtState.nonPlayerAgents[who]!!.command = command
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}