package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.events.*
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.random.Random


//Events are quests that never expire. Some can be triggered many times, some only once.
@Serializable
class EventSystem : GameStateElement() {
    override val name: String
        get() = "EventSystem" //There is only one EventSystem object in the game.
    private val dataBase = arrayListOf<EventObject>()

    /*
    Temporary database to hold newly added events until the next time update is called. This is to avoid concurrent modification exception when events add new events during execution.
     */
    private val tmpdataBase = arrayListOf<EventObject>()

    /**
     * Currently active quests.
     * Do not serialize, as they will be reconstructed from the events when loading a game.
     */
    @Transient
    val activeQuests =
        arrayListOf<Quest>() //Do not use hashSet, it is not meant to be used with objects that can be modified.

    val successfulQuests = arrayListOf<Quest>()

    @Transient
    val failedQuests = arrayListOf<Quest>()

    //Utility function called once when a new game starts.
    fun newGame() {
        add(Event_PrologueAlinaAccident())
        //add(Event_DelayRepair1())
        //add(Event_YuhoaIntro())
        //add(Event_BoyFindingMom())
        //add(Event_BefreindTheBoy())
        //add(Event_Salvor1())
        //dataBase.add(Event_ObserverIntro())
        //add(Event_AlinaIllTheory1())
        //add(Event_SalvorElection())
        //add(Event_SecureOuterBarrierEast())
        //add(Event_EugeneIntro())
        //add(Event_Vaeme1())
        //add(Event_Hans1())
        //add(Event_Lynn1())
        //add(Event_Sasha1())
        //add(Event_Jan1())
        //add(Event_Maisarah1())
        //add(Event_Astinomis1())
        // Party drama events – permanent, continuously monitor all workplace parties
        add(Event_Drama_CreditTheft())
        add(Event_Drama_Gossip())
        add(Event_Drama_Bullying())
        add(Event_Drama_Favoritism())
        add(Event_Drama_Overwork())
        add(Event_Drama_Jealousy())
        add(Event_Drama_Blame())
        add(Event_Drama_Exclusion())
        add(Event_Drama_Rivalry())
        add(Event_Drama_Undermining())
    }

    fun updateQuest(event: IQuestEventObject, quest: Quest) {
        quest.parent = parent
        quest.event = event
        if (activeQuests.any { it.name == quest.name }) {
            activeQuests.removeIf { it.name == quest.name }
        }
        activeQuests.add(quest)
    }

    fun finishQuest(event: IQuestEventObject, success: Boolean = true) {
        val toRemove = arrayListOf<Quest>()
        activeQuests.filter { it.event == event }.forEach { quest ->
            quest.completionTime = parent.time
            if (success)
                successfulQuests.add(quest)
            else
                failedQuests.add(quest)
            toRemove.add(quest)
        }
        activeQuests.removeAll(toRemove)//I am afraid of set equality check, so I used list here.

    }

    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        dataBase.forEach {
            it.injectParent(gameState)
            if (it.active)
                println(
                    "Injecting parent to event ${it.name} active=${it.active}" +
                        if (it is IQuestEventObject) " quest=${it.quest.name}" else ""
                )
        }
        gameState.timeChanged += { a, b ->
            tmpdataBase.forEach {
                it.injectParent(gameState)
                dataBase += it
            }
            tmpdataBase.clear()

            dataBase.forEach { if (it.active) it.exec(a, b) }
            gameState.requests.filter {
                !it.value.completed &&
                    gameState.playerName in it.value.issuedTo
            }.forEach { (_, req) ->

                add(Event_GenuineRequest(req.name))

            }
            gameState.scheduledMeetings.filter {
                it.value.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && it.value.involvedParty == gameState.player.division?.name
            }.keys.firstOrNull()?.let { add(Event_ElectionApproaching(it)) }
            gameState.scheduledMeetings.filter {
                it.value.type == Meeting.MeetingType.BUDGET_PROPOSAL && gameState.playerName in it.value.scheduledCharacters
            }.keys.firstOrNull()?.let { add(Event_ProposeBudget(it)) }
            gameState.scheduledMeetings.filter {
                it.value.type == Meeting.MeetingType.BUDGET_RESOLUTION && gameState.playerName in it.value.scheduledCharacters
            }.keys.firstOrNull()?.let { add(Event_ResolveBudget(it)) }
            val relevantInfos = parent.player.preparedInfoKeys.map {
                parent.informations[it]!!
            }.filter { info ->
                parent.time - info.tgtTime < 168 * IDTH //Has to be recent enough
            }
            parent.parties.values.filter {
                it.leader == parent.playerName && it.type == Party.Type.WORKPLACE
            }.forEach {
                val place = it.workplace.name
                if (relevantInfos.none { it.type == InformationType.HUMAN_RESOURCES && it.tgtPlace == place }
                    || relevantInfos.none { it.type == InformationType.APPARATUS && it.tgtPlace == place }
                    || relevantInfos.none { it.type == InformationType.RESOURCES && it.tgtPlace == place })
                    add(Event_ExpiredWorkplaceInformation(place))
                if (!it.isSalaryPaid)
                    add(Event_PaySalary(it.name))
            }
            parent.parties.values.filter {
                it.leader == parent.playerName
            }.forEach {
                val hatefulMembers = it.members.filter {
                    parent.getMutNorm(it, parent.playerName) < -0.25
                }
                if (hatefulMembers.isNotEmpty())
                    add(Event_HatefulDirectReport(hatefulMembers, it.name))
                if (it.integrity < -0.25)
                    add(Event_ImprovePartyIntegrity(it.name))

            }
        }
        // Register a listener to trigger party dramas from meeting actions (~5% chance per action).
        gameState.onMeetingAction += { action ->
            if (Random.nextDouble() < 0.05) {
                gameState.ongoingMeetings.values.firstOrNull { action.sbjCharacter in it.currentCharacters }
                    ?.let { meeting ->
                        dataBase.filter { it.active }.forEach { event -> event.execInMeeting(meeting) }
                    }
            }
        }

    }

    fun add(event: EventObject) {
        tmpdataBase.add(event)
    }

    fun displayEmoji(who: String): SpeechUI.EmojiType {
        return dataBase.firstOrNull { it.active && it.displayEmoji(who) != SpeechUI.EmojiType.NONE }
            ?.displayEmoji(who) ?: SpeechUI.EmojiType.NONE
    }

    companion object {
        val onPlayDialogue = arrayListOf<(String) -> Unit>()
    }

}
