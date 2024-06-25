package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Talk
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.scene2d

//This UI is a window that pops up when the player clicks on a character in the map. It allows the player to talk to the character or select them.
class CharacterInteractionWindowUI(var gameState: GameState) :
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
                    gameState.playerName,
                    gameState.player.place.name
                )
                action.who = characterDisplayed
                action.injectParent(gameState)
                this@CharacterInteractionWindowUI.isVisible = false
                GameEngine.acquireCallback(action)
            }
        })
    }

    private val giveResourceButton = scene2d.button {
        label("Give resources...", "console") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                this@CharacterInteractionWindowUI.isVisible = false
                ResourceTransferUI.instance.isVisible = true
                ResourceTransferUI.instance.toWhere = "home_$characterDisplayed"
                ResourceTransferUI.instance.refresh("private", GameEngine.acquireCallback, gameState.player.resources)
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
            }
        }
        )
    }
    private val closeButton = scene2d.button {
        label("Close", "console") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                this@CharacterInteractionWindowUI.isVisible = false

            }
        })
    }

    init
    {
        instance = this
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
            if (!charName.contains("Anon"))
                this.titleLabel.setText(ReadOnly.prop(charName))
            else
                this.titleLabel.setText("Survivor")
            characterDisplayed = charName

            //Clear the list of any previous buttons.
            clear()

            //If place selection mode is active, add the selection button and nothing else.
            if (mode == "CharSelection")
            {
                add(selectButton).fill()
                row()
            } else
            {
                //Disable the button if the player is already in the place. Calling place property will throw an exception when the game is first loaded.
                //Also, disable the button if the character is already in the meeting ("talking" to them already).
                if (gameState.player.currentMeeting?.currentCharacters?.contains(
                        characterDisplayed
                    ) != true
                )
                {
                    add(talkButton).fill()
                    row()
                    add(giveResourceButton).fill()
                    row()
                }
            }
            add(closeButton).fill()
        }
    }

    companion object
    {
        //Singleton instance, because there should only be one of this UI appearing at a time.
        lateinit var instance: CharacterInteractionWindowUI
    }
}