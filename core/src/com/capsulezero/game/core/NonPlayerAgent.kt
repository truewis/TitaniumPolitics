package com.capsulezero.game.core

import com.capsulezero.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class NonPlayerAgent {
    //TODO: implement pathfinding. For now, just teleport to the place
    var command: Command? = null
    fun chooseAction(gameState: GameState, character: String, place: String): GameAction {
        //Basic needs
        if (gameState.characters[character]!!.health < 20) {
            move(gameState, character, place).also {
                it.placeTo = "home"
                return it
            }
        }
        //If in a conference or a meeting, wait for it to end.
        if (gameState.ongoingConferences.any {
                it.value.characters.contains(character) && it.value.currentCharacters.contains(
                    character
                )
            })//If in a conference
        {
            return wait(gameState, character, place)
        }
        if (gameState.ongoingMeetings.any {
                it.value.characters.contains(character) && it.value.currentCharacters.contains(
                    character
                )
            })//If in a conference
        {
            return wait(gameState, character, place)
        }
        //If there is a command, execute it.
        if (command != null) {
            if (place != command!!.place)
                return move(gameState, character, place).also { it.placeTo = command!!.place }
            else
                when (command!!.action) {
                    "investigateAccidentScene" -> {
                        command = null;
                        //execute if in the list, otherwise wait.
                        return if(GameEngine.availableActions(gameState, place = place, character = character).contains("investigateAccidentScene"))
                            investigateAccidentScene(gameState, character, place)
                        else wait(gameState, character, place)
                    }

                    "clearAccidentScene" -> {
                        command = null;
                        //execute if in the list, otherwise wait.
                        return if(GameEngine.availableActions(gameState, place = place, character = character).contains("clearAccidentScene"))
                            clearAccidentScene(gameState, character, place)
                        else wait(gameState, character, place)
                    }
                }
        }
        if (gameState.ongoingConferences.any {
                it.value.characters.contains(character) && !it.value.currentCharacters.contains(
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
                        it.value.characters.contains(character) && !it.value.currentCharacters.contains(character)
                    }.keys.first()
                    return it
                }
            //----------------------------------------------------------------------------------Move to the conference
        }
        if (gameState.scheduledConferences.any { it.value.characters.contains(character) && it.value.time - gameState.time in -1..3 })//If a conference is soon
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
        if (gameState.ongoingMeetings.any {
                it.value.characters.contains(character) && !it.value.currentCharacters.contains(
                    character
                )
            })//If missed a meeting
        {
            //----------------------------------------------------------------------------------Move to the meeting
            val meetingPlace = gameState.ongoingMeetings.filter {
                it.value.characters.contains(character) && !it.value.currentCharacters.contains(character)
            }.values.first().place
            if (place != meetingPlace) {
                move(gameState, character, place).also {
                    it.placeTo = meetingPlace
                    return it
                }
            } else
                joinMeeting(gameState, character, place).also {
                    it.meetingName = gameState.ongoingMeetings.filter {
                        it.value.characters.contains(character) && !it.value.currentCharacters.contains(character)
                    }.keys.first()
                    return it
                }
            //----------------------------------------------------------------------------------Move to the meeting
        }
        if (gameState.scheduledMeetings.any { it.value.characters.contains(character) && it.value.time - gameState.time in -1..3 })//If a meeting is soon
        {
            //----------------------------------------------------------------------------------Move to the meeting
            val meetingPlace = gameState.ongoingMeetings.filter {
                it.value.characters.contains(character) && !it.value.currentCharacters.contains(character)
            }.values.first().place
            if (place != meetingPlace) {
                move(gameState, character, place).also {
                    it.placeTo = meetingPlace
                    return it
                }
            } else
                startMeeting(gameState, character, place).also {
                    it.meetingName =
                        gameState.scheduledMeetings.filter { it.value.characters.contains(character) && it.value.time - gameState.time in -1..3 }.keys.first()
                    return it
                }
            //----------------------------------------------------------------------------------Move to the meeting
        }
        //If an accident happened in the place of my control, investigate and clear it.
        gameState.places.values.filter { it.division == gameState.characters[character]!!.division }.forEach { place1 ->
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
                val res = GameEngine.isShortOfResources(apparatus, place1)
                if (res != "") {
                    //Find a place within my division with maximum res.
                    val resplace =
                        gameState.places.values.filter { it.division != "" && it.division == gameState.characters[character]!!.division }
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



        when (place) {//Choose action by place
            "home" -> {
                when (gameState.characters[character]!!.health) {
                    in 0..40 -> return sleep(gameState, character, place)
                    else ->
                        if (gameState.hour in 9..17) {
                            move(gameState, character, place).also {
                                it.placeTo = "mainControlRoom"
                                return it
                            }
                        } else {
                            return wait(gameState, character, place)
                        }
                }

            }

            "mainControlRoom" ->
                if (gameState.hour !in 9..17) {
                    move(gameState, character, place).also {
                        it.placeTo = "home"
                        return it
                    }
                } else {
                    return wait(gameState, character, place)
                }
            else -> {
                if (gameState.hour !in 9..17) {
                    move(gameState, character, place).also {
                        it.placeTo = "home"
                        return it
                    }
                } else {
                    return wait(gameState, character, place)
                }
            }
        }
        return wait(gameState, character, place)
    }
}