package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor4(var searchFrom: Int) : EventObject("Talking with Dr Paik.", true)
{

    @Transient
    override val exec = { _: Int, _: Int ->
        if (
            parent.informations.any { (key, info) ->
                info.creationTime > searchFrom && info.type == InformationType.ACTION && (info.action is UnofficialResourceTransfer).also {
                    with(info.action as UnofficialResourceTransfer) {
                        toWhere == "WelfareStationEast" && resources.contains(Resources("titaniumTank" to 1.0))
                    }
                }
            }
        )
        {
            onPlayDialogue("BribeDoctor4")
            parent.eventSystem.add(Event_BribeDoctor5())
            deactivate()
        }
    }


}