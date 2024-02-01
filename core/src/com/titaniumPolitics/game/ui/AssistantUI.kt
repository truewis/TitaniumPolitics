package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.textButton

class AssistantUI(gameState: GameState) : Table(defaultSkin), KTable
{

    init
    {
        val calendarLabel = Label("0", defaultSkin).also { it.setFontScale(4f) }
        val calendarButton = button {
            it.fill()
            add(calendarLabel)
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    Gdx.app.log("AssistantUI", "CALENDAR")
                }
            }
            )
        }
        add(ClockUI(gameState)).growX()

        row()

        button { cell ->
            cell.fill()
            label("MAP", "console") {
                setFontScale(4f)
            }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {

                    //Open Map UI
                    Gdx.app.log("AssistantUI", "MAP")
                }
            }
            )
        }
        add(PlaceAndCoordUI(gameState)).growX()


        gameState.timeChanged += { _, y ->
            Gdx.app.postRunnable { calendarLabel.setText((y / 48).toString()) } // Current Date.
        }
    }


}
