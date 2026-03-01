package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.BLACK
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.GameEngine.Companion.AcquireParams
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.core.gameActions.Talk
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.AlertUI
import com.titaniumPolitics.game.ui.ProgressBackgroundUI
import com.titaniumPolitics.game.ui.widget.DescriptionLabel
import com.titaniumPolitics.game.ui.widget.DivisionBannerUI
import com.titaniumPolitics.game.ui.widget.InformationSourceUI
import com.titaniumPolitics.game.ui.widget.ResourceDisplayUI
import com.titaniumPolitics.game.ui.widget.TitleLabel
import ktx.actors.alpha
import ktx.scene2d.button
import ktx.scene2d.container
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

class PlaceMarkerWindowUI(var gameState: GameState, var owner: MapUI) : Table() {
    var placeDisplayed = ""
    val distance
        get() = (gameState.player.place.shortestPathAndTimeTo(placeDisplayed, gameState.playerName)?.second
            ?: 0) * ReadOnly.DT / 60
    var mode = ""
    var selectedCharacter = ""
    var interrupted =
        true//Only used in move mode. Initially true to prevent any interruption handling before move starts.
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

    lateinit var moveTimeLabel: Label
    private val moveButton = scene2d.button {
        label(
            "${ReadOnly.prop("PlaceMarkerWindowUI-MoveToPlacePrefix")} ",
            "description"
        ) {
            setFontScale(0.4f)
            setAlignment(Align.center)
            color = Color.WHITE
        }
        row()
        moveTimeLabel = label(
            "",
            "description"
        ) {
            setFontScale(0.2f)
            setAlignment(Align.center)
            color = Color.WHITE
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                this@PlaceMarkerWindowUI.interrupted = false
                this@PlaceMarkerWindowUI.tgtDestination = this@PlaceMarkerWindowUI.placeDisplayed
                GameEngine.acquireEvent += this@PlaceMarkerWindowUI::spendTime
                spendTime(AcquireParams("", hashMapOf()))
                ProgressBackgroundUI.instance.setVisibleWithFade(true, "Move")
                //Close the window after the move action is initiated.
                onClose.forEach { it() }
            }
        })
    }

    private val selectButton = scene2d.button {
        label(ReadOnly.prop("PlaceMarkerWindowUI-SelectPlace"), "description") {
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
            val shortLabel = label(ReadOnly.prop("PlaceMarkerWindowUI-NoResourceInfoAvailable"), "description") {
                setFontScale(0.2f)
                setAlignment(Align.left)
                color = Color.WHITE
                wrap = true
            }
            val rdUI = ResourceDisplayUI()

            val tb = table {
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
                    shortLabel.isVisible = false
                    tb.isVisible = true
                    tb.clear()
                    tb.apply {
                        this.add(rdUI).growX().fill()
                        row()
                        this.add(InformationSourceUI(info)).growX()
                    }
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
        val divisionLabel = label(ReadOnly.prop("PlaceMarkerWindowUI-DivisionPrefix"), "description") {
            it.left()
            setFontScale(0.2f)
            setAlignment(Align.left)
            color = Color.WHITE
        }
        row()
        val managerLabel = label(ReadOnly.prop("PlaceMarkerWindowUI-ManagerPrefix"), "description") {
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
                    ReadOnly.prop("PlaceMarkerWindowUI-ManagedBy").format(ReadOnly.prop(this))
                )
            } ?: divisionLabel.setText(
                ReadOnly.prop("PlaceMarkerWindowUI-ManagedBy").format(ReadOnly.charProp("NotAssigned"))

            )
            gState.places[this@PlaceMarkerWindowUI.placeDisplayed]!!.manager?.run {
                managerLabel.setText(
                    ReadOnly.prop("PlaceMarkerWindowUI-ManagerIs").format(ReadOnly.charProp(this))
                )
            }
                ?: managerLabel.setText(
                    ReadOnly.prop("PlaceMarkerWindowUI-ManagerIs").format(ReadOnly.charProp("NotAssigned"))
                )
        }

    }

    private fun moveInterruptCondition(info: Information) {
        if (interrupted)
            return // If already interrupted, do not process further.
        if (info.tgtPlace == gameState.player.place.name && info.tgtCharacter != gameState.playerName &&
            info.action is Talk && info.knownTo.contains(gameState.playerName) && info.tgtCharacter in gameState.knownCharactersToPlayer
        ) {

            AlertUI.instance.addAlert("interruptedMove", ReadOnly.charProp(info.tgtCharacter ?: "Someone"))
            interrupted = true
            Logger.write("MoveUI: Move interrupted by ${info.author} at ${info.tgtPlace}", Logger.LogLevel.INFO)
        }
        //If I am in a meeting, interrupt the move.
        if (gameState.player.currentMeeting != null) {
            AlertUI.instance.addAlert(
                "interruptedMove",
                ReadOnly.charProp(
                    (gameState.player.currentMeeting!!.currentCharacters - gameState.playerName).firstOrNull()
                        ?: "Someone"
                )
            )
            interrupted = true
            Logger.write("MoveUI: Move interrupted by ${info.author} at ${info.tgtPlace}", Logger.LogLevel.INFO)
        }

    }

    /**
     * This function is called every time the player turn starts while the player is moving to the target destination.
     * It is called last time when the player arrives at the destination or the move is interrupted, but it does not add a new move action in that case.
     */
    fun spendTime(AcquireParams: GameEngine.Companion.AcquireParams) {
        println("Spend time called in PlaceMarkerWindowUI: ${gameState.time}")
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
        val nextStop = gameState.player.place.shortestPathAndTimeTo(tgtDestination, gameState.playerName)?.first?.get(1)
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

    }

    fun generateCorridorSuffix(placeName: String): String {
        //Generate four digit hex code based on the place name, using some hash function. This is to differentiate different corridors while still keeping the name generic.
        val hash = placeName.hashCode()
        val hex = Integer.toHexString(hash).takeLast(4).padStart(4, '0')
        return hex
    }

    fun refresh(placeName: String) {
        //setPosition(x + XOFFSET, y + YOFFSET)
        //Set the title of the window. If the place is a corridor, use the generic "Corridor" property instead of the specific place name, since corridors do not have unique properties.
        val txt =
            if (!placeName.contains("corridor")) ReadOnly.placeProp(placeName) else ReadOnly.placeProp("corridor") + generateCorridorSuffix(
                placeName
            )
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
            } else if (mode == "Timeline") {
                // Show character sightings at this place
                val sightings = gameState.informations.values
                    .filter {
                        it.type == InformationType.ACTION &&
                            it.tgtCharacter == selectedCharacter &&
                            it.tgtPlace == placeName &&
                            it.knownTo.contains(gameState.playerName)
                    }
                    .sortedByDescending { it.tgtTime }
                val sightingTable = scene2d.table {
                    top()
                    if (sightings.isEmpty()) {
                        add(label(ReadOnly.prop("TimelineUI-NoSightings"), "description") {
                            setFontScale(0.25f)
                            setAlignment(Align.left)
                            color = Color.LIGHT_GRAY
                        }).fillX().padTop(10f)
                        row()
                    } else {
                        sightings.forEach { info ->
                            val actionName = info.action?.let {
                                try { ReadOnly.prop(it::class.simpleName!!) } catch (e: Exception) { it::class.simpleName ?: "?" }
                            } ?: "?"
                            add(label(
                                ReadOnly.prop("TimelineUI-SightingEntry")
                                    .format(GameState.formatTime(info.tgtTime), actionName),
                                "description"
                            ) {
                                setFontScale(0.25f)
                                setAlignment(Align.left)
                                color = Color.WHITE
                                wrap = true
                            }).fillX().padTop(4f)
                            row()
                        }
                    }
                }
                add(ScrollPane(sightingTable).also { it.setScrollingDisabled(true, false) }).fillX().expandX().height(300f)
                row()
            } else {
                moveTimeLabel.setText(ReadOnly.prop("PlaceMarkerWindowUI-TimeToDest").format(distance))
                //Disable the button if the player is already in the place. Calling place property will throw an exception when the game is first loaded.
                if (gameState.characters[gameState.playerName]!!.place.name != placeDisplayed) {
                    add(moveButton).size(400f, 75f).fill()
                    row()
                }
            }
            if (mode != "Timeline") {
                add(resourceInformation).fillX().expandX()
                row()
                add(managementInformation).fillX().expandX()
                row()
                add(scene2d.stack {
                    add(
                        DescriptionLabel(
                            if (!placeName.contains("corridor")) ReadOnly.placeProp("$placeDisplayed-desc") else ReadOnly.placeProp(
                                "corridor-desc"
                            )
                        ).apply {
                            with(label) {
                                color = Color.LIGHT_GRAY
                            }
                        })

                    gameState.places[this@PlaceMarkerWindowUI.placeDisplayed]!!.responsibleDivision?.let { div ->
                        container(DivisionBannerUI(gameState.parties[div]!!)) {
                            align(Align.center)
                            alpha = 0.2f
                        }
                    }

                }).growX().height(200f).fill().padTop(50f)
            }
        }
        setSize(350f, 50f + content.prefHeight)
        //Update the resource information and management information tables.
        onRefresh.forEach { it() }
    }
}
