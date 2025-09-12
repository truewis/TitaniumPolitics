package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.AgendaType.*
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.get
import kotlin.math.max

@Serializable
data class NewAgenda(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    lateinit var agenda: MeetingAgenda

    override fun execute() {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        meeting.agendas.add(agenda)

        //Attention is consumed.
        meeting.currentAttention = max(
            meeting.currentAttention + (10 * sbjCharObj.will / ReadOnly.const("mutualityMax")).toInt() - 20,
            0
        )
        super.execute()
        agendaOneShotEffect(meeting, agenda, sbjCharacter, parent)
    }


    override fun isValid(): Boolean {
        //People will be more interested in agendas related to their interest. However, this is handled in NPC class.
        val mt = parent.characters[sbjCharacter]!!.currentMeeting!!
        if (!reason(mt.agendas.size < 4, "newAgenda-AgendaLimit"))
            return false //Idea Draft: A meeting can have at most 4 agendas.
        when (agenda.type) {
            AgendaType.PROOF_OF_WORK -> return agenda.attachedRequest != null && mt.agendas.none { oldAgenda -> oldAgenda.type == AgendaType.REQUEST && oldAgenda.attachedRequest == agenda.attachedRequest }
            //You have to choose which command you are responding to. The character who issued the command must be present in the meeting.
            //Other people may add supporting or disapproving information.
            AgendaType.BUDGET_PROPOSAL -> return mt.type == Meeting.MeetingType.BUDGET_PROPOSAL && with(parent) {
                ////////////////////Stationwide budget proposal//////////////////
                if (mt.involvedParty == "cabinet") {
                    val resourceTypes = setOf("water", "ration", "phosphorus")
                    return reason(
                        places["reservoirEast"]!!.resources["water"] >= agenda.attachedBudget!!.sum("water")
                                && places["farm"]!!.resources["ration"] >= agenda.attachedBudget!!.sum("ration")
                                && places["mainControlRoom"]!!.resources["phosphorus"] >= agenda.attachedBudget!!.sum("phosphorus"),
                        "newAgenda-BudgetProposal-resources"
                    )
                            &&
                            reason(
                                agenda.attachedBudget!!.value.values.none { resources -> resources.keys.any { resource -> resource !in resourceTypes } },
                                "newAgenda-BudgetProposal-key"
                            )
                }
                ////////////////////Division budget proposal//////////////////
                else {
                    val division = parties[mt.involvedParty]!!
                    val resourceTypes = setOf("water", "ration", "phosphorus")
                    return reason(
                        places[division.home]!!.resources.contains(
                            agenda.attachedBudget!!.sum()
                        ), "newAgenda-BudgetProposal-resources"
                    ) &&
                            reason(
                                agenda.attachedBudget!!.value.values.none { resources -> resources.keys.any { resource -> resource !in resourceTypes } },
                                "newAgenda-BudgetProposal-key"
                            )
                }
            }

            AgendaType.BUDGET_RESOLUTION -> return mt.type == Meeting.MeetingType.BUDGET_RESOLUTION && mt.agendas.none {
                it.type == AgendaType.BUDGET_RESOLUTION
            } //Only one budget resolution agenda can be proposed in a meeting.
                    && with(parent) {
                ////////////////////Stationwide budget resolution//////////////////
                if (mt.involvedParty == "triumvirate") {
                    val finalBudget =
                        parties["cabinet"]!!.proposedBudgets[agenda.subjectParams["whoseProposal"]!!]!!
                    return reason(
                        places["reservoirEast"]!!.resources["water"] >= finalBudget.sum("water")
                                && places["farm"]!!.resources["ration"] >= finalBudget.sum("ration")
                                && places["mainControlRoom"]!!.resources["phosphorus"] >= finalBudget.sum("phosphorus"),
                        "newAgenda-BudgetResolution-resources"
                    )
                }
                ////////////////////Division budget resolution//////////////////
                else {
                    val division = parties[mt.involvedParty]!!
                    val finalBudget = division.proposedBudgets[agenda.subjectParams["whoseProposal"]!!]!!
                    return reason(
                        places[division.home]!!.resources.contains(
                            finalBudget.sum()
                        ), "newAgenda-BudgetResolution-resources"
                    )
                }
            }

            //Can't praise or denounce the same character or party more than once in a meeting.
            AgendaType.PRAISE, AgendaType.DENOUNCE -> return reason(mt.agendas.none { oldAgenda ->
                oldAgenda.type in listOf(
                    AgendaType.PRAISE,
                    AgendaType.DENOUNCE
                ) && oldAgenda.subjectParams["character"] == agenda.subjectParams["character"]
            }, "newAgenda-RepeatPraiseDenounce")

            AgendaType.PRAISE_PARTY, AgendaType.DENOUNCE_PARTY -> return reason(mt.agendas.none { oldAgenda ->
                oldAgenda.type in listOf(
                    AgendaType.PRAISE_PARTY,
                    AgendaType.DENOUNCE_PARTY
                ) && oldAgenda.subjectParams["party"] == agenda.subjectParams["party"]
            }, "newAgenda-RepeatPraiseDenounce")

            AgendaType.REQUEST -> return agenda.attachedRequest != null && mt.currentCharacters.containsAll(agenda.attachedRequest!!.issuedTo) &&
                    agenda.attachedRequest!!.let {
                        it.issuedTo.intersect(
                            it.issuedBy
                        ).isEmpty()
                    }

            AgendaType.NOMINATE -> return mt.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && agenda.subjectParams["character"]!! in parent.parties[mt.involvedParty]!!.members
            //You can choose the person to request, and one of the actions that the person can do. The command is issued immediately, and other people can opt in.
            //The below actions are executed by the leader. Party members can request the leader to do these actions.
            //"workingHoursChange" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && parent.places[agenda.subjectParams["where"]]!!.responsibleParty == mt.involvedParty
            //"reassignWorkersToApparatus" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && parent.places[agenda.subjectParams["where"]]!!.responsibleParty == mt.involvedParty //TODO: check apparatus key.
            //"salary" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && !parent.parties[mt.involvedParty]!!.isSalaryPaid
            AgendaType.APPOINT_MEETING -> return true

            AgendaType.FIRE_MANAGER -> return mt.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE && parent.parties[mt.involvedParty]!!.leader == sbjCharacter &&
                    agenda.subjectParams["character"] in mt.currentCharacters
                    && sbjCharObj.division?.policies?.contains("jobSecurity") != true
            //TODO: impeach, fire division leader, etc. This is done by the cabinet meeting.
            //TODO: Also update NewAgendaUI.kt


        }
        return false
    }

    override fun deltaWill(): MutualityMatrix {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        val w = MutualityMatrix()
        when (agenda.type) {
            AgendaType.PRAISE -> w.addWill(
                sbjCharacter, parent.getMutNorm(
                    sbjCharacter,
                    agenda.subjectParams["character"]!!
                ) * 5.0 * sbjCharObj.stats.eScale, ""
            )

            AgendaType.DENOUNCE -> w.addWill(
                sbjCharacter, parent.getMutNorm(
                    sbjCharacter,
                    agenda.subjectParams["character"]!!
                ) * -7.0 * sbjCharObj.stats.pScale, ""
            )

            AgendaType.NOMINATE -> w.addWill(
                sbjCharacter, parent.getMutNorm(
                    sbjCharacter,
                    agenda.subjectParams["character"]!!
                ) * 20.0 * sbjCharObj.stats.pScale, ""
            )

            AgendaType.PRAISE_PARTY -> w.addWill(
                sbjCharacter, parent.getPartyMutNorm(
                    meeting.involvedParty,
                    agenda.subjectParams["party"]!!
                ) * 3.0 * sbjCharObj.stats.eScale, ""
            )

            AgendaType.DENOUNCE_PARTY -> w.addWill(
                sbjCharacter, parent.getPartyMutNorm(
                    meeting.involvedParty,
                    agenda.subjectParams["party"]!!
                ) * -5.0 * sbjCharObj.stats.pScale, ""
            )

            AgendaType.FIRE_MANAGER -> w.addWill(
                sbjCharacter, parent.getMutNorm(
                    sbjCharacter,
                    agenda.subjectParams["character"]!!
                ) * 10.0 * (sbjCharObj.stats.pScale) - 20.0 * sbjCharObj.stats.eScale //With high ethos, you feel compassion for the fired manager. With high pathos, you feel good about firing the manager you dislike.
                , ""
            )

            else -> {}
        }

        val effectivity = meeting.currentAttention / 100.0 * sbjCharObj.will / ReadOnly.const("mutualityMax")
        meeting.currentCharacters.filter { it != sbjCharacter }.forEach { listener ->
            affectListenerMutuality(effectivity, w, agenda, sbjCharacter, listener, parent)
        }
        return w
    }

    companion object {

        fun affectListenerMutuality(
            effectivity: Double,
            w: MutualityMatrix,
            agenda: MeetingAgenda,
            sbjCharacter: String,
            listener: String,
            parent: GameState
        ) {
            when (agenda.type) {
                PROOF_OF_WORK -> TODO()
                NOMINATE -> {
                    if (agenda.subjectParams["character"]!! == listener) {
                        w.addMutuality(
                            agenda.subjectParams["character"]!!,
                            sbjCharacter,
                            20.0 * effectivity,
                            "Nominate;$sbjCharacter"
                        )
                    }
                }

                REQUEST -> TODO()
                PRAISE -> {
                    if (agenda.subjectParams["character"]!! == listener) {
                        w.addMutuality(
                            agenda.subjectParams["character"]!!,
                            sbjCharacter,
                            5.0 * effectivity,
                            "Praise;$sbjCharacter"
                        )
                    }
                    w.addMutuality(
                        listener,
                        agenda.subjectParams["character"]!!,
                        1.0 * effectivity,
                        "PraiseByWitness;$sbjCharacter"
                    )
                    //Affect mutuality with the praiser based on how much the listener likes the praised.
                    w.addMutuality(
                        listener,
                        sbjCharacter,
                        1.0 * effectivity * parent.getMutNorm(listener, agenda.subjectParams["character"]!!),
                        "PraiseByWitness;$sbjCharacter"
                    )
                }

                DENOUNCE -> {
                    if (agenda.subjectParams["character"]!! == listener) {
                        w.addMutuality(
                            agenda.subjectParams["character"]!!,
                            sbjCharacter,
                            -7.0 * effectivity,
                            "Denounce;$sbjCharacter"
                        )
                    }
                    w.addMutuality(
                        listener,
                        agenda.subjectParams["character"]!!,
                        -2.0 * effectivity,
                        "DenounceByWitness;$sbjCharacter"
                    )
                    //Affect mutuality with the denouncer based on how much the listener likes the denounced.
                    w.addMutuality(
                        listener,
                        sbjCharacter,
                        -2.0 * effectivity * parent.getMutNorm(listener, agenda.subjectParams["character"]!!),
                        "DenounceByWitness;$sbjCharacter"
                    )
                }

                PRAISE_PARTY -> TODO()
                DENOUNCE_PARTY -> TODO()
                BUDGET_PROPOSAL -> TODO()
                BUDGET_RESOLUTION -> TODO()
                APPOINT_MEETING -> TODO()
                FIRE_MANAGER -> {
                    if (agenda.subjectParams["character"] == listener) {
                        w.addMutuality(
                            agenda.subjectParams["character"]!!,
                            sbjCharacter,
                            -20.0 * effectivity,
                            "FireManager;$sbjCharacter"
                        )
                    }
                }
            }
        }

        fun agendaOneShotEffect(
            meeting: Meeting,
            agenda: MeetingAgenda,
            sbjCharacter: String,
            parent: GameState
        ) {
            when (agenda.type) {
                AgendaType.PROOF_OF_WORK -> {
                }

                AgendaType.REQUEST -> {
                    agenda.attachedRequest!!.also { parent.requests[it.generateName()] = it }
                }

                AgendaType.BUDGET_PROPOSAL -> {
                    with(parent) {
                        ////////////////////Stationwide budget proposal//////////////////
                        if (meeting.involvedParty == "cabinet") {
                            //triumvirate and cabinet share the same budget.
                            parties["cabinet"]!!.isBudgetProposed = true
                            parties["triumvirate"]!!.isBudgetProposed = true
                            parties["cabinet"]!!.proposedBudgets[agenda.author] = agenda.attachedBudget!!
                            parties["triumvirate"]!!.proposedBudgets[agenda.author] = agenda.attachedBudget!!
                        }
                        ////////////////////Division budget proposal//////////////////
                        else {
                            val division = parties[meeting.involvedParty]!!
                            division.isBudgetProposed = true
                            division.proposedBudgets[agenda.author] = agenda.attachedBudget!!
                        }
                    }
                }

                AgendaType.BUDGET_RESOLUTION -> {
                    with(parent) {
                        ////////////////////Stationwide budget resolution//////////////////
                        if (meeting.involvedParty == "triumvirate") {
                            //triumvirate and cabinet share the same budget.
                            parties["cabinet"]!!.isBudgetResolved = true
                            parties["triumvirate"]!!.isBudgetResolved = true
                            val finalBudget =
                                parties["cabinet"]!!.proposedBudgets[agenda.subjectParams["whoseProposal"]!!]!!
                            parties["cabinet"]!!.proposedBudgets.clear()
                            parties["triumvirate"]!!.proposedBudgets.clear()
                            parties["cabinet"]!!.budget = finalBudget
                            parties["triumvirate"]!!.budget = finalBudget
                            //Distribute resources according to the budget plan.
                            places["reservoirEast"]!!.resources["water"] -= finalBudget.sum("water")
                            places["farm"]!!.resources["ration"] -= finalBudget.sum("ration")
                            places["mainControlRoom"]!!.resources["phosphorus"] -= finalBudget.sum("phosphorus")

                            finalBudget.value.forEach { budget ->
                                val guildHall = parties[budget.key]!!.home
                                places[guildHall]!!.resources.plusAssign(budget.value)
                            }
                        }
                        ////////////////////Division budget resolution//////////////////
                        else {
                            val division = parties[meeting.involvedParty]!!
                            val finalBudget = division.proposedBudgets[agenda.subjectParams["whoseProposal"]!!]!!
                            division.proposedBudgets.clear()
                            division.isBudgetResolved = true
                            division.budget = finalBudget
                            //Distribute resources according to the budget plan.
                            places[division.home]!!.resources -= finalBudget.sum()
                            finalBudget.value.forEach { budget ->

                                //Set workplace budget. Note that this budget only consists of a single entry.
                                parties[budget.key]!!.budget = Budget(hashMapOf(budget.key to budget.value))
                                val workplace = parties[budget.key]!!.home
                                places[workplace]!!.resources.plusAssign(budget.value)
                            }
                        }
                    }
                }

                AgendaType.PRAISE -> {
                    //Spread the information about the relation between the praiser and the praised.
                    Information(
                        author = null,
                        creationTime = parent.time,
                        type = InformationType.MUTUALITY,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        auxCharacter = agenda.subjectParams["character"]!!,
                        amount = parent.getMutuality(sbjCharacter, agenda.subjectParams["character"]!!)
                            .toInt() + (0..5).random()
                    ).also {
                        it.knownTo.addAll(meeting.currentCharacters)
                        parent.addInformation(it)
                    }
                }

                AgendaType.DENOUNCE -> {
                    //Spread the information about the relation between the denouncer and the denounced.
                    Information(
                        author = null,
                        creationTime = parent.time,
                        type = InformationType.MUTUALITY,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        auxCharacter = agenda.subjectParams["character"]!!,
                        amount = parent.getMutuality(sbjCharacter, agenda.subjectParams["character"]!!)
                            .toInt() - (0..7).random()
                    ).also {
                        it.knownTo.addAll(meeting.currentCharacters)
                        parent.addInformation(it)
                    }
                }

                AgendaType.PRAISE_PARTY -> {
                    //Spread the information about the relation between the praiser and the praised party.
                    Information(
                        author = null,
                        creationTime = parent.time,
                        type = InformationType.PARTY_MUTUALITY,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        auxParty = agenda.subjectParams["party"]!!,
                        amount = parent.getCharToPartyMutuality(sbjCharacter, agenda.subjectParams["party"]!!)
                            .toInt() + (0..3).random()
                    ).also {
                        it.knownTo.addAll(meeting.currentCharacters)
                        parent.addInformation(it)
                    }
                }

                AgendaType.DENOUNCE_PARTY -> {
                    //Spread the information about the relation between the denouncer and the denounced party.
                    Information(
                        author = null,
                        creationTime = parent.time,
                        type = InformationType.PARTY_MUTUALITY,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        auxParty = agenda.subjectParams["party"]!!,
                        amount = parent.getCharToPartyMutuality(sbjCharacter, agenda.subjectParams["party"]!!)
                            .toInt() - (0..5).random()
                    ).also {
                        it.knownTo.addAll(meeting.currentCharacters)
                        parent.addInformation(it)
                    }
                }

                AgendaType.NOMINATE -> {
                    //Nomination is handled in Meeting.kt
                    //Spread the information about the relation between the nominator and the nominee.
                    Information(
                        author = null,
                        creationTime = parent.time,
                        type = InformationType.MUTUALITY,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        auxCharacter = agenda.subjectParams["character"]!!,
                        amount = parent.getMutuality(sbjCharacter, agenda.subjectParams["character"]!!)
                            .toInt() + (0..20).random()
                    ).also {
                        it.knownTo.addAll(meeting.currentCharacters)
                        parent.addInformation(it)
                    }
                }

                AgendaType.FIRE_MANAGER -> {
                    val manager = agenda.subjectParams["character"] as String

                    //If the manager is in the division, fire them.
                    meeting.involvedParty.run {
                        val party = parent.parties[this]!!
                        if (party.type == Party.Type.DIVISION && manager in party.members) {
                            Logger.write(
                                "The manager $manager of the division ${party.name} is fired.",
                                Logger.LogLevel.INFO
                            )
                            party.removeMember(manager)
                        }
                    }
                    //Fire manager from the workplace parties in the division, too.
                    parent.parties.filter { (_, value) -> value.type == Party.Type.WORKPLACE && manager in value.members && value.workplace.responsibleDivision == meeting.involvedParty }
                        .forEach { (_, value) ->
                            Logger.write(
                                "The manager $manager of the workplace party ${value.name} is fired.",
                                Logger.LogLevel.INFO
                            )
                            value.removeMember(manager)
                        }

                    //Spread the information about the relation between the firer and the fired.
                    Information(
                        author = null,
                        creationTime = parent.time,
                        type = InformationType.MUTUALITY,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        auxCharacter = agenda.subjectParams["character"]!!,
                        amount = parent.getMutuality(sbjCharacter, agenda.subjectParams["character"]!!)
                            .toInt() - (0..20).random()
                    ).also {
                        it.knownTo.addAll(meeting.currentCharacters)
                        parent.addInformation(it)
                    }

                }

                else -> {
                }
            }
        }
    }

}