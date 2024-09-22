package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendMeetingRoutine : Routine(), IMeetingRoutine
{
    //Sometimes, this routine is created before joining the meeting. In that case, the action required to join the meeting is stored here.
    var actionDelegated: GameAction? = null

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        when (conf.type)
        {
            "triumvirateDailyConference" ->
            {

                //Budget is resolved through voting, which is not in the meeting.

            }


            "cabinetDailyConference" ->
            {

                //Budget is resolved through voting, which is not in the meeting.

            }


            "divisionDailyConference" ->
            {
                val party = gState.parties[conf.involvedParty]!!

                //If speaker, propose proof of work if nothing else is important.
                //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
                //Some information are more relevant than others.
                if (conf.agendas.any { it.type == AgendaType.PROOF_OF_WORK })
                {

                    //If we haven't tried this branch in the current routine
                    if (intVariables["try_support_proofOfWork"] != 1)
                    {
                        //If the agenda is already proposed, and we have a supporting information, support it.
                        intVariables["try_support_proofOfWork"] = 1
                        return (
                                SupportAgendaRoutine().apply {
                                    intVariables["agendaIndex"] =
                                        conf.agendas.indexOfFirst { it.type == AgendaType.PROOF_OF_WORK }
                                })//Add a routine, priority higher than work.
                    }
                }


                //If not division leader and salary is not paid, request salary.
                if (conf.currentSpeaker == name && !party.isSalaryPaid && party.leader != name)
                {
                    //Check if there is already a salary request.
                    if (conf.agendas.none { it.type == AgendaType.REQUEST && it.attachedRequest!!.action.javaClass.simpleName == "salary" })
                    {

                    } else //If the agenda already exists, support it.
                    {
                        //If we haven't tried this branch in the current routine
                        if (intVariables["try_support_salary"] != 1)
                        {
                            //If the agenda is already proposed, and we have a supporting information, support it.
                            intVariables["try_support_salary"] = 1
                            return SupportAgendaRoutine().apply {
                                intVariables["agendaIndex"] =
                                    conf.agendas.indexOfFirst { it.type == AgendaType.REQUEST && it.attachedRequest!!.action.javaClass.simpleName == "salary" }
                            }//Add a routine, priority higher than work.
                        }

                    }
                }
            }


            "divisionLeaderElection" ->
            {
                val party = gState.parties[conf.involvedParty]!!
                //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
                if (conf.currentSpeaker != name)
                {
                } else
                {
                    val nominee = gState.characters.keys.filter { it != name && party.members.contains(it) }
                        .maxByOrNull { gState.getMutuality(name, it) }!!
                    //Nominate the person with the highest mutuality, if not nominated yet.
                    //Note that nomination is only valid at the beginning of the conference.

                    if (conf.agendas.none { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee } && conf.time == gState.time)
                    {
                    }
                    //otherwise, support the nominee.
                    else
                    {
                        //If we haven't tried this branch in the current routine
                        if (intVariables["try_support_nomination"] != 1)
                        {
                            //If the agenda is already proposed, and we have a supporting information, support it.
                            intVariables["try_support_nomination"] = 1
                            return SupportAgendaRoutine().apply {
                                intVariables["agendaIndex"] =
                                    conf.agendas.indexOfFirst { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee }
                            }
                        }
                        //After you support the nominee, attack the other nominees.
                        val otherNominees =
                            gState.characters.keys.filter { it != name && it != nominee && conf.agendas.any { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee } }
                        if (otherNominees.isNotEmpty())
                        {
                            return (
                                    AttackAgendaRoutine().apply {
                                        intVariables["agendaIndex"] =
                                            conf.agendas.indexOfFirst { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee }
                                    })//Add a routine, priority higher than work.

                        }
                    }
                }
            }
        }
        return null
    }

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun execute(name: String, place: String): GameAction
    {
        if (actionDelegated != null)
        {
            val action = actionDelegated!!
            actionDelegated = null //The action is executed.
            return action
        }
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting!!
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name)
        {
            if (gState.getMutuality(
                    name,
                    conf.currentSpeaker
                ) > 50.0
            )
                return Wait(name, place)
            else
            {
                val action = Intercept(name, place)
                if (action.isValid())
                    return action
                return Wait(name, place)
            }
        }
        when (conf.type)
        {
            "triumvirateDailyConference" ->
            {

                //Budget is resolved through voting, which is not in the meeting.
            }


            "cabinetDailyConference" ->
            {

                //If budget is not proposed, propose it.
                //Budget is resolved through voting, which is not in the meeting.
            }


            "divisionDailyConference" ->
            {
                val party = gState.parties[conf.involvedParty]!!

                //If speaker, propose proof of work if nothing else is important.
                //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
                //Some information are more relevant than others.
                if (conf.agendas.none { it.type == AgendaType.PROOF_OF_WORK })
                {
                    return NewAgenda(name, place).also {
                        it.agenda = MeetingAgenda(AgendaType.PROOF_OF_WORK)
                    }
                }

                //If not division leader and salary is not paid, request salary.
                if (conf.currentSpeaker == name && !party.isSalaryPaid && party.leader != name)
                {
                    //Check if there is already a salary request.
                    if (conf.agendas.none { it.type == AgendaType.REQUEST && it.subjectParams["command"] != null })
                    {
                        //Fill in the agenda based on variables in the routine, resource and character.
                        val agenda = MeetingAgenda(AgendaType.REQUEST).apply {
                            attachedRequest = Request(
                                Salary(
                                    party.leader,
                                    tgtPlace = party.home
                                ).apply {
                                    //TODO: adjust the salary, it.resources.
                                }//Created a command to transfer the resource.
                                ,
                                issuedTo = hashSetOf(party.leader)
                            ).apply {
                                executeTime = gState.time
                                issuedBy = hashSetOf(name)
                            }
                        }
                        variables["subject"] = "" //The subject is resolved.
                        return NewAgenda(name, place).also {
                            it.agenda = agenda
                        }
                    }
                }
                //If division leader,
                if (gState.parties[conf.involvedParty]!!.leader == name)
                {
                    //Pay the salary if not paid yet.
                    if (!gState.parties[conf.involvedParty]!!.isSalaryPaid)
                    {
                        return Salary(name, place)
                    }
                    //Praise or criticize the division members, if there is any relevant information.
                    gState.parties[conf.involvedParty]!!.members.forEach { member ->
                        if (member != name && gState.informations.values.any {
                                it.tgtCharacter == member && it.knownTo.contains(
                                    name
                                )
                            })
                        {
                            //praise if the mutuality is high, criticize if the mutuality is low.
                            val mutuality = gState.getMutuality(name, member)
                            if (mutuality > 80)
                            {
                                return NewAgenda(name, place).also {
                                    it.agenda =
                                        MeetingAgenda(AgendaType.PRAISE, subjectParams = hashMapOf("who" to member))
                                }
                            } else if (mutuality < 20)
                            {
                                return NewAgenda(name, place).also {
                                    it.agenda =
                                        MeetingAgenda(
                                            AgendaType.DENOUNCE,
                                            subjectParams = hashMapOf("who" to member)
                                        )
                                }
                            }
                        }//TODO: there must be a cooldown, stored in party class.
                    }
                    //TODO: If it is not covered above, if the division is short of resources, share the information about the resource shortage.
                    //TODO: Criticize the common enemies of the division. It is determined by the party with the low mutuality with the division.
                    val enemyParty = gState.parties.values.filter { it.name != conf.involvedParty }
                        .minByOrNull { gState.getPartyMutuality(it.name, conf.involvedParty) }!!.name
                    //TODO: Criticize the leader if there is any relevant information.
//                                if (gState.parties[enemyParty]!!.leader != "")
//                                    if (gState.informations.values.any {
//                                            (it.tgtParty == enemyParty || it.tgtCharacter == gState.parties[enemyParty]!!.leader) && it.knownTo.contains(
//                                                name
//                                            )
//                                        })
//                                        InfoShare(name, place).also { action ->
//                                            action.what = gState.informations.values.filter {
//                                                (it.tgtParty == enemyParty || it.tgtCharacter == gState.parties[enemyParty]!!.leader) && it.knownTo.contains(
//                                                    name
//                                                )
//                                            }
//                                                .random().name//TODO: take the information that is most useful for criticizing.
//                                            action.application = "criticize"
//                                            action.who = hashSetOf(gState.parties[enemyParty]!!.leader)
//                                            return action
//                                        }
                    //Criticize the common enemy. It is determined by average individual mutuality.
                    val enemy = gState.characters.maxByOrNull { ch ->
                        gState.parties[conf.involvedParty]!!.members.sumOf { mem ->
                            gState.getMutuality(
                                mem,
                                ch.key
                            )
                        }
                    }
                    //TODO: request information about the commands issued today.
                }
                //TODO: If not division leader, Share information about what happened in the division today.
                if (gState.parties[conf.involvedParty]!!.leader != name)
                {
//                                gState.informations.filter {
//                                    it.value.tgtParty == conf.involvedParty && it.value.knownTo.contains(
//                                        name
//                                    ) && it.value.type == "action" && it.value.tgtTime in gState.time / 48..gState.time / 48 + 47
//                                }.forEach {
//                                    InfoShare(name, place).also { action ->
//                                        action.what = it.key
//                                        return action
//                                    }
//                                }
                }


            }


            "divisionLeaderElection" ->
            {
                val party = gState.parties[conf.involvedParty]!!
                //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.

                val nominee = gState.characters.keys.filter { it != name && party.members.contains(it) }
                    .maxByOrNull { gState.getMutuality(name, it) }!!
                //Nominate the person with the highest mutuality, if not nominated yet.
                //Note that nomination is only valid at the beginning of the conference.

                if (conf.agendas.none { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee } && conf.time == gState.time)
                {
                    return NewAgenda(name, place).also {
                        it.agenda =
                            MeetingAgenda(AgendaType.NOMINATE, subjectParams = hashMapOf("character" to nominee))
                    }
                }
                //otherwise, support the nominee.


            }

        }
        return if (conf.currentSpeaker == name)
        //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            EndSpeech(name, place)//TODO: pick next speaker based on deltaWill
        //If I'm not the speaker, wait.
        else Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.

    }

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun endCondition(name: String, place: String): Boolean
    {
        if (actionDelegated != null)//If there is still an action to be executed from the previous routine, do not leave the routine.
        {
            return false
        }
        val character = gState.characters[name]!!
        //If the conference is over, leave the routine.
        if (character.currentMeeting == null)
        {
            return true
        }
        val conf =
            character.currentMeeting!!
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        if (intVariables["routineStartTime"]!! + 4 <= gState.time)
        {
            return true
        }
        return false
    }
}