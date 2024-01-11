package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information

class infoShare(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var who = hashSetOf<String>()
    var what = ""
    var application = "" //praise, criticize, respond
    var responseTo: Information? = null //if application is respond
    override fun chooseParams() {
        //TODO: ability to fabricate information
        what = GameEngine.acquire(parent.informations.filter { it.value.knownTo.contains(tgtCharacter) }.map { it.key })

        //To all participants of the meeting/conference
        application = GameEngine.acquire(listOf("praise", "criticize", "respond", "report"))
    }
    override fun execute() {
        who =
            (parent.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }+parent.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }).toHashSet()

        val party = parent.parties.values.find { it.members.containsAll(who+tgtCharacter) }!!.name
        parent.informations[what]!!.knownTo+=who

        when (application) {
            "praise" -> {
                val factor = .1

                //If party's average opinion of the character that is discussed is positive, then the party integrity increases. Otherwise, it decreases.
                    parent.setPartyMutuality(party, party, (parent.parties[party]!!.individualMutuality(parent.informations[what]!!.tgtCharacter) - 50)*factor)

                //Every attendant's individual mutuality to the character that is discussed increases.
                who.forEach { parent.setMutuality(it, parent.informations[what]!!.tgtCharacter, 2.0) }
            }
            "criticize" -> {
                val factor = -.2
                //If party's average opinion of the character that is discussed is negative, then the party integrity increases. Otherwise, it decreases.
                //It is easier to criticize someone than to praise someone to increase party integrity.
                parent.setPartyMutuality(party, party, (parent.parties[party]!!.individualMutuality(parent.informations[what]!!.tgtCharacter)-50)*factor)
                who.forEach { parent.setMutuality(it, parent.informations[what]!!.tgtCharacter, -5.0) }
            }
            "respond" -> {
                //responseTo = tgtState.informations[what]
                //TODO: response to a previous information given in the meeting, or a rumor
            }
        }
        //TODO: party integrity affects the chances. Party integrity is affected.

        parent.characters[tgtCharacter]!!.frozen++
    }

}