package com.titaniumPolitics.game.core.gameActions

class Wait(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return true
    }

}