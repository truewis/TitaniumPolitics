package com.titaniumPolitics.game.ui

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Talk
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.scene2d

//This UI is a window that pops up when the player clicks on a character in the map. It allows the player to talk to the character or select them.
class CharacterInteractionWindowUI(var gameState: GameState) :
    FloatingWindowUI()
{
    var characterDisplayed = ""
    var mode = ""
    private val talkButton = scene2d.button {
        label("Talk With...") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Move to place.
                val action = Talk(
                    this@CharacterInteractionWindowUI.gameState.playerName,
                    this@CharacterInteractionWindowUI.gameState.player.place.name
                )
                action.who = this@CharacterInteractionWindowUI.characterDisplayed
                action.injectParent(this@CharacterInteractionWindowUI.gameState)
                this@CharacterInteractionWindowUI.isVisible = false
                GameEngine.acquireCallback(action)
            }
        })
    }

    private val giveResourceButton = scene2d.button {
        label("Give resources...") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                this@CharacterInteractionWindowUI.isVisible = false
                ResourceTransferUI.primary.isVisible = true
                ResourceTransferUI.primary.toWhere = "home_${this@CharacterInteractionWindowUI.characterDisplayed}"
                ResourceTransferUI.primary.refresh(
                    "private",
                    GameEngine.acquireCallback,
                    this@CharacterInteractionWindowUI.gameState.player.resources.toHashMap()
                )
            }
        })
    }

    private val selectButton = scene2d.button {
        label("Select Character") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Select place.
                PlaceSelectionUI.instance.selectedPlaceCallback(this@CharacterInteractionWindowUI.characterDisplayed)
            }
        }
        )
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
            content.apply {
                clear()

                add(MutualityMeter(gameState, tgtCharacter = characterDisplayed, who = gameState.playerName).also {
                    it.remove() //Do not refresh the meter, since this window is not persistent.
                })
                row()
                //If place selection mode is active, add the selection button and nothing else.
                if (mode == "CharSelection")
                {
                    add(selectButton).fill().size(200f, 50f)
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
                        add(talkButton).fill().size(200f, 50f)
                        row()
                        add(giveResourceButton).fill().size(200f, 50f)
                        row()
                    }
                }
                add(closeButton).fill().size(200f, 50f)

            }
            setSize(350f, 50f + content.prefHeight)
        }
    }

    companion object
    {
        //Singleton instance, because there should only be one of this UI appearing at a time.
        lateinit var instance: CharacterInteractionWindowUI
    }
}