package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_PrologueDecideVisitAlina : EventObject(ReadOnly.questProp("PrologueDecideVisitAlina-name"), true) {

    override fun exec(a: Int, b: Int) {
        onPlayDialogue("Prologue4")
        parent.eventSystem.add(Event_PrologueVisitAlina())
        deactivate()
    }

}
