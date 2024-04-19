package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.GameAction

class CommandUI(skin: Skin?, var gameState: GameState) : Table(skin)
{
    var titleLabel: Label
    private var isOpen = false;
    val placeDropDown = SelectBox<String>(skin)
    val actionDropDown = SelectBox<String>(skin)
    val timeSelection = Slider(0f, 48f, 1f, false, skin)
    val submitButton = TextButton("Command", skin)
    val cancelButton = TextButton("Cancel", skin)

    init
    {
        titleLabel = Label("지시", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        add(placeDropDown)
        row()
        add(actionDropDown)
        row()
        add(timeSelection)
        row()
        add(submitButton)
        add(cancelButton)
        isVisible = false
        GameEngine.acquireEvent += {
            if (it.type == "Command")
            {
                refresh(
                    it.variables["issuedBy"] as String,
                    it.variables["issuedTo"] as String,
                    it.variables["party"] as String
                )
                val who = it.variables["issuedTo"] as String
                submitButton.clearListeners()
                submitButton.addListener(object : ClickListener()
                {
                    override fun clicked(event: InputEvent?, x: Float, y: Float)
                    {
                        super.clicked(event, x, y)
                        val action =
                            Class.forName("com.titaniumPolitics.game.core.gameActions.${actionDropDown.selected}")
                                .getDeclaredConstructor(String::class.java, String::class.java).newInstance(
                                    who,
                                    gameState.places.values.find { it.characters.contains(who) }!!.name
                                ) as GameAction
                        GameEngine.acquireCallback(
                            Request(
                                placeDropDown.selected,
                                action
                            ).also { command ->
                                command.executeTime = timeSelection.value.toInt(); command.issuedBy =
                                hashSetOf(gameState.playerName)
                            })
                        isVisible = false
                    }
                })
            }

        }
    }


    fun refresh(issuedBy: String, issuedTo: String, party: String)
    {
        placeDropDown.setItems(*gameState.places.keys.toTypedArray())
        if (placeDropDown.selected == null)
            actionDropDown.setItems("")
        else
            actionDropDown.setItems(
                *GameEngine.availableActions(gameState, placeDropDown.selected, issuedTo).toTypedArray()
            )


        isVisible = true

    }

    fun open()
    {
        isVisible = true
    }


}