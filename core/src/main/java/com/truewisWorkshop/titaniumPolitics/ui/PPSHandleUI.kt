package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.CabinetWindowContainerUI
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d

/**
 * PPS (Personnel Protection System) cabinet handle.
 *
 * Extends [CabinetWindowContainerUI] with three coloured status badges —
 * Gas, Temperature, and Power — that reflect current life-support conditions.
 *
 * Behaviour:
 * - When all conditions are GREEN the handle fades to near-invisible; a mouse
 *   hover temporarily reveals it.
 * - When any condition is ORANGE or RED the handle stays fully visible and the
 *   affected marker blinks.
 */
class PPSHandleUI(
    private val ppsWindow: PPSWindowUI,
    xOffset: Float,
    yOffset: Float
) : CabinetWindowContainerUI(
    title = "PPS",
    content = ppsWindow,
    xOffset = xOffset,
    yOffset = yOffset,
    openAction = { ppsWindow.refresh() }
) {

    private val gasMarker: Image
    private val temperatureMarker: Image
    private val powerMarker: Image

    init {
        // Add three status badge markers to the handle at vertical offsets
        gasMarker = addStatusMarker("PPSGasMarker", Color.GREEN, 35f, 440f)
        temperatureMarker = addStatusMarker("PPSTempMarker", Color.GREEN, 35f, 370f)
        powerMarker = addStatusMarker("PPSPowerMarker", Color.GREEN, 35f, 300f)

        // Small rotated text labels beside each badge
        addStatusLabel(ReadOnly.prop("PPSHandleUI-Gas"), 65f, 440f)
        addStatusLabel(ReadOnly.prop("PPSHandleUI-Temperature"), 65f, 370f)
        addStatusLabel(ReadOnly.prop("PPSHandleUI-Power"), 65f, 300f)

        // Update markers whenever the window refreshes
        ppsWindow.onStatusChanged += { gas, temp, power ->
            updateConditions(gas, temp, power)
        }

        // Start near-invisible — all conditions default to GREEN
        color.a = 0.15f

        // Hover: reveal handle temporarily when all is green
        titleLabel.addListener(object : ClickListener() {
            override fun enter(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                fromActor: com.badlogic.gdx.scenes.scene2d.Actor?
            ) {
                this@PPSHandleUI.addAction(Actions.fadeIn(0.25f))
                super.enter(event, x, y, pointer, fromActor)
            }

            override fun exit(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                toActor: com.badlogic.gdx.scenes.scene2d.Actor?
            ) {
                val allGreen =
                    ppsWindow.gasStatus == PPSWindowUI.ConditionStatus.GREEN &&
                        ppsWindow.temperatureStatus == PPSWindowUI.ConditionStatus.GREEN &&
                        ppsWindow.powerStatus == PPSWindowUI.ConditionStatus.GREEN
                if (allGreen && !isOpen) {
                    this@PPSHandleUI.addAction(Actions.fadeOut(0.5f))
                }
                super.exit(event, x, y, pointer, toActor)
            }
        })
    }

    // ── Factory helpers ─────────────────────────────────────────────────────

    private fun addStatusMarker(markerName: String, color: Color, x: Float, y: Float): Image {
        val marker = scene2d.image("BadgeRound") {
            name = markerName
            this.color = color
            setSize(22f, 22f)
        }
        addActor(marker)
        marker.setPosition(x, y)
        return marker
    }

    private fun addStatusLabel(text: String, x: Float, y: Float): Label {
        val lbl = scene2d.label(text, "docTitle") {
            setFontScale(0.28f)
            color = Color.LIGHT_GRAY
            setAlignment(Align.left)
        }
        val container = scene2d.container(lbl) {
            fill()
            setSize(150f, 30f)
            isTransform = true
        }
        addActor(container)
        container.setPosition(x, y + 1f)
        return lbl
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Update the three status markers and handle opacity based on the current
     * life-support conditions.
     */
    fun updateConditions(
        gas: PPSWindowUI.ConditionStatus,
        temperature: PPSWindowUI.ConditionStatus,
        power: PPSWindowUI.ConditionStatus
    ) {
        applyMarkerColor(gasMarker, gas)
        applyMarkerColor(temperatureMarker, temperature)
        applyMarkerColor(powerMarker, power)

        val allGreen = gas == PPSWindowUI.ConditionStatus.GREEN &&
            temperature == PPSWindowUI.ConditionStatus.GREEN &&
            power == PPSWindowUI.ConditionStatus.GREEN

        if (!allGreen) {
            addAction(Actions.fadeIn(0.3f))
            if (gas != PPSWindowUI.ConditionStatus.GREEN) startBlink(gasMarker)
            if (temperature != PPSWindowUI.ConditionStatus.GREEN) startBlink(temperatureMarker)
            if (power != PPSWindowUI.ConditionStatus.GREEN) startBlink(powerMarker)
        } else if (!isOpen) {
            gasMarker.clearActions()
            temperatureMarker.clearActions()
            powerMarker.clearActions()
            gasMarker.color.a = 1f
            temperatureMarker.color.a = 1f
            powerMarker.color.a = 1f
            addAction(Actions.fadeOut(1.0f))
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun applyMarkerColor(marker: Image, status: PPSWindowUI.ConditionStatus) {
        val targetColor = when (status) {
            PPSWindowUI.ConditionStatus.GREEN -> Color.GREEN
            PPSWindowUI.ConditionStatus.ORANGE -> Color.ORANGE
            PPSWindowUI.ConditionStatus.RED -> Color.RED
        }
        // Preserve current alpha while updating RGB
        val currentAlpha = marker.color.a
        marker.color.set(targetColor).also { it.a = currentAlpha }
    }

    private fun startBlink(marker: Image) {
        marker.clearActions()
        marker.addAction(
            Actions.forever(
                Actions.sequence(
                    Actions.delay(0.5f),
                    Actions.alpha(0f, 0.2f),
                    Actions.alpha(1f, 0.2f)
                )
            )
        )
    }
}
