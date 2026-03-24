package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.NonPlayerAgent
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.AnnounceInfo
import com.titaniumPolitics.game.core.gameActions.Examine
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class WorkRoutine(var workplace: String) : Routine() {
    var corruptionTimer = 0

    /**
     * Timer to limit how often the character acquires luxury resources (fineFood) for feasts and presents.
     */
    var luxuryAcquisitionTimer = 0

    /**
     * Timer to limit how often the character will transfer resource to places that are short of it.
     */
    var transferResourceTimer = 0
    var repairApparatusTimer = 0
    var announceInfoTimer = 0
    var try_prepare_info = 0
    val meetingsAttended = hashSetOf<String>()
    val failedRequests = hashSetOf<String>()

    /**
     * Tracks the simple class names of subroutine types that have previously failed during this
     * WorkRoutine's lifetime. Failed subroutine types will not be retried.
     */
    val failedSubroutineTypes = hashSetOf<String>()

    init {
        priority = PRIORITY_WORK
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {

        //If work hours are over, rest. Also, if the character is too hungry, thirsty, or sick, rest. (Which is checked earlier.)
        if (!isWorkCondition(name, place, workplace, gState))
            return success()
        val character = gState.characters[name]!!

        // Mandatory: already forced into a meeting - handle immediately outside the priority queue.
        if (character.currentMeeting != null) {
            if (subroutines.none {
                    it is MeetingRoutine && it.meetingName == gState.meetingName(character.currentMeeting!!)
                })
                return pickMeetingRoutine(name, character.currentMeeting!!).apply {
                    priority = PRIORITY_MEETING
                }
        }

        // Build a priority queue of candidate subroutines.
        // Each entry is a lazy factory paired with an urgency score; only the winning factory is invoked.
        val candidates = mutableListOf<Pair<() -> Routine, Int>>()

        //1. If an accident happened in the place of my control, investigate and clear it.
        if (!failedSubroutineTypes.contains(InvestigateAndClearAccidentRoutine::class.java.simpleName)) {
            gState.places.values.firstOrNull {
                it.responsibleDivision != null && gState.parties[it.responsibleDivision]!!.members.contains(
                    name
                ) && it.isAccidentScene
            }?.also { accidentPlace ->
                if (subroutines.none { it is InvestigateAndClearAccidentRoutine && it.investigatePlace == accidentPlace.name }) {
                    candidates += Pair(
                        { InvestigateAndClearAccidentRoutine(accidentPlace.name) },
                        PRIORITY_WORK + 10000
                    )
                }
            }
        }

        //2. Rescue people - only if I am in the safety division and has emt trait
        if (character.division?.name == "safety" && "emt" in character.trait) {
            if (!failedSubroutineTypes.contains(RescueRoutine::class.java.simpleName)) {
                gState.places.values.firstOrNull {
                    it.characters.any { charName -> gState.characters[charName]!!.isUnconscious }
                }?.also { place1 ->
                    val charToRescueName = place1.characters.first { charName ->
                        gState.characters[charName]!!.isUnconscious
                    }
                    if (subroutines.none { it is RescueRoutine && it.rescuee == charToRescueName }) {
                        Logger.write(
                            "$name is going to rescue $charToRescueName at ${place1.name}",
                            Logger.LogLevel.ACTION_VERBOSE
                        )
                        candidates += Pair({ RescueRoutine(charToRescueName) }, PRIORITY_WORK + 8000)
                    }
                }
            }
        }

        //3. If missed a conference - urgency is high because the meeting is already happening
        if (subroutines.none { it is MeetingRoutine }) {
            val missingMeeting = gState.ongoingMeetings.values
                .firstOrNull { it.scheduledCharacters.contains(name) && !it.currentCharacters.contains(name) }
            if (missingMeeting != null && gState.meetingName(missingMeeting) !in meetingsAttended) {
                candidates += Pair(
                    { pickMeetingRoutine(name, missingMeeting).apply { priority = PRIORITY_MEETING } },
                    PRIORITY_MEETING + 500
                )
            }
        }

        //4. If a conference is scheduled - urgency rises as start time approaches
        if (subroutines.none { it is MeetingRoutine }) {
            gState.scheduledMeetings.values.firstOrNull {
                if (!it.scheduledCharacters.contains(name)) return@firstOrNull false
                val eta = gState.places[it.place]!!.shortestPathAndTimeTo(place, name)?.second
                    ?: return@firstOrNull false
                return@firstOrNull it.isValidTimeToStart(gState.time + eta) || it.isValidTimeToStart(gState.time + eta + 30)
            }?.also { conf ->
                val timeUntil = maxOf(0, conf.time - gState.time)
                val meetingUrgency = PRIORITY_MEETING + maxOf(0, ReadOnly.IDTH - timeUntil)
                candidates += Pair(
                    { pickMeetingRoutine(name, conf).apply { priority = PRIORITY_MEETING } },
                    meetingUrgency
                )
            }
        }

        //5. Execute a command if there is any.
        gState.requests.values.firstOrNull {
            if (name !in it.issuedTo) return@firstOrNull false
            if (it.name in failedRequests) return@firstOrNull false
            val eta =
                gState.places[it.action.tgtPlace]!!.shortestPathAndTimeTo(place, name)?.second
                    ?: return@firstOrNull false
            return@firstOrNull (it.executeTime in gState.time - ReadOnly.constInt("CommandExecuteTolerance") + eta..gState.time + ReadOnly.constInt(
                "CommandExecuteTolerance"
            ) + eta || it.executeTime == null) && (it.issuedBy.isEmpty() || it.issuedBy.sumOf {
                gState.getMutuality(name, it)
            } / it.issuedBy.size > it.difficulty(gState)) && GameEngine.availableActions(
                gState, it.action.tgtPlace, name
            ).contains(it.action.javaClass.simpleName)
        }?.also { request ->
            if (subroutines.none { it is ExecuteRequestRoutine && it.variables["request"] == request.name })
                candidates += Pair(
                    {
                        ExecuteRequestRoutine().also {
                            it.variables["request"] = request.name
                            it.priority = PRIORITY_WORK + 400
                        }
                    },
                    PRIORITY_WORK + 400
                )
        }

        // (c) Acquire resources for pending requests toward this character that require resources they lack.
        gState.requests.values.firstOrNull { req ->
            name in req.issuedTo && !req.completed && req.name !in failedRequests &&
                req.action is UnofficialResourceTransfer &&
                (req.action as UnofficialResourceTransfer).fromHome &&
                !gState.characters[name]!!.resources.contains((req.action as UnofficialResourceTransfer).resources)
        }?.also { req ->
            if (subroutines.none { it is BuyRoutine || it is StealRoutine }) {
                val action = req.action as UnofficialResourceTransfer
                action.resources.toHashMap().entries.firstOrNull { entry ->
                    character.resources[entry.key] < entry.value
                }?.let { entry ->
                    val isThief = "thief" in character.trait
                    val routineTypeName =
                        if (isThief) StealRoutine::class.java.simpleName else BuyRoutine::class.java.simpleName
                    if (!failedSubroutineTypes.contains(routineTypeName)) {
                        val deficit = entry.value - character.resources[entry.key]
                        candidates += Pair(
                            {
                                if (isThief) StealRoutine(entry.key, deficit)
                                else BuyRoutine(entry.key, deficit)
                            },
                            PRIORITY_WORK + 300
                        )
                    }
                }
            }
        }

        //6. Corruption for power: steal resources from workplace to party member's home
        //Only attempted once a day or once a work, whichever is shorter.
        if (gState.time - corruptionTimer > ReadOnly.constInt("CorruptionTau") / ReadOnly.DT) {
            if (!failedSubroutineTypes.contains(StealRoutine::class.java.simpleName)) {
                if (gState.parties.values.any { it.leader == name }) {
                    val party = gState.parties.values.find { it.leader == name }!!
                    val rationThreshold = ReadOnly.const("StealAmountMultiplier")
                    val waterThreshold = ReadOnly.const("StealAmountMultiplier")
                    val member = party.members.find {
                        gState.characters[it]!!.resources["ration"] <= rationThreshold * (gState.characters[it]!!.reliant) || gState.characters[it]!!.resources["water"] <= waterThreshold * (gState.characters[it]!!.reliant)
                    }
                    if (member != null && subroutines.none { it is StealRoutine }) {
                        val wantedResource =
                            if (character.resources["ration"] <= rationThreshold * (character.reliant)
                            ) "ration" else "water"
                        val memberChar = gState.characters[member]!!
                        // Urgency increases as the member's resources drop further below threshold.
                        // Use a small minimum to avoid division by zero when reliant is 0.
                        val minReliant = 0.001
                        val normalizedResource = minOf(
                            memberChar.resources["ration"] / maxOf(memberChar.reliant.toDouble(), minReliant),
                            memberChar.resources["water"] / maxOf(memberChar.reliant.toDouble(), minReliant)
                        ).coerceIn(0.0, 1.0)
                        val corruptionUrgency = PRIORITY_WORK + 200 + ((1.0 - normalizedResource) * 100).toInt()
                        val capturedMember = member
                        candidates += Pair(
                            {
                                corruptionTimer = gState.time
                                StealRoutine(
                                    wantedResource,
                                    (gState.characters[capturedMember]!!.reliant + 1) * ReadOnly.const(
                                        "StealAmountMultiplier"
                                    ),
                                    capturedMember
                                )
                            },
                            corruptionUrgency
                        )
                    }
                }
            }
        }

        //6.5. Acquire luxury resources (fineFood) for feasts and campaign presents if needed.
        //Only attempted periodically.
        if (gState.time - luxuryAcquisitionTimer > ReadOnly.constInt("CorruptionTau") / ReadOnly.DT) {
            // (a) Pre-feast acquisition: division leader with upcoming meeting wants to provide a feast
            val leaderParty = gState.parties.values.find { it.leader == name && it.type == Party.Type.DIVISION }
            if (leaderParty != null) {
                val upcomingMeeting = gState.scheduledMeetings.values.firstOrNull {
                    it.involvedParty == leaderParty.name && name in it.scheduledCharacters
                }
                if (upcomingMeeting != null) {
                    val feastScore = character.stats.eScale +
                        (if ("gourmand" in character.trait) 0.5 else 0.0) +
                        (if ("charismatic" in character.trait) 0.3 else 0.0) +
                        (1.0 - leaderParty.integrityNorm).coerceIn(0.0, 1.0) * 0.6
                    if (feastScore > ReadOnly.const("FeastWillingnessThreshold")) {
                        val neededFineFood =
                            upcomingMeeting.scheduledCharacters.size.toDouble() - character.resources["fineFood"]
                        if (neededFineFood > 0 && subroutines.none { it is BuyRoutine || it is StealRoutine }) {
                            val isThief = "thief" in character.trait
                            val routineTypeName =
                                if (isThief) StealRoutine::class.java.simpleName else BuyRoutine::class.java.simpleName
                            if (!failedSubroutineTypes.contains(routineTypeName)) {
                                val capturedNeeded = neededFineFood
                                candidates += Pair(
                                    {
                                        luxuryAcquisitionTimer = gState.time
                                        if (isThief) StealRoutine("fineFood", capturedNeeded)
                                        else BuyRoutine("fineFood", capturedNeeded)
                                    },
                                    PRIORITY_WORK + 150
                                )
                            }
                        }
                    }
                }
            }
            // (b) Campaign present acquisition: character is involved in a division election campaign
            character.division?.let { division ->
                if (gState.scheduledMeetings.values.any {
                        it.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && it.involvedParty == division.name
                    } && subroutines.none { it is BuyRoutine || it is StealRoutine }
                ) {
                    val chosen = NonPlayerAgent.chooseLuxuryResource(character) { res ->
                        gState.publicPlaces.values.any { it.resources[res] > 0 }
                    }
                    if (chosen != null) {
                        val needed = chosen.giftAmount * 3.0 - character.resources[chosen.resourceName]
                        if (needed > 0) {
                            val isThief = "thief" in character.trait
                            val routineTypeName =
                                if (isThief) StealRoutine::class.java.simpleName else BuyRoutine::class.java.simpleName
                            if (!failedSubroutineTypes.contains(routineTypeName)) {
                                val capturedChosen = chosen
                                val capturedNeeded = needed
                                candidates += Pair(
                                    {
                                        luxuryAcquisitionTimer = gState.time
                                        if (isThief) StealRoutine(capturedChosen.resourceName, capturedNeeded)
                                        else BuyRoutine(capturedChosen.resourceName, capturedNeeded)
                                    },
                                    PRIORITY_WORK + 150
                                )
                            }
                        }
                    }
                }
            }
        }

        //7. Supply resource - only if I am director
        if (character.type == Character.Type.DIRECTOR && gState.time - transferResourceTimer > 60) {
            var supplyFactory: (() -> Routine)? = null
            var supplyUrgency = 0
            character.division?.divisionPlaces?.forEach { place1 ->
                place1.apparatuses.forEach { apparatus ->
                    val res = place1.resourceShortOfHourly(apparatus) ?: return@forEach
                    val resplace = gState.places.values.filter {
                        it.manager != null && it.shortestPathAndTimeTo(place, name) != null
                    }.maxByOrNull { it.resources[res] }
                    if (resplace != null && place1.name != resplace.name && resplace.manager != name) {
                        if (gState.requests.values.none {
                                !it.completed && name in it.issuedBy && it.action.let {
                                    it is OfficialResourceTransfer && it.toWhere == place1.name
                                }
                            }) {
                            if (resplace.resources[res] > apparatus.hourlyOperationResource[res] * 10) {
                                // Urgency rises as the apparatus runs lower on resources.
                                // Use a large sentinel when consumption is zero (effectively infinite hours left).
                                val infiniteHoursLeftSentinel = 1000.0
                                val hoursLeft =
                                    if (apparatus.hourlyOperationResource[res] > 0)
                                        place1.resources[res] / apparatus.hourlyOperationResource[res]
                                    else infiniteHoursLeftSentinel
                                val urgency = PRIORITY_WORK + 100 + maxOf(0, (100 - hoursLeft.toInt())).coerceAtMost(99)
                                if (urgency > supplyUrgency) {
                                    supplyUrgency = urgency
                                    val capturedResplace = resplace
                                    val capturedPlace1 = place1
                                    val capturedRes = res
                                    val capturedApparatus = apparatus
                                    supplyFactory = {
                                        transferResourceTimer = gState.time
                                        AttendPrivateMeetingRoutine(
                                            capturedResplace.manager!!,
                                            MeetingAgenda(
                                                AgendaType.REQUEST, name, attachedRequest = Request(
                                                    OfficialResourceTransfer(
                                                        capturedResplace.manager!!, capturedResplace.name,
                                                        capturedPlace1.name, Resources(
                                                            capturedRes to max(
                                                                capturedApparatus.hourlyOperationResource[capturedRes] * 10,
                                                                capturedResplace.resources[capturedRes] * 0.3
                                                            )
                                                        ), gState
                                                    ),
                                                    issuedTo = hashSetOf(capturedResplace.manager!!),
                                                    issuedBy = hashSetOf(name)
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            supplyFactory?.let { factory -> candidates += Pair(factory, supplyUrgency) }
        }

        //8. Repair Apparatus - only if I am director
        if (character.type == Character.Type.DIRECTOR && gState.time - repairApparatusTimer > 60) {
            var repairFactory: (() -> Routine)? = null
            var repairUrgency = 0
            character.division?.divisionPlaces?.forEach { place1 ->
                place1.apparatuses.forEach { apparatus ->
                    if (apparatus.durability < 70f) {
                        // Urgency rises as durability drops further below the threshold.
                        val urgency = PRIORITY_WORK + 50 + (70 - apparatus.durability).toInt()
                        if (urgency > repairUrgency) {
                            repairUrgency = urgency
                            val capturedApparatus = apparatus
                            val capturedPlace1 = place1
                            repairFactory = if ("engineer" in character.trait) {
                                {
                                    repairApparatusTimer = gState.time
                                    RepairApparatusRoutine(capturedApparatus.ID)
                                }
                            } else {
                                val engineer = gState.characters.values.filter {
                                    "engineer" in it.trait && it.name != name
                                }.maxByOrNull { gState.getMutuality(name, it.name) }
                                if (engineer != null && gState.requests.values.none {
                                        !it.completed && name in it.issuedBy && it.action.let {
                                            it is Repair && it.apparatusID == capturedApparatus.ID
                                        }
                                    }) {
                                    val capturedEngineer = engineer
                                    {
                                        repairApparatusTimer = gState.time
                                        AttendPrivateMeetingRoutine(
                                            capturedEngineer.name,
                                            MeetingAgenda(
                                                AgendaType.REQUEST, name, attachedRequest = Request(
                                                    Repair(
                                                        capturedEngineer.name, capturedPlace1.name,
                                                        capturedApparatus.ID, gState
                                                    ),
                                                    issuedTo = hashSetOf(capturedEngineer.name),
                                                    issuedBy = hashSetOf(name)
                                                )
                                            )
                                        )
                                    }
                                } else null
                            }
                        }
                    }
                }
            }
            repairFactory?.let { factory -> candidates += Pair(factory, repairUrgency) }
        }

        //9.0 Announce Information - only if I am cabinet member.
        if (character.name in gState.parties["cabinet"]!!.members && gState.time - announceInfoTimer > 60) {
            if (!failedSubroutineTypes.contains(AnnounceInfoRoutine::class.java.simpleName)) {
                gState.informations.values.firstOrNull {
                    character.name in it.knownTo && AnnounceInfo.isAnnounceable(it)
                }?.let { info ->
                    if (character.name in gState.parties["interior"]!!.directorMembers) {
                        candidates += Pair(
                            {
                                announceInfoTimer = gState.time
                                AnnounceInfoRoutine(info.name)
                            },
                            PRIORITY_WORK + 80
                        )
                    } else {
                        gState.characters.values.filter {
                            it.name in gState.parties["interior"]!!.directorMembers
                        }.maxByOrNull { gState.getMutuality(name, it.name) }?.run {
                            if (gState.requests.values.none {
                                    !it.completed && name in it.issuedBy && it.action.let {
                                        it is AnnounceInfo && it.infoKey == info.name
                                    }
                                }) {
                                AnnounceInfoRoutine.nearestPlaceWithApparatus(place, gState)?.let { announcePl ->
                                    val announcer = this
                                    candidates += Pair(
                                        {
                                            announceInfoTimer = gState.time
                                            AttendPrivateMeetingRoutine(
                                                announcer.name,
                                                MeetingAgenda(
                                                    AgendaType.REQUEST, name, attachedRequest = Request(
                                                        AnnounceInfo(announcer.name, announcePl, info.name, gState),
                                                        issuedTo = hashSetOf(announcer.name),
                                                        issuedBy = hashSetOf(name)
                                                    )
                                                )
                                            )
                                        },
                                        PRIORITY_WORK + 80
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        //9. If there is some time, prepare information
        if (subroutines.none { it is PrepareInfoRoutine }) {
            if (gState.scheduledMeetings.none {
                    val eta =
                        gState.places[it.value.place]!!.shortestPathAndTimeTo(place, name)?.second ?: return@none false
                    it.value.scheduledCharacters.contains(name) &&
                        it.value.isValidTimeToStart(gState.time + eta)
                })//If a Meeting is not soon
            {
                //If we haven't prepared info recently
                if (gState.informations.none { (_, information) ->
                        information.author == character.name && information.type == InformationType.ACTION && information.action is PrepareInfo
                            && gState.time - information.creationTime > ReadOnly.constInt("lengthOfDay") * 2
                    }) {
                    if (!failedSubroutineTypes.contains(PrepareInfoRoutine::class.java.simpleName) && try_prepare_info == 0) {
                        candidates += Pair(
                            {
                                try_prepare_info += 1
                                PrepareInfoRoutine().apply { priority = PRIORITY_WORK + 70 }
                            },
                            PRIORITY_WORK + 70
                        )
                    }
                }
            }
        }

        //10. Campaign for election if one is scheduled for the character's division and their stats support it.
        character.division?.let { division ->
            if (gState.scheduledMeetings.values.any {
                    it.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && it.involvedParty == division.name
                }
            ) {
                val campaignScore = character.stats.pScale + character.stats.rScale
                if ((campaignScore > 1.5 || "charismatic" in character.trait) &&
                    subroutines.none { it is CampaignRoutine } &&
                    !failedSubroutineTypes.contains(CampaignRoutine::class.java.simpleName)
                ) {
                    candidates += Pair({ CampaignRoutine(division.name) }, PRIORITY_WORK + 60)
                }
            }
        }

        //11. Hire a new employee if there is a vacancy in the party.
        if (!failedSubroutineTypes.contains(HireRoutine::class.java.simpleName)) {
            gState.parties.values.filter { party ->
                party.leader == name
            }.forEach { party ->
                when (party.type) {
                    Party.Type.WORKPLACE -> {
                        party.vacancyRole()?.let { role ->
                            if (subroutines.none { it is HireRoutine }) {
                                candidates += Pair(
                                    { HireRoutine(party = party.name, role = role, null) },
                                    PRIORITY_WORK + 50
                                )
                            }
                        }
                    }

                    Party.Type.DIVISION -> {
                        party.divisionPlaces.firstOrNull {
                            it.manager == null
                        }?.let { vacantPlace ->
                            if (subroutines.none { it is HireRoutine }) {
                                candidates += Pair(
                                    { HireRoutine(party = party.name, role = null, vacantPlace.name) },
                                    PRIORITY_WORK + 50
                                )
                            }
                        }
                    }
                    //Cabinet members are not hired, they are elected within their division.
                    else -> {}
                }
            }
        }

        // Select and invoke the highest-urgency candidate.
        return candidates.maxByOrNull { it.second }?.first?.invoke()
    }

    //TODO: move name to class parameter
    private fun pickMeetingRoutine(name: String, conf: Meeting): MeetingRoutine {
        meetingsAttended += gState.meetingName(conf)
        when (conf.type) {
            Meeting.MeetingType.DIVISION_DAILY_CONFERENCE -> {
                if (name != gState.parties[conf.involvedParty]!!.leader) {
                    return AttendDivisionMeetingRoutine(gState.meetingName(conf))
                } else {
                    return LeadDivisionMeetingRoutine(gState.meetingName(conf))
                }
            }

            Meeting.MeetingType.DIVISION_LEADER_ELECTION -> {
                if (name != "ctrler") {
                    return AttendDivisionElectionRoutine(gState.meetingName(conf))
                } else {
                    return LeadDivisionElectionRoutine(gState.meetingName(conf))
                }
            }

            Meeting.MeetingType.TALK -> {
                return AttendPrivateMeetingRoutine(scheduledMeetingName = gState.meetingName(conf))
            }

            Meeting.MeetingType.CABINET_DAILY_CONFERENCE -> {
                if (name != gState.parties["cabinet"]!!.leader) {
                    return AttendCabinetMeetingRoutine(gState.meetingName(conf))
                } else {
                    return LeadCabinetMeetingRoutine(gState.meetingName(conf))
                }
            }

            Meeting.MeetingType.TRIUMVIRATE_DAILY_CONFERENCE -> {
                return AttendTriumvirateRoutine(gState.meetingName(conf))
            }

            Meeting.MeetingType.BUDGET_PROPOSAL -> {
                return AttendDivisionBudgetProposalRoutine(gState.meetingName(conf))
            }

            Meeting.MeetingType.BUDGET_RESOLUTION -> {
                return AttendDivisionBudgetResolutionRoutine(gState.meetingName(conf))
            }

            else -> {
                TODO(conf.type.toString())
            }
        }
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        //7.0 If I am an employee, examine stuff in the workplace.
        if (character.type == Character.Type.EMPLOYEE) {
            if (place == workplace) {
                when (gState.places[place]!!.workplaceParty?.getRole(name)) {

                    Party.Role.TREASURER -> {
                        //Examine the resource
                        return Examine(name, place, InformationType.RESOURCES, gState)
                    }

                    Party.Role.OVERSEER -> {
                        //Examine the human resources
                        return Examine(name, place, InformationType.HUMAN_RESOURCES, gState)
                    }

                    else -> {}
                }
            }
        }
        //7.1 If I am an engineer, examine apparatus in the workplace.
        if ("engineer" in character.trait) {
            if (place == workplace) {
                //Examine apparatus
                return Examine(name, place, InformationType.APPARATUS, gState)
            }
        }

        //Wait until there is some routine available above.
        return Wait(name, place) //If no subroutine is found, wait at the current place.
    }

    override fun onSubroutineFail(subroutine: Routine) {
        when (subroutine) {
            is ExecuteRequestRoutine -> {
                // Track the specific failed request so other requests can still be executed.
                val requestName = subroutine.variables["request"]!!
                failedRequests += requestName
            }
            is MeetingRoutine -> {
                // Meeting failures are handled via meetingsAttended; do not block all meeting subroutines.
            }
            else -> {
                // For all other subroutine types, record the failure so WorkRoutine does not retry
                // them for its lifetime.
                failedSubroutineTypes += subroutine::class.java.simpleName
            }
        }
        //Never fail the work routine itself.
    }
}
