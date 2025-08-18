package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.*
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class NewAgenda(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
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
        parent.setMutuality(sbjCharacter, sbjCharacter, deltaWill())
        extracted(effectivity, meeting, agenda, sbjCharacter, parent)
    }


    override fun isValid(): Boolean {
        //People will be more interested in agendas related to their interest. However, this is handled in NPC class.
        val mt = parent.characters[sbjCharacter]!!.currentMeeting!!
        when (agenda.type) {
            AgendaType.PROOF_OF_WORK -> return mt.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE || mt.type == Meeting.MeetingType.CABINET_DAILY_CONFERENCE //TODO: how do we handle command issued?
            //You have to choose which command you are responding to. The character who issued the command must be present in the meeting.
            //Other people may add supporting or disapproving information.
            AgendaType.BUDGET_PROPOSAL -> return mt.involvedParty == "cabinet" && !parent.isBudgetProposed
            //TODO: this is done by the mandatory cabinet voting.
            AgendaType.BUDGET_RESOLUTION -> return mt.involvedParty == "triumvirate" && !parent.isBudgetResolved
            //TODO: this is done by the mandatory triumvirate voting.
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
                }

                AgendaType.BUDGET_RESOLUTION -> {
                }

                AgendaType.PRAISE -> {
                    parent.setMutuality(
                        agenda.subjectParams["character"]!!,
                        sbjCharacter,
                        5.0 * effectivity,
                        "praise;$sbjCharacter"
                    )
                }

                AgendaType.DENOUNCE -> {
                    parent.setMutuality(
                        agenda.subjectParams["character"]!!,
                        sbjCharacter,
                        -7.0 * effectivity,
                        "denounce;$sbjCharacter"
                    )
                }

                AgendaType.PRAISE_PARTY -> {
                    meeting.involvedParty?.run {
                        parent.setPartyMutuality(
                            this,
                            agenda.subjectParams["party"]!!,
                            3.0 * effectivity,
                            "praiseParty;$sbjCharacter"
                        )
                    }
                }

                AgendaType.DENOUNCE_PARTY -> {
                    meeting.involvedParty?.run {
                        parent.setPartyMutuality(
                            this,
                            agenda.subjectParams["party"]!!,
                            -5.0 * effectivity,
                            "denounceParty;$sbjCharacter"
                        )
                    }
                    //Increase party integrity
                    meeting.involvedParty?.run {
                        parent.setPartyMutuality(
                            this,
                            this,
                            3.0 * effectivity,
                            "denounceEnemyParty;$sbjCharacter"
                        )
                    }
                }

                AgendaType.NOMINATE -> {
                    parent.setMutuality(
                        agenda.subjectParams["character"]!!,
                        sbjCharacter,
                        20.0 * effectivity,
                        "nominate;$sbjCharacter"
                    )
                }

                AgendaType.FIRE_MANAGER -> {
                    parent.setMutuality(
                        agenda.subjectParams["character"]!!,
                        sbjCharacter,
                        -20.0 * effectivity,
                        "fireManager;$sbjCharacter"
                    )
                }

                //request is not executed until the end of the meeting. Check Meeting.kt
                else -> {
                }
            }
        }
    }

}