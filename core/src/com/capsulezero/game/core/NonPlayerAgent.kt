package com.capsulezero.game.core

import com.capsulezero.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
class NonPlayerAgent(val character: String) : GameStateElement() {
    //TODO: implement pathfinding. For now, just teleport to the place
    var commands = arrayListOf<Command>()
    private var routines = arrayListOf<Routine>()//Routines are sorted by priority. 0th element is the current routine.
    val place
    get() = parent.places.values.find { it.characters.contains(character) }!!.name

    fun chooseAction(): GameAction {
        //1. High priority routine change
        //---------------------------------------------------------------------------------------------------------
        selectRoutine()
        //2. Execute action according to the current routine.--------------------------------------------------------------------------------------------------------
        return executeRoutine()
    }

    private fun selectRoutine() {
        //If there is almost no food or water, stop all activities and try to get some. ----------------------------------------------------------------------------
        if ((parent.characters[character]!!.resources["ration"]
                ?: 0) <= (parent.characters[character]!!.reliants.size + 1) || (parent.characters[character]!!.resources["water"]
                ?: 0) <= (parent.characters[character]!!.reliants.size + 1)
        ) {
            val wantedResource = if ((parent.characters[character]!!.resources["ration"]
                    ?: 0) <= (parent.characters[character]!!.reliants.size + 1)
            ) "ration" else "water"
            if (parent.characters[character]!!.trait.contains("thief")) {
                //Find a place within my division with maximum res.
                if(routines.none{it.name=="steal"})
                    routines.add(Routine("steal", 100).also { it.variables["stealResource"] = wantedResource })//Add a routine, priority higher than work.

            } else if (parent.characters[character]!!.trait.contains("bargainer")) {
                if(routines.none{it.name=="steal"})
                    routines.add(Routine("buy", 100).also { it.variables["wantedResource"] = wantedResource })//Add a routine, priority higher than work.
            }
        }


    }


