package com.capsulezero.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.capsulezero.game.core.GameState
import ktx.scene2d.Scene2DSkin

class HeadUpInterface (val gameState: GameState) : Table(Scene2DSkin.defaultSkin) {
    val todoBox: QuestUI
    init {

        instance = this
        todoBox = QuestUI(Scene2DSkin.defaultSkin, gameState)
        val leftSeparator = Table()
        leftSeparator.add(HealthMeter(gameState)).align(Align.topRight)
        leftSeparator.add(WillMeter(gameState)).align(Align.topRight)
        leftSeparator.add(ApprovalMeter(gameState)).align(Align.topRight)
        leftSeparator.add(Clock(gameState)).align(Align.topRight)
        leftSeparator.add(QuickSave(gameState)).align(Align.topRight)

        leftSeparator.row()
        leftSeparator.add(todoBox).colspan(5).growX().align(Align.topLeft)
        leftSeparator.row()
        leftSeparator.add().grow()
        leftSeparator.debug()
        add(leftSeparator).align(Align.topLeft).grow()

    }

    companion object{
        //Singleton
        lateinit var instance: HeadUpInterface
    }

}