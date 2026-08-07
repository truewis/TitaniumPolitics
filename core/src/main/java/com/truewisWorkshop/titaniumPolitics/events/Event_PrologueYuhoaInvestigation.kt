package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_PrologueYuhoaInvestigation : EventObject("Prologue1_1", true) {

    override fun exec(a: Int, b: Int) {
        val playerPlace = parent.player.place.name
        if (playerPlace == "outerBarrierEast") {
            if (parent.characters["Yuhoa"]?.place?.name != playerPlace) {
                parent.characters["Yuhoa"]?.forceMoveToPlace(playerPlace)
            }
            onPlayDialogue("Prologue1_1")
            parent.eventSystem.add(Event_PrologueAlinaAccident2())
            deactivate()
        }
    }

}

