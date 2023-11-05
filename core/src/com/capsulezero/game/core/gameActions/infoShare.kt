package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.Information

class infoShare(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var who = hashSetOf<String>()
    var what = ""
    var application = "" //praise, criticize, respond
    var responseTo: Information? = null //if application is respond
    override fun chooseParams() {
        //TODO: ability to fabricate information
        what =
            GameEngine.acquire(tgtState.informations.filter { it.value.knownTo.contains(tgtCharacter) }.map { it.key })

        //To all participants of the meeting/conference
        application = GameEngine.acquire(listOf("praise", "criticize", "respond"))
    }
    override fun execute() {
        who =
            (tgtState.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }+tgtState.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }).toHashSet()

        val party = tgtState.parties.values.find { it.members.containsAll(who+tgtCharacter) }!!.name
        tgtState.informations[what]!!.knownTo+=who

        when (application) {
            "praise" -> {
                val factor = .1

                //If party's average opinion of the character that is discussed is positive, then the party integrity increases. Otherwise, it decreases.
                    tgtState.setPartyMutuality(party, party, (tgtState.parties[party]!!.individualMutuality(tgtState.informations[what]!!.tgtCharacter) - 50)*factor)

                //Every attendant's individual mutuality to the character that is discussed increases.
                who.forEach { tgtState.setMutuality(it, tgtState.informations[what]!!.tgtCharacter, 2.0) }
            }
            "criticize" -> {
                val factor = -.2
                //If party's average opinion of the character that is discussed is negative, then the party integrity increases. Otherwise, it decreases.
                //It is easier to criticize someone than to praise someone to increase party integrity.
                tgtState.setPartyMutuality(party, party, (tgtState.parties[party]!!.individualMutuality(tgtState.informations[what]!!.tgtCharacter)-50)*factor)
                who.forEach { tgtState.setMutuality(it, tgtState.informations[what]!!.tgtCharacter, -5.0) }
            }
            "respond" -> {
                //responseTo = tgtState.informations[what]
                //TODO: response to a previous information given in the meeting, or a rumor
            }
        }
        //TODO: party integrity affects the chances. Party integrity is affected.

        tgtState.characters[tgtCharacter]!!.frozen++
    }

}