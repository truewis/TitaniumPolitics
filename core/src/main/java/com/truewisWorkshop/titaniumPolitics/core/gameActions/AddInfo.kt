package com.titaniumPolitics.game.core.gameActions

import com.badlogic.gdx.math.MathUtils.clamp
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.MutualityMatrix
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


    /**
     *
     * Unit: Mutuality
     */
    fun effectivity(): Double {
        val ret = agenda.effectivity(parent, meeting, info, sbjCharObj)
        effectivityReason =
            "AddInfo-" + ret.second + if (ret.first > 0) "-positive" else if (ret.first < 0) "-negative" else ""
        return ret.first
    }

    /**
     * If the information has a direct mutuality effect, apply it here.
     * For example, hearing the information of praising or denouncing someone directly affects mutuality with both the subject and the object of the praise/denouncement.
     */
    fun directMutualityChange(w: MutualityMatrix, listener: String) {
        if (info.type == InformationType.ACTION && info.action is NewAgenda) {
            val na = info.action as NewAgenda
            if (na.agenda.type == AgendaType.PRAISE) {
                val praised = na.agenda.subjectParams["character"]!!
                w.addMutuality(
                    listener,
                    na.sbjCharacter,
                    parent.getMutNorm(listener, praised) * 5,
                    "AddInfo-Praise"
                )
                w.addMutuality(
                    listener,
                    praised,
                    parent.getMutNorm(listener, na.sbjCharacter) * 5,
                    "AddInfo-Praise"
                )

            }
            if (na.agenda.type == AgendaType.DENOUNCE) {
                val denounced = na.agenda.subjectParams["character"]!!
                w.addMutuality(
                    listener,
                    na.sbjCharacter,
                    -parent.getMutNorm(listener, denounced) * 5,
                    "AddInfo-Denounce"
                )
                w.addMutuality(
                    listener,
                    denounced,
                    -parent.getMutNorm(listener, na.sbjCharacter) * 5,
                    "AddInfo-Denounce"
                )
            }
        }
    }

    override fun execute() {

        agenda.informationKeys.add(infoKey)

        //The amount of attention spent can be modified here.
        //Attention is consumed.
        val newsDegree =
            1.0 - meeting.currentCharacters.intersect(info.knownTo).size / (.0 + meeting.currentCharacters.size)
        val effectivity = effectivity()
        meeting.currentAttention = clamp(
            meeting.currentAttention + (10 * effectivity * newsDegree * sbjCharObj.will / ReadOnly.const("mutualityMax")).toInt() - 20,
            0, 100
        )
        agenda.applyPersuasivenessDelta(sbjCharacter, effectivity)
        meeting.resolveAgendasByPersuasiveness(parent)
        parent.informations[infoKey]!!.knownTo.addAll(meeting.currentCharacters)
        super.execute()
    }

    override fun deltaWill(): MutualityMatrix {
        val w = MutualityMatrix()
        val effectivity = effectivity()
        //affect relation with the agenda author
        w.addMutuality(agenda.author, sbjCharacter, effectivity, "AddInfo")
        //The information is known to the characters in the meeting.
        val newsCharacters = meeting.currentCharacters - info.knownTo
        //Direct mutuality change caused by the information, if any.
        newsCharacters.forEach { listener ->
            directMutualityChange(w, listener)
            NewAgenda.affectListenerMutuality(
                effectivity / ReadOnly.const("mutualityMax"), w, agenda, sbjCharacter, listener, parent
            )
        }
        return w
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