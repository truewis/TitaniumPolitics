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
    @Transient
    private val affectionResourceRequests = hashSetOf<Pair<String, String>>()
    @Transient
    private val affectionActionRequests = hashSetOf<Pair<String, String>>()
    @Transient
    private val affectionPoliticalSupportRequests = hashSetOf<Triple<String, String, String>>()
    @Transient
    private val bossThreats = hashSetOf<String>()
    @Transient
    private val reportComplaints = hashSetOf<String>()
    @Transient
    private val peerThreats = hashSetOf<String>()

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
        add(Event_DelayRepair1())
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
        add(Event_Hans1())
        //add(Event_Lynn1())
        add(Event_Sasha1())
        //add(Event_Jan1())
        add(Event_Maisarah1())
        add(Event_Astinomis1())
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

    fun registerMeetingObjective(meeting: Meeting) {
        if (parent.playerName !in meeting.currentCharacters) return
        if (dataBase.any { it is Event_ImprovisedMeetingObjective && it.meetingId == meeting.ID && it.active }) return
        val event = Event_ImprovisedMeetingObjective(meeting.ID)
        event.injectParent(parent)
        add(event)
    }

    fun failMeetingObjectivesForMeeting(meeting: Meeting) {
        dataBase.filterIsInstance<Event_ImprovisedMeetingObjective>()
            .filter { it.meetingId == meeting.ID && it.active }
            .forEach { it.deactivate(false) }
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
            indexEvent(it)
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
                indexEvent(it)
            }
            tmpdataBase.clear()

            dataBase.forEach { if (it.active) it.exec(a, b) }
            gameState.requests.filter {
                !it.value.completed &&
                    gameState.playerName in it.value.issuedTo
            }.forEach { (_, req) ->

                add(Event_GenuineRequest(req.name))

            }
            gameState.scheduledMeetings.values.firstOrNull {
                it.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && it.involvedParty == gameState.player.division?.name
            }?.let { add(Event_ElectionApproaching(it.ID)) }
            gameState.scheduledMeetings.values.firstOrNull {
                it.type == Meeting.MeetingType.BUDGET_PROPOSAL && gameState.playerName in it.scheduledCharacters
            }?.let { add(Event_ProposeBudget(it.ID)) }
            val relevantInfos = parent.player.preparedInfoKeys.map {
                parent.informations[it]!!
            }.filter { info ->
                parent.time - info.tgtTime < 168 * IDTH //Has to be recent enough
            }
            val infoTypesByPlace = hashMapOf<String?, MutableSet<InformationType>>()
            relevantInfos.forEach { info ->
                infoTypesByPlace.getOrPut(info.tgtPlace) { hashSetOf() }.add(info.type)
            }
            val playerLeadingWorkplaces = parent.parties.values.filter {
                it.leader == parent.playerName && it.type == Party.Type.WORKPLACE
            }
            playerLeadingWorkplaces.forEach { party ->
                val place = party.workplace.name
                val types = infoTypesByPlace[place]
                if (types == null ||
                    InformationType.HUMAN_RESOURCES !in types ||
                    InformationType.APPARATUS !in types ||
                    InformationType.RESOURCES !in types
                ) {
                    add(Event_ExpiredWorkplaceInformation(place))
                }
                if (!party.isSalaryPaid)
                    add(Event_PaySalary(party.name))
            }
            val playerLedParties = parent.parties.values.filter {
                it.leader == parent.playerName
            }
            playerLedParties.forEach {
                val hatefulMembers = it.members.filter {
                    parent.getMutNorm(it, parent.playerName) < -0.25
                }
                if (hatefulMembers.isNotEmpty())
                    add(Event_HatefulDirectReport(hatefulMembers, it.name))
                if (it.integrity < -0.25)
                    add(Event_ImprovePartyIntegrity(it.name))

            }
            // Trigger affection events for characters who like the player
            val likedCharacters = parent.characters.values.filter { char ->
                parent.getMutNorm(char.name, parent.playerName) > 0.25 &&
                    char.name != parent.playerName
            }
            val recentPlayerInfos = parent.informations.values.filter { info ->
                parent.playerName in info.knownTo &&
                    parent.time - info.tgtTime < 168 * IDTH
            }
            val resourceInfos = recentPlayerInfos.filter {
                it.type == InformationType.RESOURCES && it.tgtCharacter != parent.playerName
            }
            val actionInfos = recentPlayerInfos.filter {
                it.type == InformationType.ACTION || it.type == InformationType.APPARATUS
            }
            val negativeMutualityInfos = recentPlayerInfos.filter {
                it.type == InformationType.PARTY_MUTUALITY && it.amount < 0
            }
            val firstPartyByMember = hashMapOf<String, Party>()
            parent.parties.values.forEach { party ->
                party.members.forEach { member ->
                    firstPartyByMember.putIfAbsent(member, party)
                }
            }
            likedCharacters.forEach { char ->
                resourceInfos.forEach { info ->
                    if (info.tgtCharacter != char.name &&
                        (char.name to info.name) !in affectionResourceRequests
                    ) {
                        add(Event_AffectionResourceRequest(char.name, info.name))
                    }
                }

                actionInfos.forEach { info ->
                    if ((char.name to info.name) !in affectionActionRequests) {
                        add(Event_AffectionActionRequest(char.name, info.name))
                    }
                }

                val charParty = firstPartyByMember[char.name]
                if (charParty != null) {
                    negativeMutualityInfos.forEach { info ->
                        if (Triple(char.name, charParty.name, info.name) !in affectionPoliticalSupportRequests) {
                            add(Event_AffectionPoliticalSupport(char.name, charParty.name, info.name))
                        }
                    }
                }
            }

            // Trigger threat events for characters who dislike the player
            parent.characters.values.filter { char ->
                parent.getMutNorm(char.name, parent.playerName) < -0.25 &&
                    char.name != parent.playerName &&
                    char.alive
            }.forEach { char ->
                // Determine relationship to player
                val playerDivision = parent.player.division
                val charDivision = char.division

                val isCharBoss = charDivision?.leader == char.name && playerDivision?.leader == parent.playerName
                val isCharDirectReport = playerDivision?.leader == parent.playerName &&
                    playerDivision?.members?.contains(char.name) == true

                when {
                    // Boss threatening to fire
                    isCharBoss && char.name !in bossThreats -> {
                        add(Event_ThreatFromBoss(char.name))
                    }
                    // Direct report complaining
                    isCharDirectReport && char.name !in reportComplaints -> {
                        add(Event_ComplaintFromDirectReport(char.name))
                    }
                    // Peer threatening
                    !isCharBoss && !isCharDirectReport &&
                        char.name !in peerThreats -> {
                        add(Event_ThreatFromPeer(char.name))
                    }
                }
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

    private fun indexEvent(event: EventObject) {
        when (event) {
            is Event_AffectionResourceRequest ->
                affectionResourceRequests += (event.charName to event.infoKey)
            is Event_AffectionActionRequest ->
                affectionActionRequests += (event.charName to event.infoKey)
            is Event_AffectionPoliticalSupport ->
                affectionPoliticalSupportRequests += Triple(event.charName, event.partyKey, event.infoKey)
            is Event_ThreatFromBoss -> bossThreats += event.charName
            is Event_ComplaintFromDirectReport -> reportComplaints += event.charName
            is Event_ThreatFromPeer -> peerThreats += event.charName
            else -> Unit
        }
    }

    fun displayEmoji(who: String): SpeechUI.EmojiType {
        return dataBase.firstOrNull { it.active && it.displayEmoji(who) != SpeechUI.EmojiType.NONE }
            ?.displayEmoji(who) ?: SpeechUI.EmojiType.NONE
    }

    companion object {
        val onPlayDialogue = arrayListOf<(String) -> Unit>()
    }

}
