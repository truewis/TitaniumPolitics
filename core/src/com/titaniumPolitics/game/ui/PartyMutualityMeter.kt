package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class PartyMutualityMeter(var gameState: GameState, var partyA: String, var partyB: String = partyA) :
    Table(defaultSkin), KTable {
    val bar1 = MeterOvalUI()


    val refresh =
        { state: GameState ->
            setValue(
                (state.getPartyMutNorm(partyA, partyB) + 1) / 2,
            )
        }

    init {
        stack {
            it.grow()

            table {
                add(this@PartyMutualityMeter.bar1).size(300f, 30f)
            }
        }

//        textTooltip("${(bar.fill * 100).toInt()}", "default") {
//            this.setFontScale(2f)
//            it.manager.initialTime = 0.5f
//        }
        gameState.updateUI += refresh
        refresh(gameState)
    }

    //Override this method instead of remove, remove is not called properly.
    override fun setParent(parent: Group?) {
        if (parent == null) {
            gameState.updateUI -= refresh
        }
        super.setParent(parent)
    }

    fun setValue(value1: Double) {
        bar1.setValue(value1.toFloat())
    }
}
