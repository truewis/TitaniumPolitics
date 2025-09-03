package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.events.*
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


//Events are quests that never expire. Some can be triggered many times, some only once.
@Serializable
class EventSystem : GameStateElement() {
    override val name: String
        get() = "EventSystem" //There is only one EventSystem object in the game.
    private val dataBase = arrayListOf<EventObject>()
    private val tmpdataBase = arrayListOf<EventObject>()

    @Transient
    val quests =
        arrayListOf<Quest>() //Do not use haseSet, it is not meant to be used with objects that can be modified.

    //Utility function called once when a new game starts.
    fun newGame() {
        add(Event_PrologueAlinaSpeech())
        add(Event_DelayRepair1())
        add(Event_BribeDoctor1())
        add(Event_BoyFindingMom())
        //dataBase.add(Event_ObserverIntro())
        add(Event_AlinaIllTheory1())
        add(Event_SalvorElection())
        add(Event_SecureOuterBarrierEast())
    }

    fun updateQuest(quest: Quest) {
        quest.parent = parent
        if (quests.any { it.name == quest.name }) {
            quests.removeIf { it.name == quest.name }
        }
        quests.add(quest)
    }

    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        dataBase.forEach {
            it.injectParent(gameState)
            if (!it.completed)
                println(
                    "Injecting parent to event ${it.name} completed=${it.completed}" +
                            if (it is IQuestEventObject) " quest=${it.quest.name}" else ""
                )
        }
        gameState.timeChanged += { a, b ->
            dataBase.forEach { if (!it.completed) it.exec(a, b) }
            tmpdataBase.forEach {
                it.injectParent(gameState)
                dataBase += it
            }
            tmpdataBase.clear()
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

    }

    fun add(event: EventObject) {
        tmpdataBase.add(event)
    }

    fun displayEmoji(who: String): SpeechUI.EmojiType {
        return dataBase.firstOrNull { !it.completed && it.displayEmoji(who) != SpeechUI.EmojiType.NONE }
            ?.displayEmoji(who) ?: SpeechUI.EmojiType.NONE
    }

    companion object {
        val onPlayDialogue = arrayListOf<(String) -> Unit>()
    }

}
