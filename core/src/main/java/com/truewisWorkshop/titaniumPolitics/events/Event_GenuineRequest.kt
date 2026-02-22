package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable

@Serializable
class Event_GenuineRequest(val requestKey: String) : EventObject(ReadOnly.questProp("GenuineRequest-name").format(requestKey), true),
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
            by lazy {
                Quest(
                    ReadOnly.questProp("GenuineRequest-title").format(ReadOnly.charProp(request.issuedBy.firstOrNull() ?: "Someone")),
                    description = ReadOnly.questProp("GenuineRequest-desc").format(ReadOnly.prop(request.action::class.simpleName!!)),
                    tgtPlace = request.action.tgtPlace,
                    getTooltip = {
                        SimpleTextTooltipUI(
                            "You have relevant information: $hasInformation\n" +
                                    "You have prepared the information: $hasPrepared\n" +
                                    "You have reported back the information: False"

                        )
                    }
                )
            }

    override fun exec(a: Int, b: Int) {
        //Check if the request is still valid.
        //If the request is completed, or the request is removed, deactivate this event.
        if (parent.requests[requestKey] == null) {
            deactivate(false)
            return
        }
        if (request.completed)
            deactivate()
    }


}