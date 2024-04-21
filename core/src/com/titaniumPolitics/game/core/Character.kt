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

    //The character's preference of this information spreading. -1 is hate, 0 is neutral, 1 is like.
    //TODO: preference depend on the trait of the character. When other characters use this function, the trait must be not reflected since they don't know the trait.
    fun infoPreference(info: Information): Double
    {
        //Is the information about the character itself?
        if (info.tgtCharacter == name)
        {
            //The character don't like information about its wrongdoings.
            //Stole resource
            if (info.type == "action" && info.action == "UnofficialResourceTransfer")
                return -1.0
            //Stayed in home during work hours
            //Did their job well
            if (info.type == "action" && info.action == "NewAgenda")
                return 0.5
            if (info.type == "action" && info.action == "AddInfo")
                return 0.5
            if (info.type == "action" && info.action == "OfficialResourceTransfer")
                return 0.5
            if (info.type == "action" && info.action == "InvestigateAccidentScene")
                return 1.0
            if (info.type == "action" && info.action == "ClearAccidentScene")
                return 1.0

            //Depends on their party
            parent.parties.filter { it.value.members.contains(name) }.forEach { party ->
                when (party.key)
                {
                    "infrastructure" ->
                    {
                        if (info.type == "action" && info.action == "Repair")
                            return 1.0
                    }
                }
            }

        }
        //If the information is about some other people, the character's preference depends on their relationship with the target.
        //The target character's preference is reflected.
        else
        {
            if (parent.getMutuality(
                    name,
                    info.tgtCharacter
                ) > (ReadOnly.const("mutualityMin") + ReadOnly.const("mutualityMax")) / 2
            )
                return parent.characters[info.tgtCharacter]!!.infoPreference(info)
            else
                return -parent.characters[info.tgtCharacter]!!.infoPreference(info)

        }


        //Otherwise, the character is neutral to the information.
        return 0.0
    }

    @Deprecated("This function has lost its purpose with the removal of trade.")
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