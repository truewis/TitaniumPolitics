package com.titaniumPolitics.game.core.gameActions

class Sleep(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        println("$tgtCharacter slept.")
        if (parent.characters[tgtCharacter]!!.trait.contains("old"))
            parent.characters[tgtCharacter]!!.health += 40
        else
            parent.characters[tgtCharacter]!!.health += 50
        parent.characters[tgtCharacter]!!.frozen += 8
    }

    override fun isValid(): Boolean
    {
        return tgtPlace == "home"
    }

}