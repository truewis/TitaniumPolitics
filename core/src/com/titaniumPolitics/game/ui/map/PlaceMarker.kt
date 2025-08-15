package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.label
import ktx.scene2d.scene2d

open class PlaceMarker(var gameState: GameState, var owner: MapUI, var place: String) : Button() {
    init {
        //Set style of the button to the default skin.
        style = defaultSkin.get("diamond", ButtonStyle::class.java)
        val start: Pair<Float, Float> = owner.convertToScreenCoords(
            gameState.places[place]!!.coordinates.x.toFloat(),
            gameState.places[place]!!.coordinates.z.toFloat()
        )
        //Set marker to different color if player is here.
        if (gameState.player.place.name == place) {
            color = Color.RED
            addAction(
                Actions.forever(
                    Actions.sequence(
                        Actions.delay(0.5f),
                        AlphaAction().apply {
                            duration = 0.2f
                            alpha = 0f
                        },
                        AlphaAction().apply {
                            duration = 0.2f
                            alpha = 1f
                        }
                    )))
            val infoText = scene2d.label("YOU ARE HERE", "docTitle") {
                setFontScale(0.2f)
                setAlignment(Align.center)
                color = Color.RED
                touchable = Touchable.disabled // Make the label not interactable as it will cover other markers
            }
            addActor(infoText)
            infoText.setPosition(
                SIZE / 2,
                -10f, // Position it below the marker
                Align.center
            )
        }
        owner.dataTable.addActor(this)

        //Set the size of the connection to the length of the line.
        this.setSize(SIZE, SIZE)
        //Set the position of the connection to the start of the line.
        this.setPosition(start.first - width / 2, start.second - height / 2)
        this.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                //Open Place UI
                owner.currentPlaceMarkerWindow.refresh(getX(), getY(), place)
            }
        })


    }

    companion object {
        const val SIZE = 30f // Size of the marker
    }


}