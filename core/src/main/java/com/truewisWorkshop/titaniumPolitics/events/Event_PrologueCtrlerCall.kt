package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_PrologueCtrlerCall : EventObject(ReadOnly.questProp("PrologueCtrlerCall-name"), true) {

    override fun exec(a: Int, b: Int) {
        // Only continue the cutscene while Rui is still at the accident site.
        if (parent.player.place.name != "outerBarrierEast") {
            if (parent.characters["ctrler"]!!.place.name != parent.player.place.name) {
                parent.characters["ctrler"]!!.forceMoveToPlace(parent.player.place.name)
            }
            onPlayDialogue("Prologue1")
            parent.eventSystem.add(Event_PrologueYuhoaInvestigation())
            deactivate()
        }
    }

}
