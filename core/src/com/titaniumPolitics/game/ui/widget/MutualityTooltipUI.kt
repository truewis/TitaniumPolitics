package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

class MutualityTooltipUI(
    tgtCharacter: String,
    who: String,
    var gameState: GameState,
    width: Float = 350f,
    height: Float = 400f
) :

    Tooltip<Table>(scene2d.table {
        addActor(scene2d.image("TooltipShadow10p") {
            it.width = width + 100f
            it.height = height + 100f
            it.x = -50f
            it.y = -50f
            setColor(0f, 0f, 0f, 0.7f)
            touchable = Touchable.disabled//This is a shadow outside the tooltip
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
                fun createReasonLabel(a: String, b: String) =
                    TypingLabel("", Scene2DSkin.defaultSkin, "description").apply {
                        var ret = ""
                        gameState.getSignificantMutualityReasons(a, b).forEach {
                            val delta = it.first
                            val reasonKeys = it.second.split(";")
                            if (reasonKeys.size == 1) {
                                if (delta >= 0)
                                    ret += ("{COLOR=GREEN}" + ReadOnly.prop("mutuality-${reasonKeys[0]}") + '\n')
                                else
                                    ret += ("{COLOR=RED}" + ReadOnly.prop("mutuality-${reasonKeys[0]}") + '\n')
                            } else {
                                if (delta >= 0)
                                    ret += ("{COLOR=GREEN}" + ReadOnly.prop("mutuality-${reasonKeys[0]}")
                                        .format(*reasonKeys.drop(1).toTypedArray()) + '\n')
                                else
                                    ret += ("{COLOR=RED}" + ReadOnly.prop("mutuality-${reasonKeys[0]}")
                                        .format(*reasonKeys.drop(1).toTypedArray()) + '\n')
                            }
                        }
                        ret.dropLast(1) // Remove the last newline character
                        setText(ret)
                        skipToTheEnd()
                        setFontScale(0.2f)
                        setAlignment(Align.topLeft)
                        wrap = true
                    }

                val PADDING = 3f

                val tgtName = ReadOnly.charName(tgtCharacter)
                var text1 = if (gameState.getMutNorm(tgtCharacter, who) > 0.2) {
                    ReadOnly.prop("MutualityTooltipUI-YouHigh")
                } else if (gameState.getMutNorm(tgtCharacter, who) > -0.2) {
                    ReadOnly.prop("MutualityTooltipUI-YouMedium")
                } else if (gameState.getMutNorm(tgtCharacter, who) > -0.5) {
                    ReadOnly.prop("MutualityTooltipUI-YouLow")
                } else {
                    ReadOnly.prop("MutualityTooltipUI-YouVeryLow")
                }
                val reasonLabel1 = createReasonLabel(tgtCharacter, who)

                var text2 =
                    if (gameState.getMutNorm(who, tgtCharacter) > 0.2) {
                        ReadOnly.prop("MutualityTooltipUI-TheyHigh")
                    } else if (gameState.getMutNorm(who, tgtCharacter) > -0.2) {
                        ReadOnly.prop("MutualityTooltipUI-TheyMedium")
                    } else if (gameState.getMutNorm(who, tgtCharacter) > -0.5) {
                        ReadOnly.prop("MutualityTooltipUI-TheyLow")
                    } else {
                        ReadOnly.prop("MutualityTooltipUI-TheyVeryLow")
                    }
                val reasonLabel2 = createReasonLabel(who, tgtCharacter)

                pad(PADDING)
                label(text1, "description") {
                    it.size(width - 2 * PADDING, 50f)
                    setFontScale(0.3f)
                    setAlignment(Align.topLeft)
                    wrap = true
                }
                row()
                add(reasonLabel1).size(width - 2 * PADDING, 150f)
                row()
                label(text2, "description") {
                    it.size(width - 2 * PADDING, 50f)
                    setFontScale(0.3f)
                    setAlignment(Align.topLeft)
                    wrap = true
                }
                row()
                add(reasonLabel2).size(width - 2 * PADDING, 150f)
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