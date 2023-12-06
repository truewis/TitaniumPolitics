package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class chat(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    override fun execute() {
        //TODO: acquire or give whole information to another character by chance.
        val who = tgtState.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }.filter { it != tgtCharacter }.toHashSet()

        //get hints on the information the opponent character has.
        //Also give hints on the information this character has.
        var count = 0
        tgtState.informations.filter { it.value.knownTo.contains(tgtCharacter) }.map { it.key }.forEach { tgtState.informations[it]!!.letKnowExistence(who);count++}

        who.forEach {
            tgtState.informations.filter { info->info.value.knownTo.contains(it) }.map { it.key }.forEach { tgtState.informations[it]!!.letKnowExistence(tgtCharacter);count++}
        }

        val factor = .1
        //Mutualities between this character and all other characters in the meeting/conference increase by the number of information exchanged + 1.
        who.forEach { tgtState.setMutuality(tgtCharacter, it, 1.0+count*factor)
            tgtState.setMutuality(it, tgtCharacter, 1.0+count*factor)
        }

        //if (tgtCharacter == who){ println("You chat with yourself.");return}
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}