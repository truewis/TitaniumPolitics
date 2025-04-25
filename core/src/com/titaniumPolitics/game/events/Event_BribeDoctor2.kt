package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor2 : EventObject("Talking with Dr Paik.", true)
{

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("DrPaik") && parent.player.place.name == "WelfareStationEast"
        )
        {
            onPlayDialogue("BribeDoctor2")
            parent.eventSystem.dataBase.add(Event_BribeDoctor3(searchFrom = parent.time))
            deactivate()
        }
    }

    override fun activate()
    {
        parent.timeChanged += func
    }

    override fun deactivate()
    {
        parent.timeChanged -= func
    }
}