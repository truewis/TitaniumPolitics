package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class Clock (gameState: GameState) : Table(defaultSkin) {
    var l: Label

    init {
        l = Label(formatTime(gameState.time), defaultSkin, "console")
        l.setFontScale(2f)
        val b = TextButton("TIME", defaultSkin)
        add(b)
        add(l)

        gameState.timeChanged+={ _, y->
            Gdx.app.postRunnable {l.setText(formatTime(y))}}
    }
    companion object{
        fun formatTime(time:Int):String{
            val t1 = time / 48
            val t2 = (time - t1*48)/2
            val t3 = if(time%2==0) "00" else "30"
            return "${t1}D ${t2.toString().padStart(2, '0')}:${t3}"
        }
    }
}
