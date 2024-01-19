package com.titaniumPolitics.game.core.gameActions

class wait(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        parent.characters[tgtCharacter]!!.frozen++
    }

}