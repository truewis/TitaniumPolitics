package com.titaniumPolitics.game.core.gameActions

import com.badlogic.gdx.math.MathUtils.clamp
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
data class AddInfo(
    override val sbjCharacter: String, override val tgtPlace: String, var infoKey: String,
    var agendaIndex: Int
) : GameAction() {
    constructor(
        sbjCharacter: String, tgtPlace: String, infoKey: String,
        agendaIndex: Int, gameState: GameState
    ) : this(sbjCharacter, tgtPlace, infoKey, agendaIndex) {
        injectParent(gameState)
    }

    val agenda
        get() = sbjCharObj.currentMeeting!!.agendas[agendaIndex]
    val info
        get() = parent.informations[infoKey]!!
    val meeting
        get() = sbjCharObj.currentMeeting!!
    var effectivityReason = ""


    //Unit: Mutuality
    fun effectivity(): Double {
        val ret = agenda.effectivity(parent, meeting, info, sbjCharObj)
        effectivityReason =
            "AddInfo-" + ret.second + if (ret.first > 0) "-positive" else if (ret.first < 0) "-negative" else ""
        return ret.first
    }

    override fun execute() {

        agenda.informationKeys.add(infoKey)

        //The amount of attention spent can be modified here.
        //Attention is consumed.
        val newsDegree =
            1.0 - meeting.currentCharacters.intersect(info.knownTo).size / (.0 + meeting.currentCharacters.size)
        meeting.currentAttention = clamp(
            meeting.currentAttention + (10 * effectivity() * newsDegree * sbjCharObj.will / ReadOnly.const("mutualityMax")).toInt() - 20,
            0, 100
        )
        //The information is known to the characters in the meeting.
        parent.informations[infoKey]!!.knownTo.addAll(meeting.currentCharacters)
        //Call the mutuality modifier function of the agenda. If the added information is effective, the mutuality effect of the agenda is reinforced, and vice versa.
        NewAgenda.extracted(effectivity(), meeting, agenda, agenda.author, parent)
        //affect relation with the agenda author
        parent.setMutuality(agenda.author, sbjCharacter, effectivity(), "AddInfo")
        super.execute()
    }

    override fun isValid(): Boolean {
        //Array index out of bounds check.
        if (meeting.agendas.size <= agendaIndex)
            return false
        //If the information is already presented in the meeting, it cannot be presented again.
        if (meeting.agendas.any { it.informationKeys.contains(infoKey) })
            return false
        if (!reason(
                effectivity() != 0.0,
                "AddInfo-NotEffective",
            )
        ) return false
        return true //We are assuming that the information is always valid. Whether the information is effective or not is a different matter.
    }

}