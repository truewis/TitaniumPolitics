package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.DTH
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.EndMeeting
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Intercept
import com.titaniumPolitics.game.core.gameActions.NewAgenda
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*
import kotlin.math.min

//Trying implementing design pattern with function call stack is a bad idea because it is hard to debug.
//Routine was designed to be independent of the gameState, but it is not the case anymore.
@Serializable
sealed class Routine() {
    @Transient
    lateinit var gState: GameState
    val ID = UUID.randomUUID().toString()
    var priority: Int = 0
    val subroutines = arrayListOf<String>() //Store the IDs of subroutines that are currently running.
    var routineStartTime: Int = 0 //The time when the routine starts, used to calculate the duration of the routine.
    val variables: HashMap<String, String> = hashMapOf()
    val intVariables: HashMap<String, Int> = hashMapOf()
    val doubleVariables: HashMap<String, Double> = hashMapOf()
    val PRIORITY_WORK = 1000
    val PRIORITY_MEETING = 1500
    val PRIORITY_REST = 0
    val PRIORITY_LIFE_SUPPORT = 2000
    var executeDone =
        false //This is used to check if the routine execution is successful. Otherwise, there is a problem executing the routine and the parent routine should be notified.

    fun injectParent(gState: GameState) {
        this.gState = gState
    }

    abstract fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine?
    abstract fun execute(name: String, place: String): GameAction
    abstract fun endCondition(name: String, place: String): Boolean

    //The actions in this list are compared with GameEngine.availableActions() to see if the command is available.
    //Then, instances of the actions are created, their parameters are optimized for deltaWill, and their validity is checked.
    //If the action is valid, one with the highest deltaWill is executed.
    //Routines can switch to other routines in the meanwhile.
    @Transient
    open val availableActions: List<String> = listOf("Wait")

    //        get()
//        {
//            return when (name)
//            {
//                "work" -> listOf("Wait")
//                "rest" -> listOf("Eat", "Sleep", "Wait")
//                "attendMeeting" -> listOf("attendMeeting")
//                "supportAgenda" -> listOf("supportAgenda")
//                "attackAgenda" -> listOf("attackAgenda")
//                "attendConference" -> listOf("attendConference")
//                "findCharacter" -> listOf("Move")
//                "buy" -> listOf("buy")
//                else -> listOf()
//            }
//        }
    //TODO: it isn't clear at this moment how we pick between actions and routines. Shall we only pick between routines?
    //Just like the player pick actions at his will, NPC doesn't have to follow the gradient of will always. We just have to implement the penalty when the will is low in the game system.
    //Will based behaviour can be implemented in a different agent.
    fun pickAction(name: String, place: String): GameAction {

        return availableActions.intersect(GameEngine.availableActions(gState, place, name).toSet()).map {
            (Class.forName("com.titaniumPolitics.game.core.gameActions.$it")
                .getConstructor(String::class.java, String::class.java)
                .newInstance(name, place) as GameAction).apply { injectParent(gState);chooseParams() }

        }.filter { it.isValid() }.maxBy { it.optimizeWill() }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Shared Functions

    fun supportProofOfWork(conf: Meeting, name: String): Routine? {

        //If speaker, try supporting proof of work if I am involved.
        //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
        if (conf.agendas.any {
                it.type == AgendaType.PROOF_OF_WORK && (it.attachedRequest == null /*If request is null, proof of work is about the general attire, so support it anyways.*/ || (name in it.attachedRequest!!.issuedBy && it.attachedRequest!!.issuedTo.intersect(
                    conf.currentCharacters
                ).isNotEmpty()))
            }) {

            //If we haven't tried this branch in the current routine
            if (intVariables["try_support_proofOfWork"] != 1) {
                //If the agenda is already proposed, and we have a supporting information, support it.
                intVariables["try_support_proofOfWork"] = 1
                return (
                        SupportAgendaRoutine().apply {
                            intVariables["agendaIndex"] =
                                conf.agendas.indexOfFirst { it.type == AgendaType.PROOF_OF_WORK }
                        })//Add a routine, priority higher than work.
            }
        }
        return null
    }

    fun proposeProofOfWork(conf: Meeting, name: String, place: String): GameAction? {
        //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
        //Some information are more relevant than others.
        if (conf.agendas.none { it.type == AgendaType.PROOF_OF_WORK }) {
            gState.requests.values.firstOrNull {
                name in it.issuedBy && it.issuedTo.intersect(conf.currentCharacters)
                    .isNotEmpty() && !it.completed && conf.agendas.none { agenda -> agenda.type == AgendaType.REQUEST && agenda.attachedRequest == it } /*Do not demand the request submitted in this meeting to be proved right away.*/
            }?.let { req ->
                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(AgendaType.PROOF_OF_WORK, name, attachedRequest = req)
                }
            }

        }
        return null
    }

