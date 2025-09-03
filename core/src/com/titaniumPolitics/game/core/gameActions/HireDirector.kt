package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class HireDirector(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val division get() = parent.parties.filter { (_, value) -> value.leader == sbjCharacter && value.type == Party.Type.DIVISION }.values.first()
    var newHire: String? = null
    var workplace = ""


    override fun execute() {
        parent.places[workplace]?.workplaceParty!!
            .addMember(newHire!!, Party.Role.NONE)
        parent.places[workplace]?.workplaceParty!!.changeLeader(newHire!!)
        if (sbjCharObj.division == parent.player.division)
            parent.knownCharactersToPlayer += newHire!! //If hired into the player's division, add the employee to known characters.
        parent.characters[newHire]!!.type = Character.Type.DIRECTOR //Switch type. Will this prevent bug?
    }

    override fun isValid(): Boolean {
        if (newHire == null)
            return false

        if (newHire !in availableEmployees())
            return false
        //Workplace must be a place that is managed by the party.
        if (parent.places[workplace]!!.responsibleDivision != division.name)
            return false

        //Workplace manager must be null.
        if (parent.places[workplace]!!.manager != null) {
            return false
        }

        //Workplace party leader must be null.
        if (parent.places[workplace]!!.workplaceParty!!.leader != null) {
            Logger.write(
                "parent.places[workplace]?.manager is null but Workplace party leader is not null.",
                Logger.LogLevel.ERROR
            )
            return false
        }

        return true
    }

    fun availableEmployees(): List<String> {
        return tgtPlaceObj.characters.filter {
            //Cannot be anonymous.
            if (parent.characters[it]!!.type == Character.Type.ANON) {
                return@filter false
            }
            //Employee cannot be in triumvirate, be a division leader or the director.
            if (newHire in parent.parties["triumvirate"]!!.members || newHire in parent.parties.filter { it.value.type == Party.Type.DIVISION }.values.map { it.leader } || parent.places.any {
                    it.value.manager == newHire
                }) {
                return@filter false
            }

            //The employee must be a member of the party if they are in lower management.
            if (parent.places.any { (_, value) ->
                    value.workplaceParty?.members?.contains(newHire) == true
                }) {
                if (newHire !in division.members) {
                    return@filter false
                }
            }
            return@filter true

        }
    }

    fun pickBestEmployee() {
        newHire =
            availableEmployees().maxByOrNull { parent.characters[it]!!.stats.pScale } //TODO: Add more criteria for picking the best employee.
    }
}