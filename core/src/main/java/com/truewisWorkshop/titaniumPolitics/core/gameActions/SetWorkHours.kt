package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import kotlinx.serialization.Serializable

@Serializable
//SetWorkHours is performed by the workplace manager. It sets work hours of the workplace.
data class SetWorkHours(
    override val sbjCharacter: String,
    override val tgtPlace: String,
    var start: Int,
    var end: Int
) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, start: Int, end: Int, gameState: GameState) : this(
        sbjCharacter,
        tgtPlace,
        start,
        end
    ) {
        injectParent(gameState)
    }

    override fun chooseParams() {
    }

    override fun execute() {
        val workHoursDelta = tgtPlaceObj.workHoursEnd - tgtPlaceObj.workHoursStart - end + start
        tgtPlaceObj.workHoursStart = start
        tgtPlaceObj.workHoursEnd = end
        if (workHoursDelta > 0)
            parent.setPartyMutuality(
                sbjCharObj.division!!.name,
                weightedDelta = -workHoursDelta * 1.0 * tgtPlaceObj.plannedWorker / sbjCharObj.division!!.size,
                reasonKey = "SetWorkHoursIncrease"
            )
    }

    override fun isValid(): Boolean {
        if (!reason(sbjCharacter == tgtPlaceObj.workplaceParty?.overseer, "setWorkHours-notOverseer")) return false
        val who =
            (parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
                .flatMap { it.value.currentCharacters }).toHashSet()

        parent.parties.values.find { it.members.containsAll(who + sbjCharacter) }!!
        return tgtPlaceObj.manager == sbjCharacter
    }

    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is SetWorkHours && (info.action as SetWorkHours).let {
            it.start == this.start && it.end == this.end && it.tgtPlace == this.tgtPlace
        }) || (info.type == InformationType.HUMAN_RESOURCES && info.tgtPlace == this.tgtPlace) /*Do not check time for now, it is quite tricky.*/
    }

}