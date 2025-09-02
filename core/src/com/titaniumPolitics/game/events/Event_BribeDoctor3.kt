package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor3(var searchFrom: Int) : EventObject("Talking with Dr Paik.", true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        "Investigate the water storage",
        "Talk to Dr Paik about the water storage at the Welfare Station East.",
        tgtPlace = "WelfareStationEast",
        tgtCharacters = listOf("DrPaik"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("DrPaik") &&
            parent.informations.any { (_, info) ->
                info.creationTime > searchFrom && info.type == InformationType.APPARATUS && info.tgtApparatus == "WaterStorage" && info.tgtPlace == "WelfareStationEast" && info.amount <= 30
            }
        ) {
            onPlayDialogue("BribeDoctor3")
            parent.eventSystem.add(Event_BribeDoctor4(parent.time))
            deactivate()
        }
    }


}