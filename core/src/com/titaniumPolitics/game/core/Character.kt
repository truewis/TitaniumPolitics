package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
class Character : GameStateElement()
{
    override val name: String
        get() = parent.characters.filter { it.value == this }.keys.first()
    var alive = true
    var trait = hashSetOf<String>()
    var resources = hashMapOf<String, Int>()
    var preparedInfoKeys = arrayListOf<String>()//Information that can be presented in meetings.
    var health = 0
        set(value)
        {
            field = if (value < 100) value else 100//Max health is 100.
        }
    var hunger = 0
        set(value)
        {
            field = when
            {
                value < 0 -> 0
                value > 100 -> 100
                else -> value
            }//Max hunger is 100.
        }
    var thirst = 0
        set(value)
        {
            field = when
            {
                value < 0 -> 0
                value > 100 -> 100
                else -> value
            }//Max thirst is 100.
        }
    var reliants =
        hashSetOf<String>() //Characters that this character is responsible for. If they die, this character will be sad. They consume water and ration every day.
    val scheduledMeetings: HashMap<String, Meeting>
        get() = parent.scheduledMeetings.filter { it.value.scheduledCharacters.contains(name) } as HashMap<String, Meeting>
    var livingBy = ""
    var frozen = 0

    val place
        get() = parent.places.values.first { it.characters.contains(name) }

    val currentMeeting
        get() = parent.ongoingMeetings.values.firstOrNull { it.currentCharacters.contains(name) }
            ?: parent.ongoingConferences.values.firstOrNull { it.currentCharacters.contains(name) }

    val history = hashMapOf<Int, String>()

    //TODO: value may be affected by power dynamics.
    fun itemValue(item: String): Double
    {
        return when (item)
        {
            //Value of ration and water is based on the current need of the character.
            "ration" -> 5.0 * (reliants.size + 1.0) / ((resources["ration"] ?: 0) + 1.0)
            "water" -> (reliants.size + 1.0) / ((resources["water"] ?: 0) + 1.0)
            "hydrogen" -> 1.0
            "organics" -> 5.0
            "lightMetal" -> 1.0
            "heavyMetal" -> 1.0
            "rareMetal" -> 5.0
            "silicon" -> 1.0
            "plastic" -> 10.0
            "glass" -> 1.0
            "ceramic" -> 1.0
            "diamond" -> 3.0
            "helium" -> 1.0
            "glassClothes" -> 1.0
            "cottonClothes" -> 10.0

            else -> 0.0
        }

    }

    fun actionValue(action: Request): Double
    {
        //TODO: the value of the action should be calculated based on the expected outcome.
        //TODO: Action to remove rivals is more valuable.
        //TODO: Action to acquire resources is more valuable.

        //Action to repair the character's apparatus is more valuable.
        if (action.action.javaClass.simpleName == "repair" && parent.parties[parent.places[action.place]!!.responsibleParty]?.members?.contains(
                name
            ) == true
        )
        {
            val urgency =
                100.0 - parent.places[action.place]!!.apparatuses.sumOf { it.durability } / parent.places[action.place]!!.apparatuses.size
            return urgency
        }

        return 1.0
    }

    fun infoValue(info: Information): Double
    {
        //Known information is less valuable.
        if (info.knownTo.contains(name))
            return 0.0
        //Information about the character itself is more valuable.
        if (info.tgtCharacter == name)
            return 2.0
        //Information about the character's party is more valuable.
        if (parent.parties[info.tgtParty]?.members?.contains(name) == true)
            return 2.0
        //Information about valuable resource is more valuable.
        if (info.type == "resource")
            return info.resources.keys.sumOf { itemValue(it) * info.resources[it]!! }
        //UnofficialTransfer is more valuable if it is not known to the other character.
        if (info.type == "action" && info.action == "unofficialResourceTransfer" && !info.knownTo.contains(name))
            return 10.0

        return 1.0
    }

}