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
        who =
            (tgtState.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }+tgtState.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }).toHashSet()
        application = GameEngine.acquire(listOf("praise", "criticize", "respond"))
    }
    override fun execute() {
        tgtState.informations[what]!!.knownTo+=who

        if(application == "praise"){
            tgtState.setMutuality(tgtCharacter, tgtState.informations[what]!!.tgtCharacter, 2.0)
        }
        else if(application == "criticize"){
            tgtState.setMutuality(tgtCharacter, tgtState.informations[what]!!.tgtCharacter, -5.0)
        }
        else if(application == "respond"){
            //responseTo = tgtState.informations[what]
           //TODO: response to a previous information given in the meeting, or a rumor
        }

        tgtState.characters[tgtCharacter]!!.frozen++
    }

}