package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.MutualityTooltipUI
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class MutualityMeter(var gameState: GameState, var tgtCharacter: String, var who: String) : Table(defaultSkin), KTable {
    val bar1 = MeterOvalUI()
    val bar2 = MeterOvalUI()


    val refresh =
        { state: GameState ->
            setValue(
                (state.getMutNorm(tgtCharacter, who) + 1) / 2,
                (state.getMutNorm(who, tgtCharacter) + 1) / 2
            )
        }

    init {
        stack {
            it.grow()

            table {
                add(this@MutualityMeter.bar1).size(300f, 30f)
                row()
                add(this@MutualityMeter.bar2).size(300f, 30f)
            }
            container {
                size(50f, 50f)
                image("icon_gesture_58") {
                    color = Color(1f, 1f, 1f, 0.7f)
                }

            }
        }

//        textTooltip("${(bar.fill * 100).toInt()}", "default") {
//            this.setFontScale(2f)
//            it.manager.initialTime = 0.5f
//        }
        gameState.updateUI += refresh
        refresh(gameState)
        addListener(MutualityTooltipUI(tgtCharacter, who, gameState))
    }

    //Override this method instead of remove, remove is not called properly.
    override fun setParent(parent: Group?) {
        if (parent == null) {
            gameState.updateUI -= refresh
        }
        super.setParent(parent)
    }

    fun setValue(value1: Double, value2: Double) {
        bar1.setValue(value1.toFloat())
        bar2.setValue(value2.toFloat())
    }
}
