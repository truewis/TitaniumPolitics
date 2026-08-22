package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Talk

object SpeechInterpreter {
    data class SpeechLine(
        val speaker: String,
        val text: String,
        val holdSeconds: Float = DEFAULT_HOLD_SECONDS
    )

    private const val DEFAULT_HOLD_SECONDS = 1f
    private val numberRegex = Regex("^-?\\d+(\\.\\d+)?$")

    fun actionLines(action: GameAction, gameState: GameState, meeting: Meeting?): List<SpeechLine> {
        val counterpart = inferCounterpart(action, meeting)
        val rareKey = findRareKey(action.sbjCharacter, counterpart, gameState, action::class.simpleName ?: "")
        val rareLines = rareKey?.let { scriptedLines(action.sbjCharacter, counterpart, it, action.sbjCharacter) }.orEmpty()
        if (rareLines.isNotEmpty()) {
            return rareLines
        }
        return listOf(SpeechLine(action.sbjCharacter, action.generateSpeech()))
    }

    fun scriptedLines(
        primarySpeaker: String,
        otherSpeaker: String?,
        key: String,
        styleSpeaker: String = primarySpeaker,
        formatArgs: Array<out Any> = emptyArray()
    ): List<SpeechLine> {
        val indexed = indexedLines(primarySpeaker, otherSpeaker, key, styleSpeaker, formatArgs)
        if (indexed.isNotEmpty()) return indexed
        val raw = ReadOnly.scriptOrNull(key, styleSpeaker) ?: return emptyList()
        val segments = raw.split("||").map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.isEmpty()) return emptyList()
        return segments.mapNotNull { segment ->
            parseInlineSegment(primarySpeaker, otherSpeaker, segment, formatArgs)
        }
    }

    private fun indexedLines(
        primarySpeaker: String,
        otherSpeaker: String?,
        key: String,
        styleSpeaker: String,
        formatArgs: Array<out Any>
    ): List<SpeechLine> {
        val lines = arrayListOf<SpeechLine>()
        var index = 1
        while (true) {
            val lineKey = "$key-$index"
            val raw = ReadOnly.scriptOrNull(lineKey, styleSpeaker) ?: break
            val alias = ReadOnly.scriptOrNull("$lineKey-speaker", styleSpeaker)
            val duration = ReadOnly.scriptOrNull("$lineKey-duration", styleSpeaker)?.toFloatOrNull() ?: DEFAULT_HOLD_SECONDS
            val speaker = resolveSpeakerAlias(alias, primarySpeaker, otherSpeaker)
            val text = raw.safeFormat(*formatArgs)
            lines += SpeechLine(speaker, text, duration)
            index++
        }
        return lines
    }

    private fun parseInlineSegment(
        primarySpeaker: String,
        otherSpeaker: String?,
        segment: String,
        formatArgs: Array<out Any>
    ): SpeechLine? {
        val speakerSplit = segment.split(">", limit = 2)
        val (speakerAlias, body) = if (speakerSplit.size == 2) {
            speakerSplit[0].trim() to speakerSplit[1].trim()
        } else {
            "self" to segment.trim()
        }
        if (body.isEmpty()) return null
        val durationSplit = body.split("@", limit = 2)
        val text: String
        val duration: Float
        if (durationSplit.size == 2 && numberRegex.matches(durationSplit[1].trim())) {
            text = durationSplit[0].trim()
            duration = durationSplit[1].trim().toFloatOrNull() ?: DEFAULT_HOLD_SECONDS
        } else {
            text = body
            duration = DEFAULT_HOLD_SECONDS
        }
        return SpeechLine(
            resolveSpeakerAlias(speakerAlias, primarySpeaker, otherSpeaker),
            text.safeFormat(*formatArgs),
            duration
        )
    }

    private fun resolveSpeakerAlias(alias: String?, primarySpeaker: String, otherSpeaker: String?): String {
        return when (alias?.trim()?.lowercase()) {
            null, "", "self", "speaker", "sbj", "subject" -> primarySpeaker
            "other", "target", "listener", "tgt" -> otherSpeaker ?: primarySpeaker
            else -> alias
        }
    }

    private fun inferCounterpart(action: GameAction, meeting: Meeting?): String? {
        return when (action) {
            is Talk -> action.who
            is EndSpeech -> action.nextSpeaker
            is AddInfo -> action.agenda.author.takeIf { it != action.sbjCharacter }
            else -> meeting?.currentCharacters?.firstOrNull { it != action.sbjCharacter }
        }
    }

    private fun findRareKey(
        speaker: String,
        counterpart: String?,
        gameState: GameState,
        baseKey: String
    ): String? {
        if (baseKey.isBlank()) return null
        val candidates = arrayListOf<String>()
        if (!counterpart.isNullOrBlank()) {
            candidates += "$baseKey-Rare-$counterpart"
        }
        val mut = counterpart?.let { mutualityLabel(gameState, speaker, it) }
        relationshipLabels(gameState, speaker, counterpart).forEach { relation ->
            if (mut != null) {
                candidates += "$baseKey-Rare-$relation-$mut"
            }
            candidates += "$baseKey-Rare-$relation"
        }
        if (mut != null) {
            candidates += "$baseKey-Rare-$mut"
        }
        candidates += "$baseKey-Rare"
        return candidates.firstOrNull { ReadOnly.hasScript(it, speaker) }
    }

    private fun relationshipLabels(gameState: GameState, speaker: String, counterpart: String?): List<String> {
        if (counterpart.isNullOrBlank()) return emptyList()
        val labels = linkedSetOf<String>()
        val speakerChar = gameState.characters[speaker] ?: return emptyList()
        val counterpartChar = gameState.characters[counterpart] ?: return emptyList()
        val speakerWorkplace = gameState.getWorkplace(speaker)
        val counterpartWorkplace = gameState.getWorkplace(counterpart)
        if (speakerWorkplace != null && speakerWorkplace == counterpartWorkplace) {
            labels += "sameWorkplace"
            val leader = speakerWorkplace.workplaceParty?.leader
            when {
                leader == speaker -> labels += "manager"
                leader == counterpart -> labels += "directReport"
                else -> labels += "coworker"
            }
        }
        if (speakerChar.division != null && speakerChar.division == counterpartChar.division) {
            labels += "sameDivision"
            val leader = speakerChar.division?.leader
            when {
                leader == speaker -> labels += "manager"
                leader == counterpart -> labels += "directReport"
                else -> labels += "coworker"
            }
        }
        if (labels.isEmpty()) labels += "stranger"
        return labels.toList()
    }

    private fun mutualityLabel(gameState: GameState, speaker: String, counterpart: String): String {
        return when (gameState.getMutNorm(speaker, counterpart)) {
            in 0.25..1.0 -> "positive"
            in -1.0..-0.25 -> "negative"
            else -> "neutral"
        }
    }

    private fun String.safeFormat(vararg args: Any): String {
        if (args.isEmpty()) return this
        return try {
            format(*args)
        } catch (_: Exception) {
            this
        }
    }
}
