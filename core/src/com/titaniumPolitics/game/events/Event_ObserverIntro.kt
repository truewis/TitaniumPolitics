package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.quests.Quest1
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ObserverIntro : EventObject("Introduction of the Observer.", true)
{
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = {
        DialogueUI.instance.playDialogue("ObserverIntro")
        parent.timeChanged += func2
    }
    var isFunc2Played = false

    @Transient
    val func2 = { _: Int, _: Int ->
        if (!isFunc2Played)
        {
            DialogueUI.instance.playDialogue("ObserverIntro2")
            parent.timeChanged += func3
            isFunc2Played = true
        }
    }

    @Transient
    val func3 = { _: Int, _: Int ->
        if (parent.player.place.name == "constructionYardNorth"
        )
        {
            DialogueUI.instance.playDialogue("ObserverIntro3")
            deactivate()
        }
    }

    override fun activate()
    {
        parent.onStart += func
    }

    override fun deactivate()
    {
        parent.onStart -= func
        parent.timeChanged -= func2
        parent.timeChanged -= func3
    }
}