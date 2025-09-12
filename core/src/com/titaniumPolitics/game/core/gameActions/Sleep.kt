package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Sleep(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {
        Logger.write("$sbjCharacter slept.", Logger.LogLevel.ACTION_VERBOSE)
        if (parent.characters[sbjCharacter]!!.age > 50)
            parent.characters[sbjCharacter]!!.health += 3
        else
            parent.characters[sbjCharacter]!!.health += 4
        //Not affected by the will of the character, so no need to call super.execute()
        sbjCharObj.frozen += ReadOnly.constInt("SleepDuration")
        growPartyMutualityHeuristic()
    }

    override fun isValid(): Boolean {
        return tgtPlace == "home_$sbjCharacter"
    }

    override fun deltaWill(): MutualityMatrix {
        var amount = 0.0
        if (parent.characters[sbjCharacter]!!.health < ReadOnly.const("CriticalHealth"))
            amount += 10
        if (parent.characters[sbjCharacter]!!.trait.contains("old"))
            amount += 5
        if (sbjCharObj.hunger > 50)
            amount -= 5
        if (sbjCharObj.thirst > 50)
            amount -= 5
        val w = MutualityMatrix()
        w.addWill(sbjCharacter, amount, "Sleep")
        return w
    }


    /**
     * Create Party Mutuality information from mutuality information known to the character.
     * This is called when the character sleeps, representing the time spent reflecting on social relationships.
     */
    fun growPartyMutualityHeuristic() {
        //What this character thinks of other characters' view of themselves.
        val mutInfos = parent.informations.filter { (key, value) ->
            sbjCharacter in value.knownTo && value.type == InformationType.MUTUALITY && value.auxCharacter == sbjCharacter
        }
        //Collect all parties of these mutuality information characters. Other parties' heuristics are not generated.
        parent.parties.filter { (key, value) ->
            mutInfos.any { info ->
                info.value.tgtCharacter in value.members
            }
        }.forEach { (partyName, party) ->
            //Take mutInfo average as the heuristic for this party's mutuality towards the character.
            val avg =
                mutInfos.filter { it.value.tgtCharacter in party.members }.map { it.value.amount }.average()
            Information(
                author = null,
                creationTime = parent.time,
                tgtTime = parent.time,
                type = InformationType.PARTY_MUTUALITY,
                auxCharacter = sbjCharacter,
                tgtParty = partyName,
                amount = avg.toInt()
            )
                .apply {
                    knownTo = hashSetOf(sbjCharacter)
                    parent.addInformation(this)
                }

        }

    }

}