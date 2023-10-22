package com.capsulezero.game.quests

import com.capsulezero.game.core.Quests

class Quest1:Quests.QuestObject("Quest1", 240) {
    //Mechanic picks a new infrastructure minister. Quest is completed if the player has the most mutuality with the mechanic among all people in the infrastructure party.
    override val isCompleted: Boolean
        get() {
            if(parent.time!=due)return false

            val mechanic = parent.characters.values.find { it.trait.contains("mechanic") }!!
            return parent.parties["infrastructure"]!!.members.maxBy { parent.getMutuality(mechanic.name, it) } == parent.playerAgent

        }
}