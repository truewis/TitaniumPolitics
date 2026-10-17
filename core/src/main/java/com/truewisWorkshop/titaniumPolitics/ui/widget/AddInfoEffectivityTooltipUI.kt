package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.AgendaEffectivityEvaluation
import com.titaniumPolitics.game.core.AgendaEffectivityReason
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

class AddInfoEffectivityTooltipUI(
    evaluation: AgendaEffectivityEvaluation,
    reasons: List<AgendaEffectivityReason>,
    width: Float = 350f,
    height: Float = 300f
) : Tooltip<Table>(scene2d.table {
    addActor(scene2d.image("TooltipShadow10p") {
        it.width = width + 100f
        it.height = height + 100f
        it.x = -50f
        it.y = -50f
        setColor(0f, 0f, 0f, 0.7f)
        touchable = Touchable.disabled
    })
    stack {
        it.size(width, height)
        image("BlackPx")
        image("NoiseBackground") {
            setColor(1f, 1f, 1f, 0.1f)
        }
        image("PanelDottedShade700x700") {
            setColor(0f, 0f, 0f, 1f)
        }
        table {
            val padding = 8f
            pad(padding)
            label(ReadOnly.prop("AddInfoEffectivityTooltipUI-Title"), "description") {
                it.width(width - padding * 2)
                setFontScale(0.23f)
                setAlignment(Align.left)
                wrap = true
            }
            row()
            label(
                ReadOnly.prop("AddInfoEffectivityTooltipUI-Score").format(evaluation.effectivity),
                "description"
            ) {
                it.width(width - padding * 2)
                setFontScale(0.2f)
                setAlignment(Align.left)
            }
            row()
            val reasonText = buildString {
                reasons.forEachIndexed { index, reason ->
                    append(if (reason.effectivity > 0.0) "{COLOR=GREEN}" else "{COLOR=RED}")
                    val template = ReadOnly.prop(reason.localizationKey)
                    append(
                        if (reason.arguments.isEmpty()) template
                        else template.format(*reason.arguments.toTypedArray())
                    )
                    if (index < reasons.lastIndex) append('\n')
                }
            }
            add(TypingLabel("", Scene2DSkin.defaultSkin, "description").apply {
                setText(reasonText)
                skipToTheEnd()
                setFontScale(0.2f)
                setAlignment(Align.topLeft)
                wrap = true
            }).size(width - padding * 2, height - 70f).top().left()
        }
        image("Stroke500x500") {
            setColor(0f, 0f, 0f, 1f)
        }
    }
}) {
    init {
        manager.initialTime = 0.5f
    }
}
