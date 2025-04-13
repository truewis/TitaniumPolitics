package com.titaniumPolitics.game.ui.map

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.ui.FloatingWindowUI
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.scene2d

class PlaceMarkerWindowUI(var gameState: GameState, var owner: MapUI) : FloatingWindowUI()
{
    var placeDisplayed = ""
    var mode = ""
    private val moveButton = scene2d.button {
        label("Move to Place") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Move to place.
                val action = Move(
                    this@PlaceMarkerWindowUI.gameState.playerName,
                    this@PlaceMarkerWindowUI.gameState.player.place.name
                )
                action.placeTo = this@PlaceMarkerWindowUI.placeDisplayed
                action.injectParent(this@PlaceMarkerWindowUI.gameState)
                this@PlaceMarkerWindowUI.owner.isVisible = false
                this@PlaceMarkerWindowUI.isVisible = false
                GameEngine.acquireCallback(action)
            }
        })
    }

    private val selectButton = scene2d.button {
        label("Select Place") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Select place.
                PlaceSelectionUI.instance.selectedPlaceCallback(this@PlaceMarkerWindowUI.placeDisplayed)
                this@PlaceMarkerWindowUI.owner.isVisible = false
            }
        }
        )
    }

    fun refresh(x: Float, y: Float, placeName: String)
    {
        //If the window is already visible, hide it.
        if (placeDisplayed == placeName && isVisible)
        {
            isVisible = false
            placeDisplayed = ""

        } else
        {
            val XOFFSET = 10f
            val YOFFSET = 10f
            setPosition(x + XOFFSET, y + YOFFSET)
            isVisible = true
            if (placeName.contains("home")) this.titleLabel.setText(ReadOnly.prop("home"))
            else
                this.titleLabel.setText(ReadOnly.prop(placeName))
            placeDisplayed = placeName

            //Clear the list of any previous buttons.
            content.apply {
                clear()
                //If place selection mode is active, add the selection button and nothing else.
                if (mode == "PlaceSelection")
                {
                    add(selectButton).size(200f, 50f).fill()
                    row()
                } else
                {
                    //Disable the button if the player is already in the place. Calling place property will throw an exception when the game is first loaded.
                    if (gameState.characters[gameState.playerName]!!.place.connectedPlaces.contains(placeDisplayed))
                    {
                        add(moveButton).size(200f, 50f).fill()
                        row()
                    }
                }

                add(closeButton).fill().size(200f, 50f)
            }
            setSize(350f, 50f + content.prefHeight)
        }
    }
}