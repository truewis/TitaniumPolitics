package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.*
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
        val effectivity = meeting.currentAttention / 100.0 * sbjCharObj.will / ReadOnly.const("mutualityMax")

        //Attention is consumed.
        meeting.currentAttention = max(
            meeting.currentAttention + (10 * sbjCharObj.will / ReadOnly.const("mutualityMax")).toInt() - 20,
            0
        )
        super.execute()
        //affect mutuality based on the agenda.
        parent.setMutuality(sbjCharacter, sbjCharacter, deltaWill(), "newAgenda")
        extracted(effectivity, meeting, agenda, sbjCharacter, parent)
    }


    override fun isValid(): Boolean {
        //People will be more interested in agendas related to their interest. However, this is handled in NPC class.
        val mt = parent.characters[sbjCharacter]!!.currentMeeting!!
        when (agenda.type) {
            AgendaType.PROOF_OF_WORK -> return mt.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE || mt.type == Meeting.MeetingType.CABINET_DAILY_CONFERENCE //TODO: how do we handle command issued?
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

            AgendaType.PRAISE -> return true

            AgendaType.DENOUNCE -> return true
            AgendaType.PRAISE_PARTY -> return true

            AgendaType.DENOUNCE_PARTY -> return true
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
            //TODO: impeach, fire division leader, etc. This is done by the cabinet meeting.
            //TODO: Also update NewAgendaUI.kt


        }
        return false
    }

    override fun deltaWill(): Double {
        val mt = parent.characters[sbjCharacter]!!.currentMeeting!!
        when (agenda.type) {
            AgendaType.PRAISE -> return parent.getMutNorm(
                sbjCharacter,
                agenda.subjectParams["character"]!!
            ) * 5.0 * sbjCharObj.stats.eScale

            AgendaType.DENOUNCE -> return parent.getMutNorm(
                sbjCharacter,
                agenda.subjectParams["character"]!!
            ) * -7.0 * sbjCharObj.stats.pScale

            AgendaType.NOMINATE -> return parent.getMutNorm(
                sbjCharacter,
                agenda.subjectParams["character"]!!
            ) * 20.0 * sbjCharObj.stats.pScale

            AgendaType.PRAISE_PARTY -> return parent.getPartyMutNorm(
                mt.involvedParty,
                agenda.subjectParams["party"]!!
            ) * 3.0 * sbjCharObj.stats.eScale

            AgendaType.DENOUNCE_PARTY -> return parent.getPartyMutNorm(
                mt.involvedParty,
                agenda.subjectParams["party"]!!
            ) * -5.0 * sbjCharObj.stats.pScale

            else -> return .0
        }
    }

    companion object {

        fun extracted(
            effectivity: Double,
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
                    parent.setMutuality(
                        agenda.subjectParams["character"]!!,
                        sbjCharacter,
                        5.0 * effectivity,
                        "Praise;$sbjCharacter"
                    )
                }

                AgendaType.DENOUNCE -> {
                    parent.setMutuality(
                        agenda.subjectParams["character"]!!,
                        sbjCharacter,
                        -7.0 * effectivity,
                        "Denounce;$sbjCharacter"
                    )
                }

                AgendaType.PRAISE_PARTY -> {
                    meeting.involvedParty?.run {
                        parent.setPartyMutuality(
                            this,
                            agenda.subjectParams["party"]!!,
                            3.0 * effectivity,
                            "PraiseParty;$sbjCharacter"
                        )
                    }
                }

                AgendaType.DENOUNCE_PARTY -> {
                    meeting.involvedParty?.run {
                        parent.setPartyMutuality(
                            this,
                            agenda.subjectParams["party"]!!,
                            -5.0 * effectivity,
                            "DenounceParty;$sbjCharacter"
                        )
                    }
                    //Increase party integrity
                    meeting.involvedParty?.run {
                        parent.setPartyMutuality(
                            this,
                            this,
                            3.0 * effectivity,
                            "DenounceEnemyParty;$sbjCharacter"
                        )
                    }
                }

                AgendaType.NOMINATE -> {
                    parent.setMutuality(
                        agenda.subjectParams["character"]!!,
                        sbjCharacter,
                        20.0 * effectivity,
                        "Nominate;$sbjCharacter"
                    )
                }

                AgendaType.FIRE_MANAGER -> {
                    parent.setMutuality(
                        agenda.subjectParams["character"]!!,
                        sbjCharacter,
                        -20.0 * effectivity,
                        "FireManager;$sbjCharacter"
                    )
                    val manager = agenda.subjectParams["character"] as String

                    //If the manager is a Director of a place, fire them.
                    parent.places.filter { it.value.manager == manager }.forEach { place ->
                        Logger.write(
                            "The manager $manager of the place ${place.value.name} is fired.",
                            Logger.LogLevel.INFO
                        )
                        place.value.manager = null //Remove the manager from the place.
                    }
                    //Fire manager from the workplace party.
                    parent.parties.filter { (key, value) -> value.type == "workplace" && manager in value.members }
                        .forEach { (key, value) ->
                            Logger.write(
                                "The manager $manager of the workplace party ${value.name} is fired.",
                                Logger.LogLevel.INFO
                            )
                            value.members.remove(manager)
                            if (value.leader == manager)
                                value.leader = null //If the manager was the leader, set the leader to null.
                        }

                }

                else -> {
                }
            }
        }
    }

}