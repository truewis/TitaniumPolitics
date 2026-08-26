package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import ktx.scene2d.*


class PrepareInfoUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("PrepareInfoTitle", gameState, actionCallback) {

    /** Tracks selected info key per priority-agenda slot index. */
    private val slotSelections = hashMapOf<Int, String>()
    private val dataTable = Table(Scene2DSkin.defaultSkin)

    init {
        val scrollPane = ScrollPane(dataTable)
        scrollPane.setScrollingDisabled(true, false)
        content.add(scene2d.table {
            add(scrollPane).grow()
            row()
            add(this@PrepareInfoUI.submitButton)
        }).grow()
        refresh()
    }

    fun refresh() {
        slotSelections.clear()
        dataTable.clear()

        val priorityAgendas = computePriorityAgendas()
        var addedAny = false

        priorityAgendas.forEachIndexed { idx, (agendaType, source) ->
            val infoOptions = getInfoOptions(agendaType, source)
            if (infoOptions.isEmpty()) return@forEachIndexed
            addedAny = true

            dataTable.add(scene2d.label(agendaTypeLabel(agendaType), "docTitle") {
                setFontScale(0.3f)
                color = Color.WHITE
                setAlignment(Align.left)
            }).growX().padTop(12f).padLeft(10f)
            dataTable.row()

            val group = scene2d.buttonGroup(0, 1) {
                infoOptions.forEach { (key, eff) ->
                    button("check") {
                        it.size(450f, 90f).pad(4f)
                        label(
                            this@PrepareInfoUI.gameState.informations[key]!!.simpleDescription(),
                            "docTitle"
                        ) {
                            it.size(350f, 50f)
                            setAlignment(Align.center)
                            setFontScale(0.2f)
                            color = Color.WHITE
                            wrap = true
                        }
                        row()
                        label("%.1f %%".format(eff), "docTitle") {
                            it.size(150f, 40f)
                            setAlignment(Align.center)
                            setFontScale(0.2f)
                            color = if (eff >= 0) Color(0.4f, 1f, 0.4f, 1f) else Color(1f, 0.4f, 0.4f, 1f)
                        }
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent?, actor: Actor?) {
                                if (this@button.isChecked) {
                                    this@PrepareInfoUI.slotSelections[idx] = key
                                } else {
                                    this@PrepareInfoUI.slotSelections.remove(idx)
                                }
                                this@PrepareInfoUI.updateSubmit()
                            }
                        })
                    }
                }
            }
            dataTable.add(group).pad(4f)
            dataTable.row()
        }

        if (!addedAny) {
            dataTable.add(scene2d.label(ReadOnly.prop("PrepareInfoUI-NoAgendas"), "docTitle") {
                setFontScale(0.3f)
                color = Color.WHITE
            }).growX().pad(20f)
        }

        updateSubmit()
    }

    private fun updateSubmit() {
        val selected = ArrayList(slotSelections.values)
        submitButton.refresh(
            PrepareInfo(gameState.playerName, gameState.player.place.name).also {
                it.newSetOfPrepInfoKeys = selected
            }
        )
    }

    /** Collect at most 3 priority agendas: first from today's meetings, then from active quests. */
    private fun computePriorityAgendas(): List<Pair<AgendaType, Any?>> {
        val result = arrayListOf<Pair<AgendaType, Any?>>()
        val today = ReadOnly.toDays(gameState.time)

        // 1. One per today's meeting
        gameState.scheduledMeetings.values
            .filter { meeting ->
                ReadOnly.toDays(meeting.time) == today &&
                        gameState.playerName in meeting.scheduledCharacters
            }
            .forEach { meeting ->
                if (result.size >= 3) return@forEach
                val agendaType = meeting.agendaTypeForPlayer()
                    ?: return@forEach
                result.add(Pair(agendaType, meeting))
            }

        // 2. One per existing quest that has an associated agenda type
        gameState.eventSystem.activeQuests
            .filter { it.agendaType != null }
            .forEach { quest ->
                if (result.size >= 3) return@forEach
                result.add(Pair(quest.agendaType!!, quest))
            }

        return result
    }

    /**
     * Return at most two options for the given agenda type: highest positive effectivity and
     * lowest negative effectivity (omitted when they don't exist or are the same entry).
     */
    private fun getInfoOptions(agendaType: AgendaType, source: Any?): List<Pair<String, Double>> {
        val playerInfoKeys = gameState.informations.filter { gameState.playerName in it.value.knownTo }.keys
        val ranked = playerInfoKeys.mapNotNull { key ->
            val eff = computeEffectivityForType(agendaType, gameState.informations[key]!!, source)
            if (eff != 0.0) Pair(key, eff) else null
        }

        val bestPositive = ranked.filter { it.second > 0 }.maxByOrNull { it.second }
        val worstNegative = ranked.filter { it.second < 0 }.minByOrNull { it.second }

        return listOfNotNull(
            bestPositive,
            worstNegative?.takeIf { it.first != bestPositive?.first }
        )
    }

    /**
     * Simplified effectivity used before a meeting starts. Handles the cases where
     * the source is a [Meeting] (for PROOF_OF_WORK) or a [Quest] (for character-targeted
     * agenda types such as PRAISE / DENOUNCE).
     */
    private fun computeEffectivityForType(agendaType: AgendaType, info: Information, source: Any?): Double {
        val player = gameState.player
        return when (agendaType) {
            AgendaType.PROOF_OF_WORK -> {
                val meeting = source as? Meeting
                val meetingChars = meeting?.scheduledCharacters ?: hashSetOf()
                if (player.executedRequests.any { reqKey ->
                        val req = gameState.requests[reqKey] ?: return@any false
                        req.action.isProofOfWork(info) &&
                                req.issuedBy.any { it in meetingChars }
                    }) 5.0 * player.stats.lScale else 0.0
            }

            AgendaType.PRAISE, AgendaType.NOMINATE -> {
                val tgtChar = (source as? Quest)?.tgtCharacters?.firstOrNull() ?: return 0.0
                (gameState.characters[tgtChar]?.infoPreference(info) ?: 0.0) * player.stats.eScale
            }

            AgendaType.DENOUNCE -> {
                val tgtChar = (source as? Quest)?.tgtCharacters?.firstOrNull() ?: return 0.0
                -(gameState.characters[tgtChar]?.infoPreference(info) ?: 0.0) * player.stats.pScale
            }

            else -> 0.0
        }
    }

    private fun agendaTypeLabel(agendaType: AgendaType): String =
        ReadOnly.prop("PrepareInfoUI-AgendaType-${agendaType.name}")
}
