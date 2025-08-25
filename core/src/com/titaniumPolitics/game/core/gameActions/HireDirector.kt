package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class HireDirector(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val div get() = parent.parties.filter { (_, value) -> value.leader == sbjCharacter && value.type == "division" }.values.first()
    var employee: String? = null
    var workplace = ""


    override fun execute() {
        parent.parties["workplace_$workplace"]!!.leader = employee
        parent.places[workplace]?.manager = employee
    }

    override fun isValid(): Boolean {
        if (employee == null)
            return false

        if (employee !in availableEmployees())
            return false
        //Workplace must be a place that is managed by the party.
        if (parent.places[workplace]?.responsibleDivision != div.name)
            return false

        //Workplace manager must be null.
        if (parent.places[workplace]?.manager != null) {
            return false
        }

        //Workplace party leader must be null.
        if (parent.parties["workplace_$workplace"]?.leader != null) {
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
            if (employee in parent.parties["triumvirate"]!!.members || employee in parent.parties.filter { it.value.type == "division" }.values.map { it.leader } || parent.places.any {
                    it.value.manager == employee
                }) {
                return@filter false
            }

            //The employee must be a member of the party if they are in lower management.
            if (parent.places.any { (_, value) ->
                    value.workplaceParty?.members?.contains(employee) == true
                }) {
                if (employee !in div.members) {
                    return@filter false
                }
            }
            return@filter true

        }
    }

    fun pickBestEmployee() {
        employee =
            availableEmployees().maxByOrNull { parent.characters[it]!!.stats.pScale } //TODO: Add more criteria for picking the best employee.
    }
}