package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.core.gameActions.Sleep
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.ui.AlertUI
import com.titaniumPolitics.game.ui.FloatingWindowUI
import com.titaniumPolitics.game.ui.ProgressBackgroundUI
import com.titaniumPolitics.game.ui.WaitUIMode
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.scene2d

class PlaceMarkerWindowUI(var gameState: GameState, var owner: MapUI) : FloatingWindowUI() {
    var placeDisplayed = ""
    val distance get() = (gameState.player.place.shortestPathAndTimeTo(placeDisplayed)?.second ?: 0) * ReadOnly.dt / 60
    var mode = ""
    var interrupted = false//Only used in move mode.
    var tgtDestination = ""//Only used in move mode.

    init {
        gameState.onAddInfo += this::moveInterruptCondition
    }

    lateinit var moveLabel: Label
    private val moveButton = scene2d.button {
        this@PlaceMarkerWindowUI.moveLabel = label("Move to Place: " + this@PlaceMarkerWindowUI.distance + "m") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                //Move to place.
                val place = this@PlaceMarkerWindowUI.gameState.places[this@PlaceMarkerWindowUI.placeDisplayed]!!
                //If place is connect to the player's current place, move there.
                if (place.connectedPlaces.contains(this@PlaceMarkerWindowUI.gameState.player.place.name)) {
                    val action = Move(
                        this@PlaceMarkerWindowUI.gameState.playerName,
                        this@PlaceMarkerWindowUI.gameState.player.place.name
                    )
                    action.placeTo = this@PlaceMarkerWindowUI.placeDisplayed
                    action.injectParent(this@PlaceMarkerWindowUI.gameState)
                    this@PlaceMarkerWindowUI.owner.isVisible = false
                    this@PlaceMarkerWindowUI.isVisible = false

                    ProgressBackgroundUI.instance.text = ReadOnly.prop("Moving")
                    ProgressBackgroundUI.instance.setVisibleWithFade(true)

                    GameEngine.acquireCallback(action)
                }
                //If place is not connected, set the destination and start the move process.
                else {
                    this@PlaceMarkerWindowUI.tgtDestination = this@PlaceMarkerWindowUI.placeDisplayed
                    this@PlaceMarkerWindowUI.interrupted = false
                    GameEngine.acquireEvent += this@PlaceMarkerWindowUI::spendTime
                    val action = Move(
                        this@PlaceMarkerWindowUI.gameState.playerName,
                        this@PlaceMarkerWindowUI.gameState.player.place.name
                    )
                    action.placeTo = this@PlaceMarkerWindowUI.placeDisplayed
                    action.injectParent(this@PlaceMarkerWindowUI.gameState)
                    this@PlaceMarkerWindowUI.owner.isVisible = false
                    this@PlaceMarkerWindowUI.isVisible = false

                    ProgressBackgroundUI.instance.text = ReadOnly.prop("Moving")
                    ProgressBackgroundUI.instance.setVisibleWithFade(true)

                    GameEngine.acquireCallback(action)
                }
            }
        })
    }

    private val selectButton = scene2d.button {
        label("Select Place") {
            setFontScale(2f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                //Select place.
                PlaceSelectionUI.instance.selectedPlaceCallback(this@PlaceMarkerWindowUI.placeDisplayed)
                this@PlaceMarkerWindowUI.owner.isVisible = false
            }
        }
        )
    }

    private fun moveInterruptCondition(info: Information) {
        //Interrupt if a character performs an action other than wait in this place.
        if (info.tgtPlace == gameState.player.place.name && info.tgtCharacter != gameState.playerName &&
            !(info.type == InformationType.ACTION && info.action is Wait) && info.knownTo.contains(gameState.playerName)
        ) {

            AlertUI.instance.addAlert("interruptedMove", ReadOnly.prop(info.tgtCharacter))
            interrupted = true
            println("MoveUI: Move interrupted by ${info.author} at ${info.tgtPlace}")
        }

    }

    fun spendTime(AcquireParams: GameEngine.Companion.AcquireParams) {
        if (interrupted) {
            GameEngine.acquireEvent -= this::spendTime
            ProgressBackgroundUI.instance.setVisibleWithFade(false)
            return
        }
        if (gameState.player.place.name == tgtDestination) {
            GameEngine.acquireEvent -= this::spendTime
            ProgressBackgroundUI.instance.setVisibleWithFade(false)
            return
        }
        val nextStop = gameState.player.place.shortestPathAndTimeTo(tgtDestination)?.first?.get(1)
        if (nextStop == null) {
            AlertUI.instance.addAlert("interruptedMove-noPath", tgtDestination)
            interrupted = true
            GameEngine.acquireEvent -= this::spendTime
            ProgressBackgroundUI.instance.setVisibleWithFade(false)
            return
        }
        GameEngine.acquireCallback(
            Move(
                gameState.playerName,
                gameState.player.place.name
            ).apply {
                placeTo = nextStop
            }
        )
        ProgressBackgroundUI.instance.text = ReadOnly.prop("Moving")
        ProgressBackgroundUI.instance.setVisibleWithFade(true)

    }

    fun refresh(x: Float, y: Float, placeName: String) {
        //If the window is already visible, hide it.
        if (placeDisplayed == placeName && isVisible) {
            isVisible = false
            placeDisplayed = ""

        } else {
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
                if (mode == "PlaceSelection") {
                    add(selectButton).size(200f, 50f).fill()
                    row()
                } else {
                    moveLabel.setText("Move to Place: ${distance}min")
                    //Disable the button if the player is already in the place. Calling place property will throw an exception when the game is first loaded.
                    if (gameState.characters[gameState.playerName]!!.place.name != placeDisplayed) {
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