package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_PrologueAlinaAccident2 : EventObject(ReadOnly.questProp("PrologueAlinaAccident2-name"), true) {


    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "safetyHeadquarters") {
            listOf("Peiyu", "Astinomis", "ctrler").forEach { charName ->
                if (parent.characters[charName]?.place?.name != parent.player.place.name) {
                    parent.characters[charName]?.forceMoveToPlace(parent.player.place.name)
                }
            }
            onPlayDialogue("Prologue2")
            parent.eventSystem.add(Event_PrologueGoHome())
            deactivate()
        }

    }

}
