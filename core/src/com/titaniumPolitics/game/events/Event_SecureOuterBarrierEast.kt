package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Apparatus
import com.titaniumPolitics.game.core.GameDataHandler
import com.titaniumPolitics.game.ui.GraphInfoUI
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_SecureOuterBarrierEast : EventObject("Introduction of Alina.", true), IQuestEventObject {
    val targetApparatus: Apparatus
        get() = parent.places["outerBarrierEast"]!!.apparatuses.first { apparatus -> apparatus.name == "barrier" }

    @Transient
    override val quest = Quest(
        "Secure the Outer Barrier East Barrier",
        "Keep the barrier durability above 50.",
        tgtPlace = "outerBarrierEast",
        onClick = {
            GraphInfoUI.instance.refreshGraph(parent.gdh.resourceMap["apparatusDurability"]!!.column(targetApparatus.ID))
            GraphInfoUI.instance.isVisible = true
        }
    )

    override fun exec(a: Int, b: Int) {

    }
}