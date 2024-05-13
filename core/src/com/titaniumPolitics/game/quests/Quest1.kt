package com.titaniumPolitics.game.quests

import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Resign
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Quest1 : QuestObject("Be the Infrastructure Division Leader.", 240)
{
    //Mechanic picks a new infrastructure division leader. Quest is completed if the player is elected the new leader.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)


    }

    override fun activate()
    {
        parent.timeChanged += func
        //The current infrastructure minister resigns and the election happens.
        val who = parent.parties["infrastructure"]!!.leader
        val c = Request(
            parent.parties["infrastructure"]!!.home,
            Resign(tgtCharacter = who, tgtPlace = parent.parties["infrastructure"]!!.home).also {
                it.injectParent(parent)
            }, issuedTo = hashSetOf(who)
        ).also { it.generateName() }
        parent.requests[c.name] = c
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