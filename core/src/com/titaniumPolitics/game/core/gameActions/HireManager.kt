package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Character
import kotlinx.serialization.Serializable
import kotlin.collections.contains

@Serializable
class HireManager(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    val party get() = parent.parties.filter { (_, value) -> value.leader == sbjCharacter }.values.first()
    var employee: String? = null
    var role = "administrator" //The position to be hired, e.g. "administrator", "treasurer", "overseer", etc.


    override fun execute() {
        when (role) {
            "administrator" -> {
                party.administrator = employee
            }

            "treasurer" -> {
                party.treasurer = employee
            }

            "overseer" -> {
                party.overseer = employee
            }
        }
        super.execute()
    }

    override fun isValid(): Boolean {
        return employee in availableEmployees() && when (role) {
            "administrator" -> party.administrator == null
            "treasurer" -> party.treasurer == null
            "overseer" -> party.overseer == null
            else -> false
        }
    }

    fun availableEmployees(): List<String> {
        return tgtPlaceObj.characters.filter {
            //Cannot be anonymous.
            if (parent.characters[it]!!.type == Character.Type.ANON) {
                return@filter false
            }
            //Employee cannot be in triumvirate, be a division leader or the director.
            if (employee in parent.parties["triumvirate"]!!.members || employee in parent.parties.filter { it.value.type == "division" }.values.map { it.leader } || parent.places.any {
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
                "administrator" -> parent.characters[it]!!.stats.ethos
                "treasurer" -> parent.characters[it]!!.stats.logos
                "overseer" -> parent.characters[it]!!.stats.pathos
                else -> 0
            }
        }
    }

}