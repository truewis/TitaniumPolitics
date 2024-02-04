package com.titaniumPolitics.game.core.gameActions

@Deprecated("This class is deprecated. Use the move action instead.")
class home(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {

        parent.places[tgtPlace]!!.characters.remove(tgtCharacter)
        parent.places["home_"]!!.characters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

}