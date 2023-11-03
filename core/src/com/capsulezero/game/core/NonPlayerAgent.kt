package com.capsulezero.game.core

import com.capsulezero.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
class NonPlayerAgent {
    //TODO: implement pathfinding. For now, just teleport to the place
    var commands = arrayListOf<Command>()
    fun chooseAction(gameState: GameState, character: String, place: String): GameAction {
        //If there is almost no food or water, stop all activities and try to get some. ----------------------------------------------------------------------------
        if((gameState.characters[character]!!.resources["ration"] ?:0) <= (gameState.characters[character]!!.reliants.size+1) || (gameState.characters[character]!!.resources["water"] ?:0) <= (gameState.characters[character]!!.reliants.size+1)) {
            val wantedResource = if((gameState.characters[character]!!.resources["ration"] ?:0) <=(gameState.characters[character]!!.reliants.size+1)) "ration" else "water"
            if(gameState.characters[character]!!.trait.contains("thief"))
            {
                //Find a place within my division with maximum res.
                val resplace =
                    gameState.places.values.filter {
                        it.responsibleParty != "" && gameState.parties[it.responsibleParty]!!.members.contains(
                            character
                        )
                    }
                        .maxByOrNull { it.resources["ration"] ?: 0 }
                        ?: gameState.places.values.filter {
                            it.responsibleParty != "" && gameState.parties[it.responsibleParty]!!.members.contains(
                                character
                            )
                        }
                            .maxByOrNull { it.resources["water"] ?: 0 }
                        ?: return wait(gameState, character, place)
                if (place != resplace.name) {
                    move(gameState, character, place).also {
                        it.placeTo = resplace.name
                        return it
                    }
                } else {
                    unofficialResourceTransfer(gameState, character, place).also {
                        if ((gameState.characters[character]!!.resources["ration"] ?: 0) == 0) {
                            it.what = "ration"
                            it.amount = min(
                                (resplace.resources["ration"] ?: 0) / 2,
                                (gameState.characters[character]!!.reliants.size + 1) * 7
                            )
                        } else {
                            it.what = "water"
                            it.amount = min(
                                (resplace.resources["water"] ?: 0) / 2,
                                (gameState.characters[character]!!.reliants.size + 1) * 7
                            )
                        }
                        println("$character is stealing ration from ${resplace.name}: ${it.amount}")
                        return it
                    }
                }
            }
            else if (gameState.characters[character]!!.trait.contains("bargainer")){
                //Try to trade for resources
                //Select a character to trade with, based on the information known to the character.
                val tradeCharacter: String
                val info = gameState.informations.values.filter { it.type=="resource" && it.tgtCharacter!="" && it.tgtCharacter!=character && it.tgtResource == wantedResource&&it.amount>10 && it.knownTo.contains(character) }
                tradeCharacter = if(info.isNotEmpty()) {//If there is a character with the resource
                    info.random().tgtCharacter
                } else
                    gameState.characters.keys.filter { it!=character }.random()

                //Move to the place of the character
                val tradePlace = gameState.places.values.find { it.characters.contains(character) }!!.name
                if(tradePlace == "home")
                    return wait(gameState, character, place)//Wait for the character to leave home.
                if(place!=tradePlace)
                {
                    move(gameState, character, place).also { move ->
                        move.placeTo = gameState.places.values.find { it.characters.contains(character) }!!.name

                        return move
                    }
                }
                else
                {//TODO: start a conversation first
                    //Trade for the resource
                    trade(gameState, character, place).also { trade ->
                        trade.who = tradeCharacter
                        trade.item2 = wantedResource
                        trade.amount2 = gameState.characters[tradeCharacter]!!.reliants.size+1
                        //Give away unwanted resources
                        trade.item = gameState.characters[character]!!.resources.keys.filter { it!=wantedResource }.random()
                        trade.amount = gameState.characters[character]!!.resources[trade.item]?:0
                        //Give away information they want
                        trade.info = gameState.informations.values.filter { it.tgtCharacter==tradeCharacter && it.knownTo.contains(character) }.random()
                        //Give away actions they want

                        return trade
                    }
                }
            }
        }
        //------------------------------------------------------------------------------------------------------------------------------------------------

        when (place) {//Choose action by place
            "home" -> {
                if (gameState.characters[character]!!.hunger > 50 || gameState.characters[character]!!.thirst > 50)
                    return eat(gameState, character, place)
                when (gameState.characters[character]!!.health) {
                    in 0..40 -> return sleep(gameState, character, place)
                    else ->
                        if (gameState.hour in 9..17) {
                            move(gameState, character, place).also {
                                //Move to a random place such that the responsible party is one of the parties of the character.
                                try {
                                    it.placeTo = gameState.places.values.filter { pl->pl.responsibleParty != "" && gameState.parties[pl.responsibleParty]!!.members.contains(character) }.random().name

                                }catch (e:Exception)
                                {
                                    println("Warning: No place to commute found for $character")
                                    it.placeTo = "mainControlRoom"
                                }
                                return it
                            }
                        } else {
                            return wait(gameState, character, place)
                        }
                }

            }


            else -> {
                //Basic needs
                if (gameState.hour !in 9..17) {
                    move(gameState, character, place).also {
                        it.placeTo = "home"
                        return it
                    }
                }
                if (gameState.characters[character]!!.health < 30) {
                    move(gameState, character, place).also {
                        it.placeTo = "home"
                        return it
                    }
                }
                if (gameState.characters[character]!!.hunger > 80) {
                    move(gameState, character, place).also {
                        it.placeTo = "home"
                        return it
                    }
                }
                if (gameState.characters[character]!!.thirst > 60) {
                    move(gameState, character, place).also {
                        it.placeTo = "home"
                        return it
                    }
                }
            }
        }
        //If in a conference or a meeting, wait for it to end.
        if (gameState.ongoingConferences.any {
                it.value.scheduledCharacters.contains(character) && it.value.currentCharacters.contains(
                    character
                )
            })//If in a conference
        {
            return wait(gameState, character, place)
        }
        if (gameState.ongoingMeetings.any {
                it.value.scheduledCharacters.contains(character) && it.value.currentCharacters.contains(
                    character
                )
            })//If in a conference
        {
            return wait(gameState, character, place)
        }
        //If there is a command, execute it.
        if (commands.isNotEmpty()) {
            val command = commands.first()
            if (place != command.place)
                return move(gameState, character, place).also { it.placeTo = command.place }
            else {
                commands.removeAt(0)
                when (command.action) {
                    "investigateAccidentScene" -> {

                        //execute if in the list, otherwise wait.
                        return if (GameEngine.availableActions(gameState, place = place, character = character)
                                .contains("investigateAccidentScene")
                        )
                            investigateAccidentScene(gameState, character, place)
                        else wait(gameState, character, place)
                    }

                    "clearAccidentScene" -> {
                        //execute if in the list, otherwise wait.
                        return if (GameEngine.availableActions(gameState, place = place, character = character)
                                .contains("clearAccidentScene")
                        )
                            clearAccidentScene(gameState, character, place)
                        else wait(gameState, character, place)
                    }

                    "repair" -> {
                        //execute if in the list, otherwise wait.
                        return if (GameEngine.availableActions(gameState, place = place, character = character)
                                .contains("repair")
                        )
                            repair(gameState, character, place)
                        else wait(gameState, character, place)
                    }

                    else -> {
                        println("Warning: Unspecified command $command")
                        return wait(gameState, character, place)
                    }
                }

            }
        }
        if (gameState.ongoingConferences.any {
                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                    character
                )
            })//If missed a conference
        {
            //----------------------------------------------------------------------------------Move to the conference
            if (place != "mainControlRoom") {
                move(gameState, character, place).also {
                    it.placeTo = "mainControlRoom"
                    return it
                }
            } else
                joinConference(gameState, character, place).also {
                    it.meetingName = gameState.ongoingConferences.filter {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(character)
                    }.keys.first()
                    return it
                }
            //----------------------------------------------------------------------------------Move to the conference
        }
        if (gameState.scheduledConferences.any { it.value.scheduledCharacters.contains(character) && it.value.time - gameState.time in -1..3 })//If a conference is soon
        {
            //----------------------------------------------------------------------------------Move to the conference
            if (place != "mainControlRoom") {
                move(gameState, character, place).also {
                    it.placeTo = "mainControlRoom"
                    return it
                }
            } else
                return wait(gameState, character, place)//Wait for the conference to start.
            //----------------------------------------------------------------------------------Move to the conference
        }
        //If missed a meeting
        if (gameState.ongoingMeetings.any {
                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                    character
                )
            })
        {
            //----------------------------------------------------------------------------------Move to the meeting
            val meetingPlace = gameState.ongoingMeetings.filter {
                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(character)
            }.values.first().place
            if (place != meetingPlace) {
                move(gameState, character, place).also {
                    it.placeTo = meetingPlace
                    return it
                }
            } else
                joinMeeting(gameState, character, place).also {
                    it.meetingName = gameState.ongoingMeetings.filter {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(character)
                    }.keys.first()
                    return it
                }
            //----------------------------------------------------------------------------------Move to the meeting
        }
        if (gameState.scheduledMeetings.any { it.value.scheduledCharacters.contains(character) && it.value.time - gameState.time in -1..3 })//If a meeting is soon
        {
            //----------------------------------------------------------------------------------Move to the meeting
            val meetingPlace = gameState.ongoingMeetings.filter {
                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(character)
            }.values.first().place
            if (place != meetingPlace) {
                move(gameState, character, place).also {
                    it.placeTo = meetingPlace
                    return it
                }
            } else //If there is no meeting yet, create one. Concurrent meetings do not happen, as the meeting is created immediately only if there is no meeting in the place.
                startMeeting(gameState, character, place).also {
                    it.meetingName =
                        gameState.scheduledMeetings.filter { it.value.scheduledCharacters.contains(character) && it.value.time - gameState.time in -1..3 }.keys.first()
                    return it
                }
            //----------------------------------------------------------------------------------Move to the meeting
        }
        //If an accident happened in the place of my control, investigate and clear it.
        gameState.places.values.filter { it.responsibleParty!="" && gameState.parties[it.responsibleParty]!!.members.contains(character) }.forEach { place1 ->
            if (place1.isAccidentScene) {
                if (place != place1.name) {
                    move(gameState, character, place).also {
                        it.placeTo = place1.name
                        return it
                    }
                } else
                //TODO: implement investigateAccidentScene
                    clearAccidentScene(gameState, character, place).also {
                        return it
                    }
            }
        }

        //If a place in the map is short of resources, transfer resources to it.
        gameState.places.values.forEach fe@{ place1 ->
            place1.apparatuses.forEach { apparatus ->
                val res = GameEngine.isShortOfResources(apparatus, place1) //Type of resource that is short of.
                if (res != "") {
                    //Find a place within my division with maximum res.
                    val resplace =
                        gameState.places.values.filter { it.responsibleParty != ""&& gameState.parties[it.responsibleParty]!!.members.contains(character)}
                            .maxByOrNull { it.resources[res] ?: 0 }
                            ?: return@fe
                    if (place != resplace.name) {
                        move(gameState, character, place).also {
                            it.placeTo = resplace.name
                            return it
                        }
                    } else {
                        officialResourceTransfer(gameState, character, place).also {
                            it.what = res
                            it.toWhere = place1.name
                            it.amount = (resplace.resources[res] ?: 0) / 2
                            return it
                        }
                    }
                }
            }

        }
        //If I am short of resources, steal resources from a place within my division.
        if((gameState.characters[character]!!.resources["ration"]?:0)<(gameState.characters[character]!!.reliants.size+1)*7)//If less than week worth of ration left
        {
            //Find a place within my division with maximum res.
            val resplace =
                gameState.places.values.filter { it.responsibleParty != ""&&gameState.parties[it.responsibleParty]!!.members.contains(character)}
                    .maxByOrNull { it.resources["ration"] ?: 0 }
                    ?: return wait(gameState, character, place)
            if (place != resplace.name) {
                move(gameState, character, place).also {
                    it.placeTo = resplace.name
                    return it
                }
            } else {
                unofficialResourceTransfer(gameState, character, place).also {
                    it.what = "ration"
                    it.amount = min((resplace.resources["ration"] ?: 0) / 2, (gameState.characters[character]!!.reliants.size+1)*7)
                    println("$character is stealing ration from ${resplace.name}: ${it.amount}")
                    return it
                }
            }
        }

        //If I am short of resources, steal resources from a place within my division.
        if((gameState.characters[character]!!.resources["water"]?:0)<(gameState.characters[character]!!.reliants.size+1)*7)//If less than week worth of ration left
        {
            //Find a place within my division with maximum res.
            val resplace =
                gameState.places.values.filter { it.responsibleParty != "" && gameState.parties[it.responsibleParty]!!.members.contains(character)}
                    .maxByOrNull { it.resources["water"] ?: 0 }
                    ?: return wait(gameState, character, place)
            if (place != resplace.name) {
                move(gameState, character, place).also {
                    it.placeTo = resplace.name
                    return it
                }
            } else {
                unofficialResourceTransfer(gameState, character, place).also {
                    it.what = "water"
                    it.amount = min((resplace.resources["water"] ?: 0) / 2, (gameState.characters[character]!!.reliants.size+1)*7)
                    println("$character is stealing water from ${resplace.name}: ${it.amount}")
                    return it
                }
            }
        }




        return wait(gameState, character, place)
    }
}