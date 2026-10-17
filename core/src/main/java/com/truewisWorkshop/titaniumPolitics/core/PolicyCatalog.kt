package com.titaniumPolitics.game.core

import kotlin.math.abs
import kotlin.math.max

data class PolicyDemographics(
    val artificialists: Set<String>,
    val spiritualists: Set<String>,
    val atheists: Set<String>,
    val engineers: Set<String>,
    val soldiers: Set<String>,
    val administrators: Set<String>,
    val miners: Set<String>,
    val laborers: Set<String>,
    val hasFamily: Set<String>,
    val young: Set<String>,
    val old: Set<String>,
    val progressives: Set<String>,
    val conservatives: Set<String>
)

object PolicyCatalog {
    const val WORKHOUR_LIMIT_MAX_HOURS = 8
    const val POLICY_STANCE_THRESHOLD = 0.15
    private const val POLICY_PROPOSAL_THRESHOLD = 0.15

    val categories = linkedMapOf(
        "religion" to listOf(
            "banReligiousPractices",
            "onlyReligiousPracticesArtificialist",
            "onlyReligiousPracticesSpiritualist"
        ),
        "union" to listOf("banUnion"),
        "incentive" to listOf(
            "engineerIncentive",
            "administratorIncentive",
            "soldierIncentive",
            "minerIncentive",
            "laborerIncentive"
        ),
        "safety" to listOf("workhourLimit", "lockoutExperiments", "paternityLeave"),
        "social" to listOf("seniority", "jobStability", "noTitles")
    )

    private val policiesByPartyType = mapOf(
        Party.Type.WORKPLACE to setOf(
            "banUnion",
            "engineerIncentive",
            "administratorIncentive",
            "soldierIncentive",
            "minerIncentive",
            "laborerIncentive",
            "workhourLimit",
            "lockoutExperiments",
            "paternityLeave"
        ),
        Party.Type.DIVISION to setOf(
            "banReligiousPractices",
            "onlyReligiousPracticesArtificialist",
            "onlyReligiousPracticesSpiritualist",
            "seniority",
            "jobStability",
            "noTitles"
        )
    )

    fun categoryOf(policy: String): String? =
        categories.entries.firstOrNull { policy in it.value }?.key

    fun isKnown(policy: String): Boolean = categories.values.any { policy in it }

    fun isAllowed(policy: String, partyType: Party.Type): Boolean =
        policy in policiesByPartyType[partyType].orEmpty()

    fun categoriesFor(partyType: Party.Type): Map<String, List<String>> {
        val allowedPolicies = policiesByPartyType[partyType].orEmpty()
        return categories.mapValues { (_, policies) -> policies.filter { it in allowedPolicies } }
            .filterValues { it.isNotEmpty() }
    }

    fun demographics(party: Party): PolicyDemographics {
        val members = party.members
        fun withTrait(trait: String) = members.filter {
            trait in party.parent.characters[it]!!.trait
        }.toSet()

        return PolicyDemographics(
            artificialists = withTrait("artificialist"),
            spiritualists = withTrait("spiritualist"),
            atheists = withTrait("atheist"),
            engineers = withTrait("engineer"),
            soldiers = withTrait("soldier"),
            administrators = withTrait("administrator"),
            miners = withTrait("miner"),
            laborers = members.filter {
                party.parent.characters[it]!!.type == Character.Type.ANON
            }.toSet(),
            hasFamily = members.filter {
                val character = party.parent.characters[it]!!
                character.reliant > 0 && character.type != Character.Type.ANON
            }.toSet(),
            young = members.filter { party.parent.characters[it]!!.age < 30 }.toSet(),
            old = members.filter { party.parent.characters[it]!!.age > 50 }.toSet(),
            progressives = members.filter { party.parent.characters[it]!!.stats.riskTaking > 12 }.toSet(),
            conservatives = members.filter { party.parent.characters[it]!!.stats.riskTaking < 8 }.toSet()
        )
    }

    fun affectedCharacters(party: Party?, policy: String): Pair<List<String>, List<String>> {
        if (party == null) return Pair(emptyList(), emptyList())
        val groups = demographics(party)
        val members = party.members
        val positive: Set<String>
        val negative: Set<String>
        when (policy) {
            "banReligiousPractices" -> {
                positive = groups.atheists
                negative = groups.spiritualists + groups.artificialists
            }
            "onlyReligiousPracticesArtificialist" -> {
                positive = groups.artificialists
                negative = groups.spiritualists
            }
            "onlyReligiousPracticesSpiritualist" -> {
                positive = groups.spiritualists
                negative = groups.artificialists
            }
            "banUnion" -> {
                positive = emptySet()
                negative = groups.laborers
            }
            "engineerIncentive" -> {
                positive = groups.engineers
                negative = members - groups.engineers
            }
            "administratorIncentive" -> {
                positive = groups.administrators
                negative = members - groups.administrators
            }
            "soldierIncentive" -> {
                positive = groups.soldiers
                negative = members - groups.soldiers
            }
            "minerIncentive" -> {
                positive = groups.miners
                negative = members - groups.miners
            }
            "laborerIncentive" -> {
                positive = groups.laborers
                negative = members - groups.laborers
            }
            "workhourLimit", "lockoutExperiments", "jobStability" -> {
                positive = groups.conservatives
                negative = groups.progressives
            }
            "paternityLeave" -> {
                positive = groups.hasFamily
                negative = members - groups.hasFamily
            }
            "seniority" -> {
                positive = groups.old
                negative = groups.young
            }
            "noTitles" -> {
                positive = groups.laborers.intersect(groups.progressives)
                negative = groups.conservatives - groups.laborers
            }
            else -> return Pair(emptyList(), emptyList())
        }
        return Pair(positive.sorted(), negative.sorted())
    }

