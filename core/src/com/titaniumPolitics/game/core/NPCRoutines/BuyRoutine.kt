package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly.IDTH
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
        tradeCharacter = if (info.isNotEmpty()) {//If this character knows a character with the resource
            info.random().tgtCharacter!!
        } else
            gState.aliveCharacters.keys.filter { it != name && gState.characters[it]!!.type != Character.Type.ANON }
                .random()

        //Don't add new subroutine if already finding character.
        if (subroutines.none { it is FindCharacterRoutine }) {
            //FindCharacter
            // if the character is not in the same place.
            if (place != gState.places.values.find { it.characters.contains(tradeCharacter) }!!.name) {
                return FindCharacterRoutine(tradeCharacter)
            } else {
                //Only if there is no ongoing meeting, start a meeting with the character to trade.
                if (subroutines.none { it is IMeetingRoutine })
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
        //If too much time has passed, end the routine.
        if (gState.time - routineStartTime > IDTH) {
            failed()
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        success()
        return Wait(name, place) //TODO: temporary implementation
    }

}