package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class BuyRoutine(val buyResource: String, val buyAmount: Double) : Routine() {
    lateinit var tradeCharacter: String
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        //Try to trade for resources
        //Select a character to trade with, based on the information known to the character.

        val info = gState.informations.values.filter {
            it.type == InformationType.RESOURCES && it.tgtCharacter != null && it.tgtCharacter != name && it.resources.containsKey(
                buyResource
            ) && it.resources[buyResource] > buyAmount && it.knownTo.contains(
                name
            )
        }

        // For luxury resources, prefer the server (treasurer) at the appropriate private store.
        val storeServer = gState.parties.values.firstOrNull { party ->
            party.type == com.titaniumPolitics.game.core.Party.Type.WORKPLACE &&
                party.treasurer != null &&
                party.treasurer != name &&
                party.home != null &&
                gState.places.containsKey(party.home) &&
                gState.places[party.home]!!.responsibleDivision == null &&
                //Name contains buyResource, i.e. fineFoodStore
                gState.places[party.home]!!.name.contains(buyResource)
        }?.treasurer

        tradeCharacter = when {
            storeServer != null -> storeServer // Prefer store server for this luxury resource
            info.isNotEmpty() -> info.random().tgtCharacter!! // Otherwise use known information
            else -> gState.activeCharacters.keys.filter { it != name && gState.characters[it]!!.type != Character.Type.ANON }
                .random()
        }


        //FindCharacter
        // if the character is not in the same place.
        if (place != gState.places.values.find { it.characters.contains(tradeCharacter) }!!.name) {
            return FindCharacterRoutine(tradeCharacter)
        } else {
            //Only if there is no ongoing meeting, start a meeting with the character to trade.
            return AttendPrivateMeetingRoutine(
                tradeCharacter, MeetingAgenda(
                    AgendaType.REQUEST, name, attachedRequest = Request(
                        UnofficialResourceTransfer(
                            tradeCharacter,
                            tgtPlace = place,
                            "home_$name",
                            true,
                            Resources(
                                buyResource to
                                    buyAmount
                            )
                        )//Created a command to transfer the resource.
                        ,
                        issuedTo = hashSetOf(tradeCharacter),
                        issuedBy = hashSetOf(name),
                        executeTime = gState.time
                    )
                )
            )
            //Since this is a request, the success of this routine cannot be known because it is up to tradeCharacter whether they send resource or not.

        }
    }

    override fun execute(name: String, place: String): GameAction {
        success()
        return Wait(name, place) //TODO: temporary implementation
    }

}
