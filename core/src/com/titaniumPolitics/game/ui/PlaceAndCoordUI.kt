package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin.defaultSkin

class PlaceAndCoordUI(gameState: GameState) : Table(defaultSkin)
{
    var l: Label
    var c: Label
    var t: Label

    init
    {
        l = Label("", defaultSkin, "console")
        l.setFontScale(2f)
        c = Label("", defaultSkin, "console")
        c.setFontScale(1f)
        t = Label("", defaultSkin, "console")
        t.setFontScale(1f)
        add(l).growX()
        row()
        add(c).growX()
        row()
        add(t).growX()

        gameState.updateUI += { x ->
            Gdx.app.postRunnable {
                if (x.player.place.name.contains("home"))
                {
                    l.setText(ReadOnly.prop("home"))
                    c.setText(ReadOnly.prop("uncharted"))
                } else
                {
                    l.setText(ReadOnly.prop(x.player.place.name))
                    c.setText(x.player.place.coordinates.toString())
                }
                //Display temperature with two decimal places
                t.setText("%.2f°C".format(x.player.place.temperature))

            }
        }
    }

}
