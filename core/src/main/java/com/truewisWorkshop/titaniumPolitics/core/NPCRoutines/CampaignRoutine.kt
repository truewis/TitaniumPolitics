package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

/**
 * A routine where the character campaigns for an upcoming division leader election.
 * The character wanders public places and talks to eligible voters (party members),
 * praising their preferred candidate or denouncing opponents based on their stats and mutualities.
 */
@Serializable
class CampaignRoutine(val electionParty: String) : Routine() {
    init {
        priority = PRIORITY_REST + 5
    }

    val triedTalkingTo = mutableSetOf<String>()

    /**Tracks voters who have already received a present during this campaign, to avoid double-gifting.*/
    val givenPresentsTo = mutableSetOf<String>()

    /**
     * The luxury resource chosen for giving presents during this campaign session.
     * Cached so that the character doesn't switch resources mid-campaign.
     */
    var chosenPresentResource: String? = null

    /**
     * Returns the candidate this character most wants to support (highest mutual affinity),
     * including themselves if they are a director in the party.
     */
    private fun supportedCandidate(name: String): String? {
        val party = gState.parties[electionParty] ?: return null
        val candidates = party.directorMembers.toList()
        if (candidates.isEmpty()) return null
        val selfIncluded =
            if (name !in candidates && name in party.members && gState.characters[name]!!.type == Character.Type.DIRECTOR)
                candidates + name
            else candidates
        return selfIncluded.maxByOrNull { gState.getMutuality(name, it) }
    }

    /**
     * Returns the candidate this character most wants to oppose (lowest mutual affinity),
     * excluding the supported candidate.
     */
    private fun opposedCandidate(name: String, supported: String?): String? {
        val party = gState.parties[electionParty] ?: return null
        val candidates = party.directorMembers.filter { it != name && it != supported }.toList()
        if (candidates.isEmpty()) return null
        return candidates.minByOrNull { gState.getMutuality(name, it) }
    }

    /**
     * Builds a campaign agenda: PRAISE for the supported candidate or DENOUNCE for an opponent.
     * Returns null if neither relationship is strong enough to warrant action.
     */
    fun buildCampaignAgenda(name: String): MeetingAgenda? {
        val supported = supportedCandidate(name)
        if (supported != null &&
            gState.getMutuality(name, supported) > ReadOnly.const("FriendMutualityThreshold")
        ) {
            return MeetingAgenda(AgendaType.PRAISE, name).also {
                it.subjectParams["character"] = supported
            }
        }
        val opposed = opposedCandidate(name, supported)
        if (opposed != null &&
            gState.getMutuality(name, opposed) < ReadOnly.const("EnemyMutualityThreshold")
        ) {
            return MeetingAgenda(AgendaType.DENOUNCE, name).also {
                it.subjectParams["character"] = opposed
            }
        }
        return null
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        // End campaign if the election is no longer scheduled.
        val electionScheduled = gState.scheduledMeetings.values.any {
            it.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && it.involvedParty == electionParty
        }
        if (!electionScheduled) return success()

        val char = gState.characters[name]!!
        val party = gState.parties[electionParty]!!

        // Resolve (or refresh) which luxury resource to use for presents.
        val presentDef = chosenPresentResource
            ?.let { NonPlayerAgent.ALL_LUXURY_RESOURCES.firstOrNull { def -> def.resourceName == it } }
            ?: NonPlayerAgent.chooseLuxuryResource(char) { res ->
                gState.publicPlaces.values.any { it.resources[res] > 0 }
            }
        if (presentDef != null && chosenPresentResource == null) {
            chosenPresentResource = presentDef.resourceName
        }

        // Acquire the chosen luxury resource for presents if the character doesn't have enough.
        if (presentDef != null &&
            char.resources[presentDef.resourceName] < presentDef.giftAmount &&
            subroutines.none { it is BuyRoutine || it is StealRoutine }
        ) {
            val needed = presentDef.giftAmount - char.resources[presentDef.resourceName]
            return if ("thief" in char.trait) StealRoutine(presentDef.resourceName, needed)
            else BuyRoutine(presentDef.resourceName, needed)
        }

        // If currently in a talk meeting, attend it and seize the moment to campaign.
        char.currentMeeting?.let { meeting ->
            if (meeting.type == Meeting.MeetingType.TALK) {
                triedTalkingTo += meeting.currentCharacters
                if (subroutines.none { it is AttendPrivateMeetingRoutine }) {
                    return AttendPrivateMeetingRoutine(
                        scheduledMeetingName = gState.meetingName(meeting),
                        agenda = buildCampaignAgenda(name)
                    )
                }
                return null
            }
        }

        // Move to a public place where there are voters we haven't spoken to yet.
        if (place !in Place.publicPlaces) {
            val targetPlace = Place.publicPlaces.firstOrNull { placeName ->
                gState.places[placeName]?.characters?.any {
                    it != name && it in party.members && it !in triedTalkingTo
                } == true
            } ?: Place.publicPlaces.random()
            return MoveRoutine(targetPlace)
        }

        // Talk to an uncontacted voter in the current public place.
        val potentialVoters = gState.places[place]!!.characters.filter {
            it != name && it in party.members && it !in triedTalkingTo
        }
        if (potentialVoters.isNotEmpty()) {
            val voter = potentialVoters.random()
            triedTalkingTo.add(voter)
            if (subroutines.none { it is AttendPrivateMeetingRoutine }) {
                return AttendPrivateMeetingRoutine(
                    toWho = voter,
                    agenda = buildCampaignAgenda(name)
                )
            }
        }

        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val char = gState.characters[name]!!
        val party = gState.parties[electionParty] ?: return Wait(name, place)
        // Give a luxury resource present to uncontacted voters sharing the same public place.
        val presentDef = chosenPresentResource
            ?.let { NonPlayerAgent.ALL_LUXURY_RESOURCES.firstOrNull { def -> def.resourceName == it } }
            ?: return Wait(name, place)
        if (place in Place.publicPlaces && char.resources[presentDef.resourceName] >= presentDef.giftAmount) {
            val uncontactedVoter = gState.places[place]!!.characters.firstOrNull {
                it != name && it in party.members && it !in givenPresentsTo
            }
            if (uncontactedVoter != null) {
                val gift = UnofficialResourceTransfer(
                    name, "home_$name", "home_$uncontactedVoter", true,
                    Resources(presentDef.resourceName to presentDef.giftAmount), gState
                )
                if (gift.isValid()) {
                    givenPresentsTo.add(uncontactedVoter)
                    return gift
                }
            }
        }
        return Wait(name, place)
    }
}
