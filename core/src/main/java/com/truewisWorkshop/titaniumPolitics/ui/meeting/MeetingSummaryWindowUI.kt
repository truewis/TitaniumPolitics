package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin

class MeetingSummaryWindowUI(
    private val gameState: GameState,
    private val meeting: Meeting,
    private val previousMutuality: Map<Pair<String, String>, Double>,
    private val knownInformationBeforeMeeting: Set<String>,
) : Window(ReadOnly.prop("MeetingSummaryUI-title"), Scene2DSkin.defaultSkin) {

    init {
        val content = Table(Scene2DSkin.defaultSkin)
        val pane = ScrollPane(content, Scene2DSkin.defaultSkin).apply {
            setScrollingDisabled(true, false)
            setFadeScrollBars(false)
        }
        add(pane).size(1300f, 700f).pad(15f)
        populate(content)
        pack()
        isMovable = true
    }

    private fun populate(content: Table) {
        addSectionTitle(content, ReadOnly.prop("MeetingSummaryUI-mutualityChanges"))
        val mutualityRows = previousMutuality.entries.mapNotNull { (pair, before) ->
            val after = gameState.getMutuality(pair.first, pair.second)
            val delta = after - before
            if (kotlin.math.abs(delta) < 1e-2) null else Triple(pair, before, after)
        }.sortedByDescending { kotlin.math.abs(it.third - it.second) }
        if (mutualityRows.isEmpty()) {
            addEmptyRow(content)
        } else {
            addHeader(content, ReadOnly.prop("MeetingSummaryUI-from"), ReadOnly.prop("MeetingSummaryUI-to"), ReadOnly.prop("MeetingSummaryUI-change"))
            mutualityRows.forEach { (pair, before, after) ->
                val change = after - before
                content.add(label(ReadOnly.charProp(pair.first))).left().pad(4f)
                content.add(label(ReadOnly.charProp(pair.second))).left().pad(4f)
                content.add(label(String.format("%.1f -> %.1f (%+.1f)", before, after, change))).left().pad(4f)
                content.row()
            }
        }

        addSectionTitle(content, ReadOnly.prop("MeetingSummaryUI-newInformation"))
        val newInfo = meeting.deployedInformationKeys.mapNotNull { gameState.informations[it] }
            .filter { gameState.playerName in it.knownTo && it.name !in knownInformationBeforeMeeting }
            .sortedByDescending { it.creationTime }
        if (newInfo.isEmpty()) {
            addEmptyRow(content)
        } else {
            addHeader(content, ReadOnly.prop("InformationViewUI-Time"), ReadOnly.prop("InformationViewUI-Description"))
            newInfo.forEach { information ->
                content.add(label(GameState.formatTime(information.creationTime))).left().pad(4f)
                content.add(label(information.simpleDescription(), true)).growX().left().pad(4f)
                content.row()
            }
        }

        addSectionTitle(content, ReadOnly.prop("MeetingSummaryUI-passedAgendas"))
        addAgendaRows(content, meeting.passedAgendas, Color(0.3f, 0.85f, 0.3f, 1f))

        addSectionTitle(content, ReadOnly.prop("MeetingSummaryUI-ignoredAgendas"))
        addAgendaRows(content, meeting.ignoredAgendas, Color(0.95f, 0.35f, 0.35f, 1f))
    }

    private fun addAgendaRows(content: Table, agendas: List<MeetingAgenda>, color: Color) {
        if (agendas.isEmpty()) {
            addEmptyRow(content)
            return
        }
        agendas.forEach { agenda ->
            content.add(label(agendaTitle(agenda))).growX().left().pad(4f)
            content.add(label(ReadOnly.charProp(agenda.author), false, color)).left().pad(4f)
            content.row()
        }
    }

    private fun agendaTitle(agenda: MeetingAgenda): String {
        val typeTitle = when (agenda.type) {
            AgendaType.PROOF_OF_WORK -> ReadOnly.prop("NewAgendaUI-proofOfWork")
            AgendaType.PRAISE -> ReadOnly.prop("NewAgendaUI-praise")
            AgendaType.DENOUNCE -> ReadOnly.prop("NewAgendaUI-denounce")
            AgendaType.PRAISE_PARTY -> ReadOnly.prop("NewAgendaUI-praiseParty")
            AgendaType.DENOUNCE_PARTY -> ReadOnly.prop("NewAgendaUI-denounceParty")
            AgendaType.REQUEST -> ReadOnly.prop("NewAgendaUI-request")
            AgendaType.PROMISE -> ReadOnly.prop("NewAgendaUI-promise")
            AgendaType.NOMINATE -> ReadOnly.prop("NewAgendaUI-nominate")
            AgendaType.BUDGET_PROPOSAL -> ReadOnly.prop("NewAgendaUI-budgetProposal")
            AgendaType.BUDGET_RESOLUTION -> ReadOnly.prop("NewAgendaUI-budgetResolution")
            AgendaType.APPOINT_MEETING -> ReadOnly.prop("NewAgendaUI-scheduleMeeting")
            AgendaType.FIRE_MANAGER -> ReadOnly.prop("NewAgendaUI-fireManager")
        }
        val subject = agenda.subjectParams["character"]?.let { " (${ReadOnly.charProp(it)})" }
            ?: agenda.subjectParams["party"]?.let { " (${it})" }
            ?: ""
        return typeTitle + subject
    }

    private fun addSectionTitle(content: Table, text: String) {
        content.add(label(text, false, Color(1f, 1f, 1f, 1f), 0.45f)).colspan(3).growX().left().padTop(12f).padBottom(4f)
        content.row()
    }

    private fun addHeader(content: Table, first: String, second: String, third: String? = null) {
        content.add(label(first, false, Color.LIGHT_GRAY, 0.32f)).left().pad(4f)
        content.add(label(second, false, Color.LIGHT_GRAY, 0.32f)).left().pad(4f)
        if (third != null) {
            content.add(label(third, false, Color.LIGHT_GRAY, 0.32f)).left().pad(4f)
        }
        content.row()
    }

    private fun addEmptyRow(content: Table) {
        content.add(label(ReadOnly.prop("MeetingSummaryUI-none"), true)).colspan(3).growX().left().pad(4f)
        content.row()
    }

    private fun label(
        text: String,
        wrap: Boolean = false,
        color: Color = Color.WHITE,
        scale: Float = 0.28f
    ): Label {
        return Label(text, Scene2DSkin.defaultSkin, "docTitle").also {
            it.setAlignment(Align.left)
            it.setFontScale(scale)
            it.color = color
            it.wrap = wrap
        }
    }
}
