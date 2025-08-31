package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.Party.Role
import kotlinx.serialization.Serializable
import kotlin.collections.contains

@Serializable
data class HireManager(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val party get() = parent.parties.filter { (_, value) -> value.leader == sbjCharacter }.values.first()
    var employee: String? = null
    var role = Role.ADMINISTRATOR //The position to be hired, e.g. "administrator", "treasurer", "overseer", etc.


    override fun execute() {
        party.addMember(employee!!, role)
        if (sbjCharObj.division == parent.player.division)
            parent.knownCharactersToPlayer += employee!! //If hired into the player's division, add the employee to known characters.
        super.execute()
    }

    override fun isValid(): Boolean {
        return employee in availableEmployees() && party.getCharByRole(role) == null
    }

    fun availableEmployees(): List<String> {
        return tgtPlaceObj.characters.filter {
            //Cannot be anonymous.
            if (parent.characters[it]!!.type == Character.Type.ANON) {
                return@filter false
            }
            //Employee cannot be in triumvirate, be a division leader or the director.
            if (employee in parent.parties["triumvirate"]!!.members || employee in parent.parties.filter { it.value.type == Party.Type.DIVISION }.values.map { it.leader } || parent.places.any {
                    it.value.manager == employee
                }) {
                return@filter false
            }

            //The employee cannot be in lower management.
            if (parent.places.any { (_, value) ->
                    value.workplaceParty?.members?.contains(employee) == true
                }) {
                return@filter false
            }
            return@filter true

        }
    }

    fun pickBestEmployee() {
        employee = availableEmployees().maxByOrNull {
            when (role) {
                Role.ADMINISTRATOR -> parent.characters[it]!!.stats.ethos
                Role.TREASURER -> parent.characters[it]!!.stats.logos
                Role.OVERSEER -> parent.characters[it]!!.stats.pathos
                else -> parent.characters[it]!!.stats.ethos + parent.characters[it]!!.stats.logos + parent.characters[it]!!.stats.pathos //Default case, sum of all stats.
            }
        }
    }

}