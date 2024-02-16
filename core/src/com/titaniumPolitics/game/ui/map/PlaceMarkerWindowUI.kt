package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Move
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.scene2d

class PlaceMarkerWindowUI(var gameState: GameState) : Window("Place Marker", defaultSkin)
{
    var placeDisplayed = ""
    private val moveButton = scene2d.button {
        label("Move to Place", "console") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Move to place.
                val action = Move(
                    gameState.playerAgent,
                    gameState.places.values.find { it.characters.contains(gameState.playerAgent) }!!.name
                )
                action.placeTo = placeDisplayed
                action.injectParent(gameState)
                MapUI.instance.isVisible = false
                this@PlaceMarkerWindowUI.isVisible = false
                GameEngine.acquireCallback(action)
            }
        })
    }

    init
    {
        titleLabel.setFontScale(2f)
        setSize(300f, 200f)
        setPosition(100f, 100f)

    }

    fun refresh(x: Float, y: Float, placeName: String)
    {
        //If the window is already visible, hide it.
        if (placeDisplayed == placeName)
        {
            isVisible = false
            placeDisplayed = ""

        } else
        {
            val XOFFSET = 10f
            val YOFFSET = 10f
            setPosition(x + XOFFSET, y + YOFFSET)
            isVisible = true
            this.titleLabel.setText(placeName)
            placeDisplayed = placeName

            //Clear the list of any previous buttons.
            clear()
            //Disable the button if the player is already in the place. Calling place property will throw an exception when the game is first loaded.
            if (gameState.characters[gameState.playerAgent]!!.place.name != placeDisplayed)
                add(moveButton).fill()
        }
    }
}