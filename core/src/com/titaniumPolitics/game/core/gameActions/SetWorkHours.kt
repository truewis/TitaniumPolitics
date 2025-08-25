package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable

@Serializable
//SetWorkHours is performed by the workplace manager. It sets work hours of the workplace.
data class SetWorkHours(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    var start = 8
    var end = 17
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

    override fun deltaWill(): Double {
        return super.deltaWill() * sbjCharObj.stats.pScale
    }

}