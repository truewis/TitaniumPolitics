package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class PlaceAndCoordUI(gameState: GameState) : Table(defaultSkin)
{
    var l: Label
    var c: Label

    init
    {
        l = Label(formatTime(gameState.time), defaultSkin, "console")
        l.setFontScale(2f)
        c = Label(formatTime(gameState.time), defaultSkin, "console")
        c.setFontScale(2f)
        add(l).growX()
        row()
        add(c).growX()

        gameState.updateUI += { x ->
            Gdx.app.postRunnable {
                l.setText(x.player.place.name)
                c.setText(x.player.place.name.length.toString())//This is a test. It should be the coordinates of the place.
            }
        }
    }

    companion object
    {
        fun formatTime(time: Int): String
        {
            val t1 = time / 48
            val t2 = (time - t1 * 48) / 2
            val t3 = if (time % 2 == 0) "00" else "30"
            return "${t1}D ${t2.toString().padStart(2, '0')}:${t3}"
        }
    }
}
