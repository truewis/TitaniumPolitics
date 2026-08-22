package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Hans3(val searchFrom: Int) : EventObject(ReadOnly.questProp("Hans3-name"), true), IQuestEventObject {
    private fun hasPreparedAccidentInfo() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.any { info ->
            info.creationTime > searchFrom &&
                info.type == InformationType.ACCIDENT &&
                info.tgtPlace == "outerBarrierEast"
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Hans3-title"),
        ReadOnly.questProp("Hans3-desc"),
        tgtPlace = "outerBarrierEast",
        tgtCharacters = listOf("Hans"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "outerBarrierEast" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Hans") == true
            && hasPreparedAccidentInfo()
            && parent.getMutuality("Hans", parent.playerName) >= 10.0
        ) {
            onPlayDialogue("Hans3")
            parent.eventSystem.add(Event_Hans4(parent.time))
            deactivate()
        }
    }
}
