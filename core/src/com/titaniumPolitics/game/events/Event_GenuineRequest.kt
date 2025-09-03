package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_GenuineRequest(val requestKey: String) : EventObject("A reasonable request: $requestKey", true),
    IQuestEventObject {
    val request get() = parent.requests[requestKey]!!

    val hasInformation
        get() = parent.informations.values.any {
            parent.playerName in it.knownTo && request.action.isProofOfWork(it)
        }
    val hasPrepared
        get() = parent.informations.any {
            parent.playerName in it.value.knownTo && request.action.isProofOfWork(it.value)
                    && it.key in parent.player.preparedInfoKeys
        }

    override val quest
        get() = Quest(
            "Request from %s".format(ReadOnly.charProp(request.issuedBy.firstOrNull() ?: "Someone")),
            description = "You were requested to work on %s".format(ReadOnly.prop(request.action::class.simpleName!!)),
            tgtPlace = request.action.tgtPlace,
            tooltip = SimpleTextTooltipUI(
                "You have relevant information: $hasInformation\n" +
                        "You have prepared the information: $hasPrepared\n" +
                        "You have reported back the information: False"

            )
        )

    override fun exec(a: Int, b: Int) {
        if (request.completed)
            deactivate()
    }


}