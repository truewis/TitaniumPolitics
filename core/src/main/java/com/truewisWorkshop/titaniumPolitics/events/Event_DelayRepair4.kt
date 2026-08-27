package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_DelayRepair4(
    val targetApparatusID: String,
    val hospitalApparatusID: String,
    val searchFrom: Int,
) : EventObject(ReadOnly.questProp("DelayRepair4-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("DelayRepair4-title"),
        ReadOnly.questProp("DelayRepair4-desc"),
        tgtPlace = "welfareStationEast",
    )

    private fun getFirstRelevantRepair(): Information? =
        parent.informations.values
            .filter { info ->
                info.creationTime > searchFrom &&
                    info.type == InformationType.ACTION &&
                    info.action is Repair &&
                    (info.action as Repair).apparatusID in setOf(targetApparatusID, hospitalApparatusID)
            }
            .minByOrNull { it.creationTime }

    override fun exec(a: Int, b: Int) {
        val firstRepair = getFirstRelevantRepair() ?: return
        val repairedId = (firstRepair.action as Repair).apparatusID
        if (repairedId == hospitalApparatusID) {
            parent.eventSystem.add(Event_DelayRepair5(targetApparatusID))
        }
        deactivate()
    }
}