    private fun executeRoutine(): GameAction {
        if (routines.isEmpty()) {
            whenIdle()
            if (routines.isEmpty())
                return wait(parent, character, place)
        }
        routines.sortByDescending { it.priority }
        //Leave meeting or conference if the routine was changed.
        if (routines[0].name != "attendMeeting" && parent.ongoingMeetings.any {
                it.value.currentCharacters.contains(
                    character
                )
            }) {
            return leaveMeeting(parent, character, place)
        }
        if (routines[0].name != "attendConference" && parent.ongoingConferences.any {
                it.value.currentCharacters.contains(
                    character
                )
            }) {
            return leaveConference(parent, character, place)
        }
        //Execute action by the current routine
        when (routines[0].name) {
            "rest" -> {
                if (place != "home") {
                    routines.add(Routine("move", routines[0].priority + 10).also {
                        it.variables["movePlace"] = "home"
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                }
                if (parent.characters[character]!!.hunger > 50 || parent.characters[character]!!.thirst > 50)
                    return eat(parent, character, place)
                when (parent.characters[character]!!.health) {
                    in 0..40 -> return sleep(parent, character, place)
                    else ->
                        if (parent.hour in 8..18) {//Preparation for work takes 1 hour. Normal work hours are 9-17.
                            routines.removeAt(0)//Remove the current routine.
                            routines.add(Routine("work", 0))
                            return executeRoutine()
                        } else {
                            return wait(parent, character, place)
                        }
                }
            }

            "move" -> {
                if (place == routines[0].variables["movePlace"]) {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else {

                    if (routines[0].variables["movePlace"]=="home")
                    {
                        if (place != parent.characters[character]!!.home) {
                            return move(parent, character, place).also { it.placeTo = parent.characters[character]!!.home }//If player is far from the home, go outside the home.
                        }
                        else
                        {
                            return home(parent, character, place)//If player is outside the home, go inside.
                        }
                    }
                    else
                    {
                        if(place == "home")//If the character is at home, go outside.
                            return move(parent, character, place).also { it.placeTo = parent.characters[character]!!.home }
                        return move(parent, character, place).also { it.placeTo = routines[0].variables["movePlace"]!! }
                    }

                    //TODO: implement pathfinding. For now, just teleport to the place
                }

            }

            "steal" -> {
                val resplace =
                    parent.places.values.filter {
                        it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                            character
                        )
                    }
                        .maxByOrNull { it.resources["ration"] ?: 0 }
                        ?: parent.places.values.filter {
                            it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                                character
                            )
                        }
                            .maxByOrNull { it.resources["water"] ?: 0 }
                if (resplace == null) {
                    //Stop stealing because there is no place to steal from.
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else {
                    if (place != resplace.name) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = resplace.name
                        })//Add a move routine with higher priority.
                    } else {
                        //Finish stealing

                        unofficialResourceTransfer(parent, character, place).also {
                            it.what = routines[0].variables["stealResource"]!!
                            it.amount = min(
                                (resplace.resources["ration"] ?: 0) / 2,
                                (parent.characters[character]!!.reliants.size + 1) * 7
                            )
                            println("$character is stealing ${it.what} from ${resplace.name}: ${it.amount}")
                            routines.removeAt(0)//Remove the current routine.
                            return it
                        }


                    }
                }


            }

            "work" -> {
                //If an accident happened in the place of my control, investigate and clear it.
                parent.places.values.filter {
                    it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                        character
                    )
                }.forEach { place1 ->
                    if (place1.isAccidentScene) {
                        if (place != place1.name) {
                            routines.add(Routine("move", routines[0].priority + 10).also {
                                it.variables["movePlace"] = place1.name
                            })//Add a move routine with higher priority.
                            return executeRoutine()
                        } else
                        //TODO: implement investigateAccidentScene. Right now the information is immediately known to the division leader.

                            clearAccidentScene(parent, character, place).also {
                                return it
                            }
                    }
                }

                //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest.
                if (parent.hour !in 8..18) {
                    routines.removeAt(0)//Remove the current routine.
                    routines.add(Routine("rest", 0))
                    return executeRoutine()
                }
                if(parent.characters[character]!!.health < 30 || parent.characters[character]!!.hunger > 80 || parent.characters[character]!!.thirst > 60)
                {
                    routines.removeAt(0)//Remove the current routine.
                    routines.add(Routine("rest", 50)) //Rest with higher priority.
                    return executeRoutine()
                }
                //If there is a command, execute it.
                if (commands.isNotEmpty()) {
                    val command = commands.first()
                    if (place != command.place)
                        return move(parent, character, place).also { it.placeTo = command.place }
                    else {
                        commands.removeAt(0)
                        routines.add(
                            Routine(
                                command.action,
                                routines[0].priority + 10
                            )
                        )//Add an action routine with higher priority.
                        return executeRoutine()
                    }
                }
                if (parent.ongoingConferences.any {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                            character
                        )
                    })//If missed a conference
                {
                    val conference = parent.ongoingConferences.filter {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                            character
                        )
                    }.values.first()
                    //----------------------------------------------------------------------------------Move to the conference
                    if (place != conference.place) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = conference.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else {
                        routines.add(Routine("attendConference", routines[0].priority + 10).also {
                            it.intVariables["time"] = conference.time
                        })//Add a routine with higher priority.
                        joinConference(parent, character, place).also {
                            it.meetingName = parent.ongoingConferences.filter {
                                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                    character
                                )
                            }.keys.first()
                            return it
                        }
                    }
                    //----------------------------------------------------------------------------------Move to the conference
                }
                 if  (parent.scheduledConferences.any { it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3 })//If a conference is soon
                {
                    val conf = parent.scheduledConferences.filter {
                        it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3
                    }.values.first()
                    //----------------------------------------------------------------------------------Move to the conference
                    if (place != conf.place) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = conf.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else {
                        //if this character is the leader, start the conference.
                        if (parent.parties[conf.involvedParty]!!.leader == character) {
                            routines.add(Routine("attendConference", routines[0].priority + 10).also {
                                it.intVariables["time"] = conf.time
                            })//Add a routine with higher priority.
                            startConference(parent, character, place).also { action ->
                                action.meetingName =
                                    parent.scheduledConferences.keys.first { parent.scheduledConferences[it] == conf }
                                return action
                            }
                        } else {
                            return wait(
                                parent,
                                character,
                                place
                            )//Wait for the conference to start.
                        }

                    }
                    //----------------------------------------------------------------------------------Move to the conference
                }
                //If missed a meeting
                if (parent.ongoingMeetings.any {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                            character
                        )
                    }) {
                    //----------------------------------------------------------------------------------Move to the meeting
                    val meeting = parent.ongoingMeetings.filter {
                        it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                            character
                        )
                    }.values.first()
                    if (place != meeting.place) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = meeting.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else {
                        routines.add(Routine("attendMeeting", routines[0].priority + 10).also {
                            it.intVariables["time"] = meeting.time
                        })//Add a routine with higher priority.
                        joinMeeting(parent, character, place).also {
                            it.meetingName = parent.ongoingMeetings.filter {
                                it.value.scheduledCharacters.contains(character) && !it.value.currentCharacters.contains(
                                    character
                                )
                            }.keys.first()
                            return it
                        }
                    }
                    //----------------------------------------------------------------------------------Move to the meeting
                }
                if (parent.scheduledMeetings.any { it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3 })//If a meeting is soon
                {
                    //----------------------------------------------------------------------------------Move to the meeting
                    val meeting = parent.scheduledMeetings.filter {
                        it.value.scheduledCharacters.contains(character) && it.value.time - parent.time in -1..3
                    }.values.first()
                    if (place != meeting.place) {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = meeting.place
                        })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else //If there is no meeting yet, create one. Concurrent meetings do not happen, as the meeting is created immediately only if there is no meeting in the place.
                        routines.add(Routine("attendMeeting", routines[0].priority + 10).also {
                            it.intVariables["time"] = meeting.time
                        })//Add a routine with higher priority.
                        startMeeting(parent, character, place).also { action ->
                            action.meetingName =
                                parent.scheduledMeetings.filter { it.value == meeting }.keys.first()
                            return action
                        }
                    //----------------------------------------------------------------------------------Move to the meeting
                }
                //If a place in the map is short of resources, transfer resources to it.
                parent.places.values.forEach fe@{ place1 ->
                    place1.apparatuses.forEach { apparatus ->
                        val res = GameEngine.isShortOfResources(apparatus, place1) //Type of resource that is short of.
                        if (res != "") {
                            //Find a place within my division with maximum res.
                            val resplace =
                                parent.places.values.filter {
                                    it.responsibleParty != "" && parent.parties[it.responsibleParty]!!.members.contains(
                                        character
                                    )
                                }
                                    .maxByOrNull { it.resources[res] ?: 0 }
                                    ?: return@fe
                            if (place != resplace.name) {
                                routines.add(
                                    Routine(
                                        "move",
                                        routines[0].priority + 10
                                    ).also {
                                        it.variables["movePlace"] = resplace.name
                                    })//Add a move routine with higher priority.
                                return executeRoutine()
                            } else {
                                officialResourceTransfer(parent, character, place).also {
                                    it.what = res
                                    it.toWhere = place1.name
                                    it.amount = (resplace.resources[res] ?: 0) / 2
                                    return it
                                }
                            }
                        }
                    }

                }
                //If there is nothing above to do, move to a place that is the home of one of the parties of the character.
                //If already at home, wait.
                if (parent.parties.values.any {party->party.home== place && party.members.contains(character) })
                {
                    return wait(parent, character, place)
                } else
                //Move to a place that is the home of one of the parties of the character.
                {
                    try {
                        routines.add(Routine("move", routines[0].priority + 10).also {
                            it.variables["movePlace"] = parent.places.values.filter {place->
                                parent.parties.values.any {party->party.home== place.name && party.members.contains(character) }
                            }.random().name
                        })//Add a move routine with higher priority.
                        return executeRoutine()

                    } catch (e: Exception) {
                        println("Warning: No place to commute found for $character.")
                        return wait(parent, character, place)
                    }

                }
            }
            "attendMeeting" ->{
                //If the meeting is over, leave the routine.
                if(parent.ongoingMeetings.none { it.value.currentCharacters.contains(character) })
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                //If an hour has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
                if(routines[0].intVariables["time"]!!+1<=parent.time)
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                return wait(parent, character, place)
                //TODO: do something in the meeting. Leave the meeting if nothing to do.
            }
            "attendConference" ->{

                //If the conference is over, leave the routine.
                if(parent.ongoingConferences.none { it.value.currentCharacters.contains(character) })
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                val conf = parent.ongoingConferences.filter { it.value.currentCharacters.contains(character) }.values.first()
                //If an hour has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
                if(routines[0].intVariables["time"]!!+2<=parent.time)
                {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                }
                when (conf.subject){
                    "budgetProposal"->{
                        //If leader, propose budget
                        if(parent.parties[conf.involvedParty]!!.leader==character)
                        {
                            if(!parent.isBudgetProposed)
                            {
                                return budgetProposal(parent, character, place)
                            }
                            else //If budget is proposed, leave the conference.
                            {
                                routines.removeAt(0)//Remove the current routine.
                                return executeRoutine()
                            }
                        }
                        else
                        {
                            //If not leader, wait for the budget to be proposed.
                            if(!parent.isBudgetProposed)
                            {
                                return wait(parent, character, place)
                            }
                            else
                            {
                                routines.removeAt(0)//Remove the current routine.
                                return executeRoutine()
                            }
                        }
                    }
                }
                return wait(parent, character, place)
                //TODO: do something in the meeting. Leave the meeting if nothing to do.

            }

            "findCharacter" -> {
                //Stop if the character is at the same place
                if (place == parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name) {
                    routines.removeAt(0)//Remove the current routine.
                    return executeRoutine()
                } else

                //Move to findCharacter if the character is not at home
                {
                    if(parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name=="home")
                        return wait(parent, character, place)

                    routines.add(Routine("move", routines[0].priority + 10).also {
                        it.variables["movePlace"] =
                            parent.places.values.find { it.characters.contains(routines[0].variables["character"]) }!!.name
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                }


            }

            "buy" -> {
                //Try to trade for resources
                //Select a character to trade with, based on the information known to the character.
                val tradeCharacter: String
                val info = parent.informations.values.filter {
                    it.type == "resource" && it.tgtCharacter != "" && it.tgtCharacter != character && it.tgtResource == routines[0].variables["wantedResource"] && it.amount > 10 && it.knownTo.contains(
                        character
                    )
                }
                tradeCharacter = if (info.isNotEmpty()) {//If this character knows a character with the resource
                    info.random().tgtCharacter
                } else
                    parent.characters.keys.filter { it != character }.random()

                //FindCharacter
                // if the character is not in the same place.
                if (place != parent.places.values.find { it.characters.contains(tradeCharacter) }!!.name) {
                    routines.add(Routine("findCharacter", routines[0].priority + 10).also {
                        it.variables["character"] = tradeCharacter
                    })//Add a move routine with higher priority.
                    return executeRoutine()
                } else {
                    //If the character is in the same place, start a conversation first

                    if (parent.ongoingMeetings.none {
                            it.value.currentCharacters.containsAll(
                                listOf(
                                    character,
                                    tradeCharacter
                                )
                            )
                        }) {
                        routines.add(
                            Routine(
                                "talkToCharacter",
                                routines[0].priority + 10
                            ).also {
                                it.variables["character"] = tradeCharacter
                            })//Add a move routine with higher priority.
                        return executeRoutine()
                    } else {
                        //if the character is in the same meeting, trade for the resource

                        trade(parent, character, place).also { trade ->
                            trade.who = tradeCharacter
                            trade.item2 = routines[0].variables["wantedResource"]!!
                            trade.amount2 = parent.characters[tradeCharacter]!!.reliants.size + 1
                            //Give away unwanted resources
                            trade.item =
                                parent.characters[character]!!.resources.keys.filter { it != routines[0].variables["wantedResource"]!! }
                                    .random()
                            trade.amount = parent.characters[character]!!.resources[trade.item] ?: 0
                            //Give away information they want
                            trade.info = parent.informations.values.filter {
                                it.tgtCharacter == tradeCharacter && it.knownTo.contains(character)
                            }.random()
                            //Give away actions they want
                            trade.onFinished = {
                                if (it)//Trade accepted
                                    routines.removeAt(0)//Remove the current routine.
                                else
                                    routines[0].variables["desperation"] =
                                        ((routines[0].variables["desperation"]?.toInt()
                                            ?: 0) + 1).toString() //Increase desperation and try again.
                            }
                            return trade
                        }
                    }
                }
            }

        }
        return wait(parent, character, place)

    }


    private fun whenIdle() {
        //When work hours, work
        if (parent.hour in 8..18) {
            routines.add(Routine("work", 0))
            return
        }
        else
        //When not work hours, rest
            routines.add(Routine("rest", 0))
    }

    @Serializable
    class Routine(val name: String, val priority: Int) {
        val variables: HashMap<String, String> = hashMapOf()
        val intVariables: HashMap<String, Int> = hashMapOf()
    }





    //TODO: value may be affected by power dynamics.
    fun itemValue(item:String):Double{
        return when(item){
            //Value of ration and water is based on the current need of the character.
            "ration"->5.0*(parent.characters[character]!!.reliants.size+1.0) / ((parent.characters[character]!!.resources["ration"]?:0)+1.0)
            "water"->(parent.characters[character]!!.reliants.size+1.0) / ((parent.characters[character]!!.resources["water"]?:0)+1.0)
            "hydrogen"->1.0
            "organics"->5.0
            "lightMetal"->1.0
            "heavyMetal"->1.0
            "rareMetal"->5.0
            "silicon"->1.0
            "plastic"->10.0
            "glass"->1.0
            "ceramic"->1.0
            "diamond"->3.0
            "helium"->1.0
            "glassClothes"->1.0
            "cottonClothes"->10.0

            else->0.0
        }

    }
    fun actionValue(action:Command):Double{
        //TODO: the value of the action should be calculated based on the expected outcome.
        //TODO: Action to remove rivals is more valuable.
        //TODO: Action to acquire resources is more valuable.

        //Action to repair the character's apparatus is more valuable.
        if(action.action=="repair" && parent.parties[parent.places[action.place]!!.responsibleParty]?.members?.contains(character)==true)
        {
            val urgency = 100.0 - parent.places[action.place]!!.apparatuses.sumOf { it.durability } / parent.places[action.place]!!.apparatuses.size
            return urgency
        }

        return 1.0
    }
    fun infoValue(info:Information):Double{
        //Known information is less valuable.
        if(info.knownTo.contains(character))
            return 0.0
        //Information about the character itself is more valuable.
        if(info.tgtCharacter==character)
            return 2.0
        //Information about the character's party is more valuable.
        if(parent.parties[info.tgtParty]?.members?.contains(character) == true)
            return 2.0
        //Information about valuable resource is more valuable.
        if(info.type=="resource")
            return itemValue(info.tgtResource) * info.amount
        //UnofficialTransfer is more valuable if it is not known to the other character.
        if(info.type=="action" && info.action=="unofficialResourceTransfer" && !info.knownTo.contains(character))
            return 10.0

        return 1.0
    }
}