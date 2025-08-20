package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.BLACK
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.AlertUI
import com.titaniumPolitics.game.ui.ProgressBackgroundUI
import com.titaniumPolitics.game.ui.widget.DescriptionLabel
import com.titaniumPolitics.game.ui.widget.ResourceDisplayUI
import com.titaniumPolitics.game.ui.widget.TitleLabel
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

class PlaceMarkerWindowUI(var gameState: GameState, var owner: MapUI) : Table() {
    var placeDisplayed = ""
    val distance get() = (gameState.player.place.shortestPathAndTimeTo(placeDisplayed)?.second ?: 0) * ReadOnly.DT / 60
    var mode = ""
    var interrupted = false//Only used in move mode.
    var tgtDestination = ""//Only used in move mode.
    private val onRefresh = mutableListOf<() -> Unit>()
    val onClose = mutableListOf<() -> Unit>()
    val content = Table()
    val titleLabel = TitleLabel("", 0.5f)

    init {
        add(titleLabel).growX().fill()
        row()
        add(content).grow()
        gameState.onAddInfo += this::moveInterruptCondition
    }

    lateinit var moveLabel: Label
    private val moveButton = scene2d.button {
        this@PlaceMarkerWindowUI.moveLabel =
            label("Move to Place: " + this@PlaceMarkerWindowUI.distance + "m", "description") {
                setFontScale(0.4f)
                setAlignment(Align.center)
                color = Color.WHITE
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
                    ProgressBackgroundUI.instance.setVisibleWithFade(true, "Move")

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

                    ProgressBackgroundUI.instance.setVisibleWithFade(true, "Move")

                    GameEngine.acquireCallback(action)
                }
                //Close the window after the move action is initiated.
                onClose.forEach { it() }
            }
        })
    }

    private val selectButton = scene2d.button {
        label("Select Place", "description") {
            setFontScale(0.4f)
            setAlignment(Align.center)
            color = Color.WHITE
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                //Select place.
                PlaceSelectionUI.instance.selectedPlaceCallback(this@PlaceMarkerWindowUI.placeDisplayed)
            }
        }
        )
    }


    val resourceInformation = scene2d.table {
        name = "resourceInformation"
        it.height = 50f
        add(
            TitleLabel(
                ReadOnly.prop("resourceInformation"),
                0.3f,
                BLACK
            ).apply { left(); label.setAlignment(Align.left) }).height(50f).growX().fill()
        row()
        stack {
            it.growX()
            val shortLabel = label("No resource information available", "description") {
                setFontScale(0.2f)
                setAlignment(Align.left)
                color = Color.WHITE
                wrap = true
            }
            val rdUI = ResourceDisplayUI()
            val timeLabel = scene2d.label("", "docTitle") {
                setFontScale(0.2f)
                setAlignment(Align.left)
                color = Color.WHITE
            }

            val tb = table {
                add(rdUI).grow()
                row()
                add(timeLabel).growX()
            }
            this@PlaceMarkerWindowUI.onRefresh += {
                //Update the resource information label with the most recent information about the place.
                val gState = this@PlaceMarkerWindowUI.gameState
                gState.informations.values.filter {
                    it.type == InformationType.RESOURCES && it.tgtPlace == this@PlaceMarkerWindowUI.placeDisplayed && it.knownTo.contains(
                        gState.playerName
                    )
                }.maxByOrNull { it.tgtTime }?.let { info ->
                    rdUI.current = info.resources
                    rdUI.refresh()
                    timeLabel.setText(GameState.formatTime(info.tgtTime))
                    shortLabel.isVisible = false
                    tb.isVisible = true
                }
                    ?: run {
                        //If no information is available, display a message.
                        shortLabel.isVisible = true
                        tb.isVisible = false
                        shortLabel.setText(
                            ReadOnly.prop("noResourceInformationAvailable")
                        )

                    }

            }
        }

    }
    val managementInformation = scene2d.table {
        name = "managementInformation"
        it.height = 50f
        add(
            TitleLabel(
                ReadOnly.prop("managementInformation"),
                0.3f,
                BLACK
            ).apply { left(); label.setAlignment(Align.left) }).height(50f).growX().fill()
        row()
        val divisionLabel = label("Division: ", "description") {
            it.left()
            setFontScale(0.2f)
            setAlignment(Align.left)
            color = Color.WHITE
        }
        row()
        val managerLabel = label("Manager: ", "description") {
            it.left()
            setFontScale(0.2f)
            setAlignment(Align.left)
            color = Color.WHITE
        }
        this@PlaceMarkerWindowUI.onRefresh += {
            //Update the resource information label with the most recent information about the place.
            val gState = this@PlaceMarkerWindowUI.gameState
            gState.places[this@PlaceMarkerWindowUI.placeDisplayed]!!.responsibleDivision?.run {

                divisionLabel.setText(
                    "Managed by " + ReadOnly.prop(this)
                )
            } ?: divisionLabel.setText(
                "Managed by: Not assigned"
            )
            gState.places[this@PlaceMarkerWindowUI.placeDisplayed]!!.manager?.run {
                managerLabel.setText(
                    "Manager: " + ReadOnly.charProp(this)
                )
            }
                ?: managerLabel.setText(
                    "Manager: Not assigned"
                )
        }

    }

    private fun moveInterruptCondition(info: Information) {
        if (interrupted)
            return // If already interrupted, do not process further.
        if (false
        //Do not interrupt moving
//        if (info.tgtPlace == gameState.player.place.name && info.tgtCharacter != gameState.playerName &&
//            !(info.type == InformationType.ACTION && info.action is Wait) && info.knownTo.contains(gameState.playerName)
        ) {

            AlertUI.instance.addAlert("interruptedMove", ReadOnly.charProp(info.tgtCharacter ?: "Someone"))
            interrupted = true
            Logger.write("MoveUI: Move interrupted by ${info.author} at ${info.tgtPlace}", Logger.LogLevel.INFO)
        }

    }

    fun spendTime(AcquireParams: GameEngine.Companion.AcquireParams) {
        if (interrupted) {
            GameEngine.acquireEvent -= this::spendTime
            ProgressBackgroundUI.instance.setVisibleWithFade(false, "Move")
            return
        }
        if (gameState.player.place.name == tgtDestination) {
            GameEngine.acquireEvent -= this::spendTime
            ProgressBackgroundUI.instance.setVisibleWithFade(false, "Move")
            return
        }
        val nextStop = gameState.player.place.shortestPathAndTimeTo(tgtDestination)?.first?.get(1)
        if (nextStop == null) {
            AlertUI.instance.addAlert("interruptedMove-noPath", tgtDestination)
            interrupted = true
            GameEngine.acquireEvent -= this::spendTime
            ProgressBackgroundUI.instance.setVisibleWithFade(false, "Move")
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
        ProgressBackgroundUI.instance.setVisibleWithFade(true, "Move")

    }

    fun refresh(placeName: String) {
        //setPosition(x + XOFFSET, y + YOFFSET)
        val txt = ReadOnly.placeProp(placeName)
        this.titleLabel.label.setText(txt)
        if (txt.length > 27)
            this.titleLabel.label.setFontScale(0.4f) //TODO: This is a temporary fix, should be replaced with Issue #124
        else
            this.titleLabel.label.setFontScale(0.5f)
        placeDisplayed = placeName

        //Clear the list of any previous buttons.
        content.apply {
            top()
            clear()
            //If place selection mode is active, add the selection button and nothing else.
            if (mode == "PlaceSelection") {
                add(selectButton).size(400f, 75f).fill()
                row()
            } else {
                moveLabel.setText("Move to Place: ${distance}min")
                //Disable the button if the player is already in the place. Calling place property will throw an exception when the game is first loaded.
                if (gameState.characters[gameState.playerName]!!.place.name != placeDisplayed) {
                    add(moveButton).size(400f, 75f).fill()
                    row()
                }
            }
            add(resourceInformation).fillX().expandX()
            row()
            add(managementInformation).fillX().expandX()
            row()
            add(DescriptionLabel(ReadOnly.placeProp("$placeDisplayed-desc")).apply {
                with(label) {
                    color = Color.LIGHT_GRAY
                }
            }).growX().height(200f).fill().padTop(50f)
        }
        setSize(350f, 50f + content.prefHeight)
        //Update the resource information and management information tables.
        onRefresh.forEach { it() }
    }
}