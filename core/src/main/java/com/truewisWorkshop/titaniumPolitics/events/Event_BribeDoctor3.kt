package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor3(var searchFrom: Int) : EventObject(ReadOnly.questProp("BribeDoctor3-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("BribeDoctor3-title"),
        ReadOnly.questProp("BribeDoctor3-desc"),
        tgtPlace = "WelfareStationEast",
        tgtCharacters = listOf("DrPaik"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("DrPaik") &&
            parent.informations.any { (_, info) ->
                info.creationTime > searchFrom && info.type == InformationType.APPARATUS && info.tgtApparatusName == "WaterStorage" && info.tgtPlace == "WelfareStationEast" && info.amount <= 30
            }
        ) {
            onPlayDialogue("BribeDoctor3")
            parent.eventSystem.add(Event_BribeDoctor4(parent.time))
            deactivate()
        }
    }


}