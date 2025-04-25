package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor3(var searchFrom: Int) : EventObject("Talking with Dr Paik.", true)
{

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("DrPaik") &&
            parent.informations.any { (key, info) ->
                info.creationTime > searchFrom && info.type == InformationType.APPARATUS_DURABILITY && info.tgtApparatus == "WaterStorage" && info.tgtPlace == "WelfareStationEast" && info.amount <= 30
            }
        )
        {
            onPlayDialogue("BribeDoctor3")
            parent.eventSystem.dataBase.add(Event_BribeDoctor4(parent.time))
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