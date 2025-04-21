package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Character : GameStateElement()
{
    var will: Double
        get() = parent.getMutuality(name)
        set(value)
        {
            parent.setMutuality(name, delta = value - will)
        }
    override val name: String
        get() = parent.characters.filter { it.value == this }.keys.first()
    var alive = true
    var trait = hashSetOf<String>()
    var resources = Resources()
    var preparedInfoKeys =
        arrayListOf<String>()//Information that can be presented in meetings. Note that preparing the information prevents it from expiring.
    var health = .0
        set(value)
        {
            field = if (value < const("HealthMax")) value else const("HealthMax")//Max health is 100.
        }
    var hunger = .0
        set(value)
        {
            field = when
            {
                value < .0 -> .0
                value > const("HungerMax") -> const("HungerMax")
                else -> value
            }//Max hunger is 100.
        }
    var thirst = .0
        set(value)
        {
            field = when
            {
                value < .0 -> .0
                value > const("ThirstMax") -> const("ThirstMax")
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
    val party
        get() = parent.parties.values.find { it.members.contains(name) }
    var assistants =
        hashSetOf<String>()//TODO: Think about utilizing assistants. How do we pay them? How is it different from requests between free individuals?

    //They improve resource transfer speed and prepare information speed.
    var mercenaries = hashSetOf<String>()

    val history = hashMapOf<Int, String>()
    val finishedRequests =
        HashSet<String>() //Requests that this character thinks are finished. The recipient of the request may not be aware of this yet.

    fun hireCost(): Double
    {
        return 10000.0 / parent.idlePop
    }

    //Item value is normalized to mutuality.
    fun itemValue(resources: Resources): Double
    {
        var sum = .0
        resources.forEach { (key, value) -> sum += itemValue(key) * value }
        return sum

    }

    //Item value is normalized to mutuality.
    //TODO: value may be affected by power dynamics.
    fun itemValue(item: String): Double
    {
        val ret = when (item)
        {
            //Value of ration and water is based on the current need of the character.
            "ration" -> 5.0e-2 * (reliants.size + 1.0) / (resources["ration"] + 1.0)
            "water" -> (reliants.size + 1.0) / (resources["water"] + 1.0)
            "hydrogen" -> 1.0e-2
            "organics" -> 5.0e-2
            "lightMetal" -> 1.0e-2
            "heavyMetal" -> 1.0e-2
            "rareMetal" -> 5.0e-2
            "silicon" -> 1.0e-2
            "plastic" -> 10.0e-2
            "glass" -> 1.0e-2
            "ceramic" -> 1.0e-2
            "diamond" -> 3.0e-2
            "helium" -> 1.0e-2
            "glassClothes" -> 1.0e-2
            "cottonClothes" -> 10.0e-2

            else -> 0.0
        }
        return ret * const("mutualityMax")

    }

    fun actionValue(action: GameAction): Double
    {
        //TODO: the value of the action should be calculated based on the expected outcome.
        //TODO: Action to remove rivals is more valuable.
        //TODO: Action to acquire resources is more valuable.

        //Action to repair the character's apparatus is more valuable.
        if (action.javaClass.simpleName == "repair" && parent.parties[parent.places[action.tgtPlace]!!.responsibleParty]?.members?.contains(
                name
            ) == true
        )
        {
            val urgency =
                100.0 - parent.places[action.tgtPlace]!!.apparatuses.sumOf { it.durability } / parent.places[action.tgtPlace]!!.apparatuses.size
            return urgency
        }

        return 1.0
    }

    //The character's preference of this information spreading. -1 is hate, 0 is neutral, 1 is like.
    //TODO: preference depend on the trait of the character. When other characters use this function, the trait must be not reflected since they don't know the trait.
    fun infoPreference(info: Information): Double
    {
        var ret = .0
        //Is the information about the character itself?
        if (info.tgtCharacter == name)
        {
            //The character don't like information about its wrongdoings.
            //Stole resource
            if (info.type == InformationType.ACTION && info.action!!.javaClass.simpleName == "UnofficialResourceTransfer")
                ret = -1.0
            //Stayed in home during work hours
            //Did their job well
            if (info.type == InformationType.ACTION && info.action!!.javaClass.simpleName == "NewAgenda")
                ret = 0.5
            if (info.type == InformationType.ACTION && info.action!!.javaClass.simpleName == "AddInfo")
                ret = 0.5
            if (info.type == InformationType.ACTION && info.action!!.javaClass.simpleName == "OfficialResourceTransfer")
                ret = 0.5
            if (info.type == InformationType.ACTION && info.action!!.javaClass.simpleName == "InvestigateAccidentScene")
                ret = 1.0
            if (info.type == InformationType.ACTION && info.action!!.javaClass.simpleName == "ClearAccidentScene")
                ret = 1.0

            //Depends on their party
            parent.parties.filter { it.value.members.contains(name) }.forEach { party ->
                when (party.key)
                {
                    "infrastructure" ->
                    {
                        if (info.type == InformationType.ACTION && info.action!!.javaClass.simpleName == "Repair")
                            ret = 1.0
                    }
                }
            }

        } else
        {
            //Accidents are always interesting.
            if (info.type == InformationType.CASUALTY)
                ret = 2.0
            else
            {

                //Otherwise, if the information is about some other people, the character's preference depends on their relationship with the target.
                //The target character's preference is reflected.
                if (parent.getMutuality(
                        name,
                        info.tgtCharacter
                    ) > (const("mutualityMin") + const("mutualityMax")) / 2
                )
                    return parent.characters[info.tgtCharacter]!!.infoPreference(info)
                else
                    return -parent.characters[info.tgtCharacter]!!.infoPreference(info)
            }

        }


        //Otherwise, the character is neutral to the information.
        return ret * const("mutualityMax")
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
        if (info.type == InformationType.RESOURCES)
            return info.resources.keys.sumOf { itemValue(it) * info.resources[it]!! }
        //UnofficialTransfer is more valuable if it is not known to the other character.
        if (info.type == InformationType.ACTION && info.action!!.javaClass.simpleName == "unofficialResourceTransfer" && !info.knownTo.contains(
                name
            )
        )
            return 10.0

        return 1.0
    }

}