    fun preferenceScore(
        party: Party,
        policy: String,
        characterName: String,
        agendaAuthor: String? = party.leader
    ): Double {
        if (!isAllowed(policy, party.type) || characterName !in party.members) return 0.0
        val personalPreference = directPreference(party, policy, characterName)
        val author = agendaAuthor?.takeIf { it != characterName && it in party.members }
            ?: return personalPreference
        val authorPreference = directPreference(party, policy, author)
        val relationship = party.parent.getMutNorm(characterName, author)
        return (personalPreference * 0.8 + authorPreference * relationship * 0.2)
            .coerceIn(-1.0, 1.0)
    }

    fun situationPriority(party: Party, policy: String): Double {
        if (!isAllowed(policy, party.type)) return 0.0
        val groups = demographics(party)
        val memberCount = party.members.size
        if (memberCount == 0) return 0.0
        fun share(members: Set<String>) = members.size.toDouble() / memberCount

        val positiveShare: Double
        val negativeShare: Double
        when (policy) {
            "banReligiousPractices" -> {
                positiveShare = share(groups.atheists)
                negativeShare = share(groups.spiritualists + groups.artificialists)
            }
            "onlyReligiousPracticesArtificialist" -> {
                positiveShare = share(groups.artificialists)
                negativeShare = share(groups.spiritualists)
            }
            "onlyReligiousPracticesSpiritualist" -> {
                positiveShare = share(groups.spiritualists)
                negativeShare = share(groups.artificialists)
            }
            "banUnion" -> return -share(groups.laborers)
            "engineerIncentive" -> return incentivePriority(share(groups.engineers))
            "administratorIncentive" -> return incentivePriority(share(groups.administrators))
            "soldierIncentive" -> return incentivePriority(share(groups.soldiers))
            "minerIncentive" -> return incentivePriority(share(groups.miners))
            "laborerIncentive" -> return incentivePriority(share(groups.laborers))
            "workhourLimit" -> {
                val places = operationalPlaces(party)
                val overlongShift = places.maxOfOrNull {
                    (it.workHoursLength - WORKHOUR_LIMIT_MAX_HOURS).toDouble() /
                            WORKHOUR_LIMIT_MAX_HOURS
                } ?: 0.0
                return max(overlongShift, operationalRisk(places) * 0.5).coerceIn(-1.0, 1.0)
            }
            "lockoutExperiments" -> return operationalRisk(operationalPlaces(party))
            "paternityLeave" -> return share(groups.hasFamily)
            "seniority" -> {
                positiveShare = share(groups.old)
                negativeShare = share(groups.young)
            }
            "jobStability" -> return (1.0 - 2.0 * party.integrityNorm).coerceIn(-1.0, 1.0)
            "noTitles" -> {
                positiveShare = share(groups.progressives)
                negativeShare = share(groups.conservatives)
            }
            else -> return 0.0
        }
        return (positiveShare - negativeShare).coerceIn(-1.0, 1.0)
    }

    fun bestProposal(
        party: Party,
        leaderName: String,
        reportNames: Collection<String>
    ): String? {
        if (party.leader != leaderName || leaderName !in party.members) return null
        val activeCategories = party.policies.mapNotNull(::categoryOf).toSet()
        val candidates = categoriesFor(party.type).values.flatten()
            .filter { it !in party.policies && categoryOf(it) !in activeCategories }
            .map { policy ->
                val leaderPreference = directPreference(party, policy, leaderName)
                val reports = reportNames
                    .filter { it != leaderName && it in party.members }
                    .map { report ->
                        val trust = (party.parent.getMutNorm(leaderName, report) + 1.0) / 2.0
                        val weight = 0.5 + trust * 0.5
                        preferenceScore(party, policy, report, leaderName) to weight
                    }
                val expectedSupport = if (reports.isEmpty()) 0.0 else
                    reports.sumOf { it.first * it.second } / reports.sumOf { it.second }
                val score = 0.45 * leaderPreference +
                        0.30 * expectedSupport +
                        0.25 * situationPriority(party, policy)
                policy to score
            }
            .maxWithOrNull(compareBy<Pair<String, Double>> { it.second }.thenBy { it.first })
            ?: return null
        return candidates.first.takeIf { candidates.second >= POLICY_PROPOSAL_THRESHOLD }
    }

