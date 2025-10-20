package com.titaniumPolitics.game.events

import com.badlogic.gdx.graphics.Color.RED
import com.titaniumPolitics.game.core.Apparatus
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.ui.GraphInfoUI
import com.titaniumPolitics.game.ui.Quest
import com.truewisWorkshop.titaniumPolitics.ui.GraphScreen
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
        dueTime = 14400,
        onClick = {
            GraphInfoUI.instance.refreshGraph(
                parent.gdh.resourceMap["apparatusDurability"]!!.column(targetApparatus.ID),
                GraphScreen.DataType.DURABILITY
            )
            GraphInfoUI.instance.addHorizontalLine(40f, RED, 2f, "Critical")
            GraphInfoUI.instance.isVisible = true
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.time > quest.dueTime!!) {
            deactivate(true)
            return
        }
        if (targetApparatus.durability < 40) {
            deactivate(false)
            GameEngine.gameOver("GameOver-BarrierDestroyed")
            return
        }

    }
}
