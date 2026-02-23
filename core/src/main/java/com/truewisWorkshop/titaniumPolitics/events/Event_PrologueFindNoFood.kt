package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_PrologueFindNoFood : EventObject(ReadOnly.questProp("PrologueFindNoFood-name"), true) {

    override fun exec(a: Int, b: Int) {
        onPlayDialogue("Prologue3_1")
        parent.eventSystem.add(Event_PrologueDecideVisitAlina())
        deactivate()
    }

}
