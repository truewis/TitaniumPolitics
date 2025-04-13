package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

@Deprecated("Use infoShare instead")
class praise(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = ""
    override fun chooseParams()
    {
        who =
            GameEngine.acquire(parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
                .flatMap { it.value.currentCharacters })
    }

    override fun execute()
    {
        //TODO: praise
        if (sbjCharacter == who)
        {
            println("You praise yourself.");return
        }
        parent.setMutuality(sbjCharacter, who, 2.0)
        parent.characters[sbjCharacter]!!.frozen++
    }

}