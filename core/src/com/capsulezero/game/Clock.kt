package com.capsulezero.game

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.capsulezero.game.core.GameState
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import ktx.scene2d.Scene2DSkin.defaultSkin

class Clock (gameState: GameState) : Table(defaultSkin) {
    var l: Label

    init {
        l = TypingLabel("{COLOR=GREEN}"+formatTime(gameState.time), defaultSkin, "trnsprtConsole")
        l.setFontScale(2f)
        val b = TextButton("시간", defaultSkin)
        add(l)
        add(b)
        gameState.timeChanged+={ _, y->l.setText(formatTime(y))}
    }
    companion object{
        fun formatTime(time:Int):String{
            val t1 = time / 48
            val t2 = (time - t1)/2
            val t3 = if(time%2==0) "00" else "30"
            return "${t1}D ${t2}:${t3}"
        }
    }
}
