package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Talk
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.scene2d

//This UI is a window that pops up when the player clicks on a character in the map. It allows the player to talk to the character or select them.
class CharacterInteractionWindowUI(var gameState: GameState, var owner: CharacterSelectUI?) :
    Window("Char Marker", defaultSkin)
{
    var characterDisplayed = ""
    var mode = ""
    private val talkButton = scene2d.button {
        label("Talk With...", "console") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Move to place.
                val action = Talk(
                    gameState.playerAgent,
                    gameState.places.values.find { it.characters.contains(gameState.playerAgent) }!!.name
                )
                action.who = characterDisplayed
                action.injectParent(gameState)
                owner?.isVisible = false
                this@CharacterInteractionWindowUI.isVisible = false
                GameEngine.acquireCallback(action)
            }
        })
    }

    private val selectButton = scene2d.button {
        label("Select Character", "console") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Select place.
                PlaceSelectionUI.instance.selectedPlaceCallback(characterDisplayed)
                owner?.isVisible = false
            }
        }
        )
    }

    init
    {
        isVisible = false
        titleLabel.setFontScale(2f)
        setSize(300f, 200f)
        setPosition(100f, 100f)

    }

    fun refresh(x: Float, y: Float, charName: String)
    {
        //If the window is already visible, hide it.
        if (characterDisplayed == charName)
        {
            isVisible = false
            characterDisplayed = ""

        } else
        {
            val XOFFSET = 0
            val YOFFSET = 0
            setPosition(x + XOFFSET, y + YOFFSET)
            isVisible = true
            this.titleLabel.setText(charName)
            characterDisplayed = charName

            //Clear the list of any previous buttons.
            clear()

            //If place selection mode is active, add the selection button and nothing else.
            if (mode == "CharSelection")
            {
                add(selectButton).fill()
            } else
            {
                //Disable the button if the player is already in the place. Calling place property will throw an exception when the game is first loaded.
                if (gameState.characters[gameState.playerAgent]!!.place.name != characterDisplayed)
                    add(talkButton).fill()
            }
        }
    }
}