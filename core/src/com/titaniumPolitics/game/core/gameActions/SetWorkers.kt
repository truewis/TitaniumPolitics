package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
//SetWorkers is performed by the workplace manager. It sets the number of unnamed workers per apparatus.
class SetWorkers(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var workers = 0
    var apparatusID = ""
    override fun chooseParams()
    {
    }

    override fun execute()
    {
        parent.getApparatus(apparatusID).plannedWorker == workers

    }

    override fun isValid(): Boolean
    {
        if (parent.getApparatusPlace(apparatusID).name != tgtPlace) return false

        (parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
            .flatMap { it.value.currentCharacters }).toHashSet()

        return parent.getApparatusPlace(apparatusID).manager == sbjCharacter
    }

}