package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState

@Deprecated("This class is deprecated. Division leaders are elected by the party members.")
class LeaderAssignment(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    var targetParty = ""
    var who = ""
    override fun chooseParams() {
        targetParty = GameEngine.acquire(parent.parties.filter { it.value.leader == "" }.keys.toList())
        who = GameEngine.acquire(parent.characters.keys.filter { parent.parties[targetParty]!!.members.contains(it) }
            .toList())
    }

    override fun isValid(): Boolean =
        parent.characters[sbjCharacter]!!.trait.contains("mechanic")//Only the mechanic can assign leaders.

    //TODO: Leader is voted by the party members.
    override fun execute() {
        parent.parties[targetParty]!!.leader = who
        parent.characters[sbjCharacter]!!.frozen++

    }

}