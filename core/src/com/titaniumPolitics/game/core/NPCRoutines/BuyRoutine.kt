package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import kotlinx.serialization.Serializable

@Serializable
class BuyRoutine(val buyResource: String) : Routine() {
    var err = false
    lateinit var tradeCharacter: String
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        //Try to trade for resources
        //Select a character to trade with, based on the information known to the character.

        val info = gState.informations.values.filter {
            it.type == InformationType.RESOURCES && it.tgtCharacter != null && it.tgtCharacter != name && it.resources.containsKey(
                buyResource
            ) && it.resources[buyResource] > 10 && it.knownTo.contains(
                name
            )
        }
        tradeCharacter = if (info.isNotEmpty()) {//If this character knows a character with the resource
            info.random().tgtCharacter!!
        } else
            gState.characters.keys.filter { it != name }.random()

        //FindCharacter
        // if the character is not in the same place.
        if (place != gState.places.values.find { it.characters.contains(tradeCharacter) }!!.name) {
            if (subroutines.none { it is FindCharacterRoutine })
                return FindCharacterRoutine(tradeCharacter)
        } else {
            //If the character is in the same place, start a conversation first
            if (gState.ongoingMeetings.none {
                    it.value.currentCharacters.containsAll(
                        listOf(
                            name,
                            tradeCharacter
                        )
                    )
                }) {
                if (subroutines.none { it is TalkRoutine })
                    return TalkRoutine(
                        tradeCharacter, MeetingAgenda(
                            AgendaType.REQUEST, name, attachedRequest = Request(
                                UnofficialResourceTransfer(
                                    tradeCharacter,
                                    tgtPlace = place,
                                    "home_$name",
                                    true,
                                    Resources(
                                        buyResource to
                                                gState.characters[name]!!.reliant * 1.0 //The amount of resource to request is proportional to the number of reliants.
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
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        TODO("Not supposed to be called")
    }

    override fun endCondition(name: String, place: String): Boolean {
        return true
    }
}