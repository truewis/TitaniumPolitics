package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
//SetWorkHours is performed by the workplace manager. It sets work hours of the workplace.
class SetWorkHours(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var start = 8
    var end = 17
    override fun chooseParams()
    {
    }

    override fun execute()
    {
        val workHoursDelta = tgtPlaceObj.workHoursEnd - tgtPlaceObj.workHoursStart - end + start
        tgtPlaceObj.workHoursStart = start
        tgtPlaceObj.workHoursEnd = end
        parent.setPartyMutuality(
            sbjCharObj.division!!.name,
            delta = -workHoursDelta * 1.0 * tgtPlaceObj.plannedWorker / sbjCharObj.division!!.size
        )
    }

    override fun isValid(): Boolean
    {
        val who =
            (parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
                .flatMap { it.value.currentCharacters }).toHashSet()

        parent.parties.values.find { it.members.containsAll(who + sbjCharacter) }!!
        return tgtPlaceObj.manager == sbjCharacter
    }

}