    fun informationImpact(
        party: Party,
        policy: String,
        info: Information
    ): Double {
        if (!isAllowed(policy, party.type)) return 0.0
        val targetCharacter = info.tgtCharacter
            ?: info.action?.sbjCharacter
            ?: info.auxCharacter
        if (targetCharacter != null && targetCharacter in party.members) {
            val (beneficiaries, harmed) = affectedCharacters(party, policy)
            if (targetCharacter in beneficiaries) return 1.0
            if (targetCharacter in harmed) return -1.0
            if (policy == "banUnion" &&
                "administrator" in party.parent.characters[targetCharacter]!!.trait
            ) return 0.5
        }

        if (policy !in setOf("workhourLimit", "lockoutExperiments")) return 0.0
        val place = party.parent.places[info.tgtPlace] ?: return 0.0
        if (place !in operationalPlaces(party)) return 0.0
        return when (info.type) {
            InformationType.CASUALTY ->
                (info.amount.toDouble() / (info.amount + 3.0)).coerceIn(0.0, 1.0)
            InformationType.ACCIDENT -> 0.75
            InformationType.SOUND -> 0.25
            InformationType.APPARATUS -> {
                val durability = info.variables["durability"] ?: return 0.0
                val durabilityImpact = ((70.0 - durability) / 30.0).coerceIn(-1.0, 1.0)
                val danger = info.variables["danger"] ?: 0.0
                val graveDanger = info.variables["graveDanger"] ?: 0.0
                max(durabilityImpact, (danger + graveDanger) * 100.0).coerceIn(-1.0, 1.0)
            }
            InformationType.HUMAN_RESOURCES -> {
                val idealWorkers = place.apparatuses.sumOf { it.idealWorker }
                if (idealWorkers == 0) 0.0 else {
                    val staffingDeviation = abs(info.amount.toDouble() / idealWorkers - 1.0)
                    if (staffingDeviation < 0.1) -0.1 else staffingDeviation.coerceIn(0.0, 1.0)
                }
            }
            else -> 0.0
        }
    }

    private fun directPreference(party: Party, policy: String, characterName: String): Double {
        if (policy == "banUnion" &&
            "administrator" in party.parent.characters[characterName]!!.trait
        ) return 0.75
        val (beneficiaries, harmed) = affectedCharacters(party, policy)
        return when (characterName) {
            in beneficiaries -> 1.0
            in harmed -> -1.0
            else -> 0.0
        }
    }

    private fun incentivePriority(qualifiedShare: Double): Double =
        if (qualifiedShare == 0.0) -0.5 else (qualifiedShare * 2.0 - 0.25).coerceIn(-1.0, 1.0)

    private fun operationalPlaces(party: Party): List<Place> = when (party.type) {
        Party.Type.WORKPLACE -> listOfNotNull(party.parent.places[party.workplace.name])
        Party.Type.DIVISION -> party.divisionPlaces.toList()
        else -> emptyList()
    }

    private fun operationalRisk(places: List<Place>): Double {
        val apparatuses = places.flatMap { it.apparatuses }
        if (apparatuses.isEmpty()) return 0.0
        val durabilityMax = ReadOnly.const("DurabilityMax")
        return apparatuses.map { apparatus ->
            val durabilityRisk = if (durabilityMax <= 0.0) 0.0
            else (1.0 - apparatus.durability / durabilityMax).coerceIn(0.0, 1.0)
            val staffingRisk = if (apparatus.idealWorker <= 0) 0.0 else
                (abs(apparatus.plannedWorker - apparatus.idealWorker).toDouble() /
                        apparatus.idealWorker).coerceIn(0.0, 1.0)
            max(durabilityRisk, staffingRisk)
        }.average()
    }

    fun migrateLegacyPolicies(parties: Collection<Party>) {
        parties.forEach { party ->
            if (party.policies.remove("jobSecurity")) {
                party.policies.add("jobStability")
            }
        }
    }

    fun enforceWorkHourLimits(gameState: GameState) {
        val divisionsWithLimit = gameState.parties.values
            .filter { it.type == Party.Type.DIVISION && "workhourLimit" in it.policies }
            .map { it.name }
            .toSet()
        val workplacesWithLimit = gameState.parties.values
            .filter { it.type == Party.Type.WORKPLACE && "workhourLimit" in it.policies }
            .map { it.name }
            .toSet()

        gameState.places.values
            .filter { place ->
                (place.responsibleDivision?.let { it in divisionsWithLimit } == true ||
                    place.workplaceParty?.name?.let { it in workplacesWithLimit } == true) &&
                        place.workHoursLength > WORKHOUR_LIMIT_MAX_HOURS
            }
            .forEach { place ->
                place.workHoursEnd = minOf(
                    place.workHoursStart + WORKHOUR_LIMIT_MAX_HOURS,
                    ReadOnly.constInt("lengthOfDay") / ReadOnly.IDTH
                )
            }
    }
}
