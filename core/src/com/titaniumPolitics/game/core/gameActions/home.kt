package com.titaniumPolitics.game.core.gameActions

@Deprecated("This class is deprecated. Use the move action instead.")
class home(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {

        parent.places[tgtPlace]!!.characters.remove(sbjCharacter)
        parent.places["home_"]!!.characters.add(sbjCharacter)
        parent.characters[sbjCharacter]!!.frozen++
    }

}