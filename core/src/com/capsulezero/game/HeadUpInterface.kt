package com.capsulezero.game

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.capsulezero.game.core.GameState
import ktx.scene2d.Scene2DSkin

class HeadUpInterface (val gameState: GameState) : Table(Scene2DSkin.defaultSkin) {
    init {
        instance = this

        val leftSeparator = Table()
        add(leftSeparator).align(Align.topLeft).fill()
        leftSeparator.add(Clock(gameState)).align(Align.topRight)
        leftSeparator.row()
        leftSeparator.add().growY()


    }

    companion object{
        //Singleton
        lateinit var instance:HeadUpInterface
    }

}