package com.titaniumPolitics.game.quests

import com.titaniumPolitics.game.core.Command
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Resign
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Quest1 : QuestObject("Be a minister", 240)
{
    //Mechanic picks a new infrastructure minister. Quest is completed if the player has the most mutuality with the mechanic among all people in the infrastructure party.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //The current infrastructure minister resigns and the mechanic picks a new one.
        val who = parent.parties["infrastructure"]!!.leader
        parent.characters[who]!!.commands.add(
            Command(
                parent.parties["infrastructure"]!!.home,
                Resign(tgtCharacter = who, tgtPlace = parent.parties["infrastructure"]!!.home).also {
                    it.injectParent(gameState)
                }).also { it.executeTime = 48 + 18; it.issuedParty = "infrastructure" })

    }

    override fun activate()
    {
        parent.timeChanged += func
    }

    override fun deactivate()
    {
        parent.timeChanged -= func
    }

    @Transient
    val func: (Int, Int) -> Unit = { i: Int, i1: Int ->

        val mechanic = parent.characters.values.find { it.trait.contains("mechanic") }!!
        if (parent.parties["infrastructure"]!!.members.maxBy {
                parent.getMutuality(
                    mechanic.name,
                    it
                )
            } == parent.playerName)
            complete()

    }
}