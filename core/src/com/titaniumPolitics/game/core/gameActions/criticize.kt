package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

@Deprecated("Use infoShare instead")
class criticize(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
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
        //TODO: chat
        if (sbjCharacter == who)
        {
            println("You criticize yourself.");return
        }
        parent.setMutuality(sbjCharacter, who, -5.0)
        parent.characters[sbjCharacter]!!.frozen++
    }

}