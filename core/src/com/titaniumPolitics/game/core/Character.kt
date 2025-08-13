package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.Arrest
import com.titaniumPolitics.game.core.gameActions.BlockAccess
import com.titaniumPolitics.game.core.gameActions.ClearAccidentScene
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.InvestigateAccidentScene
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.core.gameActions.NewAgenda
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class Character : GameStateElement() {
    @Serializable
    enum class Type {
        DIRECTOR, EMPLOYEE, ANON
    }

    var will: Double
        get() = parent.getMutuality(name)
        set(value) {
            parent.setMutuality(name, delta = value - will)
        }
    private var _name: String? = null
    override val name: String
        get() = _name ?: parent.characters.filter { it.value == this }.keys.first().also { _name = it }
    var alive = true
    var trait = hashSetOf<String>()
    var type = Type.DIRECTOR

    var resources: Resources
        get() =
            parent.places["home_$name"]!!.resources
        set(value) {
            parent.places["home_$name"]!!.resources = value
        }

    var preparedInfoKeys =
        arrayListOf<String>()//Information that can be presented in meetings. Note that preparing the information prevents it from expiring.

    var stats = Stat() //Stats of the character. Used to calculate the effectiveness of actions.

    var health = .0
        set(value) {
            field = if (value < const("HealthMax")) value else const("HealthMax")//Max health is 100.
            if (field < const("CriticalHealth") && (hunger > const("hungerThreshold") || thirst > const("thirstThreshold"))) {
                if (reliant > 1)
                    killReliant(max(reliant / 10, 1))
            }
        }
    var hunger = .0
        set(value) {
            field = when {
                value < .0 -> .0
                value > const("HungerMax") -> const("HungerMax")
                else -> value
            }//Max hunger is 100.
        }
    var thirst = .0
        set(value) {
            field = when {
                value < .0 -> .0
                value > const("ThirstMax") -> const("ThirstMax")
                else -> value
            }//Max thirst is 100.
        }
    var reliant =
        1 //Characters that this character is responsible for. If they die, this character will be sad. They consume water and ration every day. Always bigger or equal to 1
    val scheduledMeetings: HashMap<String, Meeting>
        get() = parent.scheduledMeetings.filter { it.value.scheduledCharacters.contains(name) } as HashMap<String, Meeting>
    var livingBy = ""
    var frozen = 0

    val place
        get() = parent.places.values.first { it.characters.contains(name) }

    val currentMeeting
        get() = parent.ongoingMeetings.values.firstOrNull { it.currentCharacters.contains(name) }
    val division
        get() = parent.parties.values.find { it.members.contains(name) && it.type == "division" }
    var assistants =
        hashSetOf<String>()//TODO: Think about utilizing assistants. How do we pay them? How is it different from requests between free individuals?

    //They improve resource transfer speed and prepare information speed.
    //You can hire specialists to write you various reports, which appears as separate UIs as well.
    var mercenaries = hashSetOf<String>()

    val history = arrayListOf<String>()
    val executedRequests =
        HashSet<String>() //Requests that this character thinks are finished. The recipient of the request may not be aware of this yet. This is handled in Request.refresh().

    fun hireCost(): Double {
        return 10000.0 / parent.idlePop
    }

    //Item value is normalized to mutuality.
    fun itemValue(resources: Resources): Double {
        var sum = .0
        resources.forEach { (key, value) -> sum += itemValue(key) * value }
        return sum

    }

    fun randomizeTraitAndStats() {
        //Randomly assign a trait to the character.
        val traits = listOf(
            "gourmand",
            "old",
            "young",
            "psychopath",
            "charismatic",
            "shy",
            "introvert",
            "extrovert",
            "lazy",
            "hardworking"
        )
        trait.add(traits.random())
        //Randomly assign stats to the character.
        stats = Stat(
            logos = (0..20).random(),
            ethos = (0..20).random(),
            pathos = (0..20).random(),
        )
    }

    fun killReliant(num: Int) {
        if (num == 0) return
        if (num >= reliant) throw Exception()
        reliant -= num
        parent.popChanged.forEach { it() }
        hunger = 0.0//This character ate the reliant.
        thirst = 0.0
        resources["corpse"] += num * 1.0
        Information(
            author = null,
            creationTime = parent.time,
            type = InformationType.CASUALTY,
            tgtPlace = place.name,
            auxParty = place.responsibleDivision,
            amount = num
        ).also {
            parent.addInformation(it) //cpy.publicity = 5
            it.knownTo += name
        }

    }

    fun itemValueModifier(item: String): Double {
        when (item) {
            "ration" -> return max(
                (reliant) / (resources["ration"] + 1.0),
                1.0
            )//1 kg of ration is enough for 1 people for a day.
            "water" -> max((reliant) / (resources["water"] + 1.0), 1.0)//1 kg of water is enough for 1 people for a day.
        }
        return 1.0 //TODO: Implement item value modifier based on the character's trait.
    }

    //Item value is normalized to mutuality.
    //TODO: value may be affected by power dynamics.
    fun itemValue(item: String): Double {
        val ret = parent.getMarketPrice(item)
        return ret * itemValueModifier(item)

    }

    fun actionValue(action: GameAction): Double {
        //TODO: the value of the action should be calculated based on the expected outcome.

        when (action) {
            is UnofficialResourceTransfer -> {
                //Action value of unofficial resource transfer from me is equal to the value of the resources transferred.
                if (action.sbjCharacter == name && action.fromHome)
                    return -itemValue(action.resources)
                if (action.toWhere == "home_$name")
                    return itemValue(action.resources)
            }

            is OfficialResourceTransfer -> {
                //Action value of official resource transfer depends on the division integrity.
                //for not, set it to 0.
                return .0
            }

            is Repair -> {
                //Fixing the apparatus where I am the manager is more valuable.
                //This scales with division integrity.
                val party = parent.parties.filter { name in it.value.members }.keys.firstOrNull() ?: return 0.0
                val factor = if (place.manager == name) 2.0 else 1.0
                val urgency =
                    1.0 - parent.places[action.tgtPlace]!!.apparatuses.sumOf { it.durability } / parent.places[action.tgtPlace]!!.apparatuses.size / 100.0
                if (parent.places[action.tgtPlace]!!.responsibleDivision == party) {
                    return urgency * parent.getPartyMutuality(party) * factor
                }
                //Otherwise, the action value is 0.
                return 0.0
            }

            is ClearAccidentScene -> {
                //Clearing the accident scene is more valuable if I am the manager of the place.
                if (parent.places[action.tgtPlace]!!.responsibleDivision == division?.name) {
                    val factor = if (place.manager == name) 2.0 else 1.0
                    return factor * parent.getPartyMutuality(division!!.name)
                }
                return 0.0
            }

            is InvestigateAccidentScene -> {
                //I hate someone investigating the accident scene where I am the manager.
                if (parent.places[action.tgtPlace]!!.responsibleDivision == division?.name) {
                    if (place.manager == name && action.sbjCharacter != name) {
                        return (const("mutualityMax") - parent.getPartyMutuality(division!!.name)) * 0.5
                    }
                }
                return 0.0
            }

            is NewAgenda -> {
                when (action.agenda.type) {
                    AgendaType.PROOF_OF_WORK -> return 0.0 //TODO()
                    AgendaType.NOMINATE -> {
                        //If it is someone else nominating me, like. If it is nominating someone else, dislike. Neutral if it is not my division.
                        if (action.agenda.subjectParams["character"] == name)
                            return 20.0
                        else if (action.agenda.subjectParams["party"] == division?.name)
                            return -10.0
                        return 0.0

                    }

                    AgendaType.REQUEST -> return 0.0 // Prevent nested request!
                    AgendaType.PRAISE -> {
                        //Based on the mutuality.
                        return parent.getMutNorm(name, action.agenda.subjectParams["character"]!!) * 5.0
                    }

                    AgendaType.DENOUNCE -> {
                        //Based on the mutuality.
                        return parent.getMutNorm(name, action.agenda.subjectParams["character"]!!) * -7.0
                    }

                    AgendaType.PRAISE_PARTY -> {
                        //Based on the party's mutuality.
                        return parent.getMutNorm(
                            name, parent.parties[action.agenda.subjectParams["party"]!!
                            ]!!.leader
                        ) * 3.0
                    }

                    AgendaType.DENOUNCE_PARTY -> {
                        //Based on the party's friendliness.
                        return parent.getMutNorm(
                            name, parent.parties[action.agenda.subjectParams["party"]!!
                            ]!!.leader
                        ) * -5.0
                    }

                    AgendaType.BUDGET_PROPOSAL -> return 0.0 // TODO()
                    AgendaType.BUDGET_RESOLUTION -> return 0.0 // TODO()
                    AgendaType.APPOINT_MEETING -> return 0.0// TODO()
                    AgendaType.FIRE_MANAGER -> {
                        //If firing me, heavy dislike.
                        if (action.agenda.subjectParams["character"] == name)
                            return -30.0
                        //Otherwise, if firing my friend, dislike.
                        else return parent.getMutNorm(name, action.agenda.subjectParams["character"]!!) * -20.0
                    }
                }
            }

            else -> {

            }
        }


        //TODO: Action to remove rivals is more valuable.

        return .0
    }

    //The character's preference of this information spreading. -1 is hate, 0 is neutral, 1 is like.
    //TODO: preference depend on the trait of the character. When other characters use this function, the trait must be not reflected since they don't know the trait.
    fun infoPreference(info: Information): Double {
        var ret = .0
        //Is the information about the character itself?
        if (info.tgtCharacter == name) {
            //The character don't like information about its wrongdoings.
            //Stole resource
            if (info.type == InformationType.ACTION && info.action is UnofficialResourceTransfer && !(info.action as UnofficialResourceTransfer).fromHome /*If not from any homes, it is probably stolen. We don't care about the destination.*/)
                ret = -1e-1 * stats.pScale
            //Stayed in home during work hours?
            //Did their job well
            if (info.type == InformationType.ACTION && info.action is NewAgenda)
                ret = 5e-2
            if (info.type == InformationType.ACTION && info.action is AddInfo)
                ret = 5e-2
            if (info.type == InformationType.ACTION && info.action is OfficialResourceTransfer)
                ret = 5e-2
            if (info.type == InformationType.ACTION && info.action is InvestigateAccidentScene)
                ret = 1e-1
            if (info.type == InformationType.ACTION && info.action is ClearAccidentScene)
                ret = 1e-1

            //Depends on their party
            parent.parties.filter { it.value.members.contains(name) }.forEach { party ->
                when (party.key) {
                    "infrastructure" -> {
                        if (info.type == InformationType.ACTION && info.action is Repair)
                            ret = 1e-1
                    }

                    "safety" -> {
                        if (info.type == InformationType.ACTION && info.action is BlockAccess)
                            ret = 1e-1
                        if (info.type == InformationType.ACTION && info.action is Arrest)
                            ret = 1e-1
                    }
                }
            }

        } else {
            //Accidents are always interesting.
            if (info.type == InformationType.CASUALTY)
                ret = 2e-1 * stats.lScale
            else {

                //Otherwise, if the information is about some other people, the character's preference depends on their relationship with the target.
                //The target character's preference is reflected.
                info.tgtCharacter?.run {
                    ret = parent.characters[this]!!.infoPreference(info) * parent.getMutNorm(
                        name,
                        this
                    ) * stats.eScale
                }

            }

            //I don't like unresolved requests that are given to me.
            if (info.type == InformationType.ACTION && (info.action is NewAgenda) && (info.action as NewAgenda).agenda.type == AgendaType.REQUEST
                && (info.action as NewAgenda).agenda.attachedRequest!!.issuedTo.contains(name) && !(info.action as NewAgenda).agenda.attachedRequest!!.completed
            ) {
                ret = -1e-1 * (1 - (info.action as NewAgenda).agenda.attachedRequest!!.issuedBy.sumOf {
                    parent.getMutNorm(
                        name,
                        it
                    )
                } / (info.action as NewAgenda).agenda.attachedRequest!!.issuedBy.size) * stats.pScale
                //If I hate the issuers, I hate this information even more. If I like the issuers, I don't hate this information as much.
            }

        }


        //Otherwise, the character is neutral to the information.
        return ret * const("mutualityMax")
    }

    @Deprecated("This function has lost its purpose with the removal of trade.")
    fun infoValue(info: Information): Double {
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