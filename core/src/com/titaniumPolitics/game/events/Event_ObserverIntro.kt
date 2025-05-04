package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ObserverIntro : EventObject("Introduction of the Observer.", true)
{

    override fun exec(a: Int, b: Int)
    {
        onPlayDialogue("ObserverIntro")
        parent.timeChanged += exec2
    }

    var isFunc2Played = false

    @Transient
    val exec2 = { _: Int, _: Int ->
        if (!isFunc2Played)
        {
            onPlayDialogue("ObserverIntro2")
            parent.timeChanged += exec3
            isFunc2Played = true
        }
    }

    @Transient
    val exec3 = { _: Int, _: Int ->
        if (parent.player.place.name == "constructionYardNorth"
        )
        {
            onPlayDialogue("ObserverIntro3")
            deactivate()
        }
    }

    override fun activate()
    {
        parent.onStart += { exec(0, 0) }
    }

    override fun deactivate()
    {
        parent.timeChanged -= exec2
        parent.timeChanged -= exec3
    }
}