    fun matchRequests(conf: Meeting, name: String, place: String): GameAction? {
        //Find all requests in this meeting that is issued to me.
        val requests = conf.agendas.filter { it.type == AgendaType.REQUEST }.map { it.attachedRequest }
            .filter { it != null && name in it.issuedTo }
        val char = gState.characters[name]!!
        if (requests.isEmpty()) return null

        //Compute the aggregate value of the requests.
        val totalValue = requests.sumOf { char.actionValue(it!!.action) }

        //Compute the total item value of each of the issuers.
        val issuers = requests.flatMap { it!!.issuedBy }.distinct()
        val issuersItemValues =
            issuers
                .map { n1 -> Pair(n1, char.itemValue(gState.characters[n1]!!.resources)) }

        val issuersActionValues =
            issuers
                .map { n1 -> Pair(n1, askForValuableAction(n1, name)?.let { char.actionValue(it) } ?: .0) }


        val maxItemValue = issuersItemValues.maxOfOrNull { it.second } ?: 0.0
        val maxActionValue = issuersActionValues.maxOfOrNull { it.second } ?: 0.0

        //If the maximum value is 0, then there is no matching request.
        if (maxItemValue <= 0 && maxActionValue <= 0) return null

        if (maxItemValue > maxActionValue) {

            //Find the issuer with the highest item value.
            val bestIssuer = issuersItemValues.maxByOrNull { it.second }?.first ?: return null

            //If the best issuer's item value is higher than the total value of the requests, propose a new agenda to match the requests.
            if (issuersItemValues.maxOf { it.second } > totalValue) {

                //Pick resources from the best issuer until the total value of the requests is met.
                val resourcesToTransfer = gState.characters[bestIssuer]!!.resources
                    .keys.filter { char.itemValue(it) > 0 }

                // Sort the resource keys by my relative demand.
                val sortedResources = resourcesToTransfer.sortedByDescending { char.itemValueModifier(it) }

                // Pick resources until the total value of the requests is met.
                val resourcesToTransferMap = hashMapOf<String, Double>()
                var remainingValue = totalValue
                for (resource in sortedResources) {
                    if (remainingValue <= 0) break
                    val amount = min(
                        gState.characters[bestIssuer]!!.resources[resource],
                        remainingValue
                    )
                    if (amount > 0) {
                        resourcesToTransferMap[resource] = amount
                        remainingValue -= amount * char.itemValueModifier(resource)
                    }
                }


                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(
                        AgendaType.REQUEST,
                        author = name,
                        attachedRequest = Request(
                            action = UnofficialResourceTransfer(bestIssuer, "home_$bestIssuer").apply {
                                fromHome = true
                                toWhere = "home_$name"
                                resources = Resources(resourcesToTransferMap)
                            },
                            issuedTo = hashSetOf(bestIssuer),
                            issuedBy = hashSetOf(name)
                        )
                    )
                }
            }
        } else {
            //If the maximum action value is higher than the total value of the requests, propose a new agenda to match the requests.
            val bestActionIssuer = issuersActionValues.maxByOrNull { it.second }?.first ?: return null
            val action = askForValuableAction(bestActionIssuer, name) ?: return null

            if (gState.characters[bestActionIssuer]!!.actionValue(action) >= totalValue) {
                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(
                        AgendaType.REQUEST,
                        author = name,
                        attachedRequest = Request(
                            action = action,
                            issuedTo = hashSetOf(bestActionIssuer),
                            issuedBy = hashSetOf(name)
                        )
                    )
                }
            }
        }

        return null
    }

    fun askForValuableAction(who: String, name: String): GameAction? {
        val tgtChar = gState.characters[who] ?: return null
        if ("engineer" in tgtChar.trait) {
            //If the place I am managing has a broken apparatus, request repair.
            gState.places.values.filter { it.manager == name }.forEach { placeObj ->
                if (placeObj.apparatuses.any { it.durability < 50f })
                    return Repair(who, placeObj.name).apply {
                        injectParent(gState)
                    }
            }


        }
        return null
    }

    fun interceptCondition(
        conf: Meeting,
        name: String,
        place: String
    ): GameAction {
        if (gState.getMutuality(
                name,
                conf.currentSpeaker!!
            ) > ReadOnly.const("SpeakerInterceptMutualityThreshold")
        )
            return Wait(name, place)
        else {
            val action = Intercept(name, place).also { it.injectParent(gState) }
            if (action.isValid())
                return action
            return Wait(name, place)
        }
    }

    fun endMeetingIfLowAttention(
        conf: Meeting,
        name: String,
        place: String
    ): GameAction? {
        //If the attention of the meeting is low, end the meeting.
        if (conf.currentCharacters.count() > 1 && conf.currentAttention < 10) {
            return EndMeeting(name, place)
        }
        return null
    }

    fun meetingRoutineEndCondition(name: String, type: Meeting.MeetingType): Boolean {
        return routineStartTime + 7200 / ReadOnly.DT <= gState.time || gState.characters[name]!!.currentMeeting?.let { it.type != type } ?: false || (gState.characters[name]!!.currentMeeting?.currentAttention
            ?: 100) < 10
        /*Sometimes characters are transferred between different meetings without their turn. In that case, the previous meeting routine is killed here.*/
    }

    override fun toString(): String {
        return "${this::class.simpleName}(ID='$ID', priority=$priority, subroutines=$subroutines, routineStartTime=$routineStartTime, variables=$variables, intVariables=$intVariables, doubleVariables=$doubleVariables, executeDone=$executeDone)"
    }

    companion object {
        fun isWorkHourWithETA(gState: GameState, place: String, workplace: String, padding: Int = 0): Boolean {
            //Consider the estimated time to workplace, if the character is not at home.
            val eta = gState.places[place]!!.shortestPathAndTimeTo(workplace)?.second ?: 0
            val extendedWorkHours =
                (gState.places[workplace]!!.workHours.first / DTH).toInt() - eta - padding..(gState.places[workplace]!!.workHours.last / DTH).toInt() + eta + padding
            return (gState.timeInDay in extendedWorkHours)
        }
    }
}