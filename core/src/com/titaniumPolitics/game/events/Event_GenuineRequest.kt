package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_GenuineRequest(val requestKey: String) : EventObject("A reasonable request", true), IQuestEventObject {
    val request get() = parent.requests[requestKey]!!

    @Transient
    override val quest = Quest(
        "Request from %s".format(ReadOnly.charProp(request.issuedBy.firstOrNull() ?: "Someone")),
        description = "You were requested to work on %s".format(ReadOnly.prop(request.action::class.simpleName!!)),
        tgtPlace = request.action.tgtPlace,
    )

    override fun exec(a: Int, b: Int) {
        if (request.completed)
            deactivate()
    }


}