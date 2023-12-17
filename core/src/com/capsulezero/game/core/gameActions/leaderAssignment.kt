package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameEngine

class leaderAssignment(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var targetParty = ""
    var who = ""
    override fun chooseParams() {
        targetParty = GameEngine.acquire(parent.parties.filter { it.value.leader=="" }.keys.toList())
        who = GameEngine.acquire(parent.characters.keys.filter { parent.parties[targetParty]!!.members.contains(it) }.toList())
    }

    override fun isValid(): Boolean = parent.characters[tgtCharacter]!!.trait.contains("mechanic")//Only the mechanic can assign leaders.
    override fun execute() {
        parent.parties[targetParty]!!.leader = who
        parent.characters[tgtCharacter]!!.frozen++

    }

}