package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class leaderAssignment(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(
    targetState, targetCharacter,
    targetPlace
) {
    var targetParty = ""
    var who = ""
    override fun chooseParams() {
        targetParty = GameEngine.acquire(tgtState.parties.filter { it.value.leader=="" }.keys.toList())
        who = GameEngine.acquire(tgtState.characters.keys.filter { tgtState.parties[targetParty]!!.members.contains(it) }.toList())
    }

    override fun isValid(): Boolean = tgtState.characters[tgtCharacter]!!.trait.contains("mechanic")//Only the mechanic can assign leaders.
    override fun execute() {
        tgtState.parties[targetParty]!!.leader = who
        tgtState.characters[tgtCharacter]!!.frozen++

    }

}