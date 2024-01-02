package com.capsulezero.game.quests

import com.capsulezero.game.core.Command
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.Quests
import com.capsulezero.game.core.gameActions.resign

class Quest1:Quests.QuestObject("Be a minister", 240) {
    //Mechanic picks a new infrastructure minister. Quest is completed if the player has the most mutuality with the mechanic among all people in the infrastructure party.
    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        //The current infrastructure minister resigns and the mechanic picks a new one.
        val who = parent.parties["infrastructure"]!!.leader
        parent.characters[who]!!.commands.add(Command(parent.parties["infrastructure"]!!.home, resign(tgtCharacter = who, tgtPlace = parent.parties["infrastructure"]!!.home).also { it.injectParent(gameState) }).also { it.executeTime = 48+18; it.issuedParty = "infrastructure" })

    }
    override val isCompleted: Boolean
        get() {
            if(parent.time!=due)return false

            val mechanic = parent.characters.values.find { it.trait.contains("mechanic") }!!
            return parent.parties["infrastructure"]!!.members.maxBy { parent.getMutuality(mechanic.name, it) } == parent.playerAgent

        }
}