package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class chat(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    override fun execute() {
        val who = tgtState.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }
        //TODO: chat
        //TODO: get hints on the information the opponent character has.
        //Also give hints on the information this character has.

        tgtState.informations.filter { it.value.knownTo.contains(tgtCharacter) }.map { it.key }.forEach { tgtState.informations[it]!!.knowExistence.addAll(who)}

        who.forEach {
            tgtState.informations.filter { info->info.value.knownTo.contains(it) }.map { it.key }.forEach { tgtState.informations[it]!!.knowExistence.add(tgtCharacter)}
        }

        //if (tgtCharacter == who){ println("You chat with yourself.");return}
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}