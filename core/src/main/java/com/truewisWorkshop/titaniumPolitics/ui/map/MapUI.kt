package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.LIGHT_GRAY
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.TasksUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

open class MapUI(val gameState: GameState) : Table(defaultSkin) {
    val currentConnections = arrayListOf<Connection>()
    val currentMarkers = arrayListOf<PlaceMarker>()
    val currentPlaceMarkerWindow = PlaceMarkerWindowUI(gameState, this)
    private lateinit var scrollPane: ScrollPane
    val dataTable = Table(skin)
    val PADDING = 100f
    val WIDTH = 1080f * 5
    val HEIGHT = 1080f * 5
    var minX = 0
    var minY = 0
    var maxX = 0
    var maxY = 0


    init {
        Logger.write(this::class.java.simpleName + " initialized.", Logger.LogLevel.INFO)
        scrollPane = ScrollPane(dataTable)
        scrollPane.setScrollingDisabled(false, false)
        add(scrollPane).grow()


        val st = scene2d.stack {
            setSize(this@MapUI.WIDTH, this@MapUI.HEIGHT)
            name = "background"

            container(
                image(
                    TextureRegionDrawable(
                        CapsuleStage.instance.assetManager.get(
                            "MapGrid.png", Texture::class.java
                        )!!
                    )
                ) {
                    addListener(object : ClickListener() {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                            //Close Place Marker UI?
                        }
                    }
                    )
                    setColor(1f, 1f, 1f, 0.5f)
                }) {
                fill()
                padRight(-this@MapUI.WIDTH / 4)
                padLeft(-this@MapUI.WIDTH / 4)
                padTop(-this@MapUI.WIDTH / 8)
                padBottom(-this@MapUI.WIDTH / 8 * 3)

            }
        }
        dataTable.addActor(st)
        st.setPosition(PADDING, PADDING)
        currentPlaceMarkerWindow.isVisible = true
        add(currentPlaceMarkerWindow).growY().fill().width(400f)
        dataTable.add().grow()


    }


    open fun refresh() {
        //Calculate the bounds.
        minX = gameState.places.minOf { it.value.coordinates.x }
        minY = gameState.places.minOf { it.value.coordinates.z }
        maxX = gameState.places.maxOf { it.value.coordinates.x }
        maxY = gameState.places.maxOf { it.value.coordinates.z }

        //Background size is determined by the extent of the markers.
        dataTable.cells[0].size(
            WIDTH + PADDING * 2, //Add some padding
            HEIGHT + PADDING * 2 //Add some padding
        )
        dataTable.pack()
        currentMarkers.forEach { dataTable.removeActor(it) }
        currentMarkers.clear()
        //Draw markers for places
        gameState.places.forEach { (placeName, plObj) ->
            if (plObj.isAuthorized(gameState.playerName)) {
                if (plObj.isBuildingIn == null)
                    PlaceMarker(gameState, this, placeName).also {
                        currentMarkers.add(it)
                        it.onClick += {
                            currentMarkers.forEach {
                                if (it != currentMarkers.first { it.place == placeName } && it.isBuildingVisible) {
                                    it.isBuildingVisible = false
                                }
                            }
                        }
                    }
            }
        }

        currentConnections.forEach { dataTable.removeActor(it) }
        currentConnections.clear()
        //Draw connections between places
        currentMarkers.forEach {
            val placeName = it.place
            val place = gameState.places[placeName]!!
            if (!placeName.contains("home")) {
                place.movableConnectedPlaces(gameState.playerName).forEach { connection ->
                    if (!connection.contains("home")) {
                        Connection(gameState, this, placeName, connection).also {
                            it.color = Color.RED
                            currentConnections.add(it)
                        }
                    }
                }

            }
        }
        //Add quest markers.
        gameState.eventSystem.activeQuests.forEach { quest ->
            if (quest.relatedPlace != null) {
                currentMarkers.firstOrNull { it.place == quest.relatedPlace || gameState.places[quest.relatedPlace]!!.isBuildingIn == it.place }
                    ?.addQuestMarker(TasksUI.QuestMarker(quest))
            }
        }

        //Scroll to the player's place.
        val playerPlaceMarker = currentMarkers.firstOrNull { it.place == gameState.player.place.name }
        if (playerPlaceMarker == null) {
            //Player is in a building. Scroll to the place they are living by. And set isBuildingVisible to true.
            val livingByPlaceMarker = currentMarkers.first { it.place == gameState.player.place.isBuildingIn }
            livingByPlaceMarker.isBuildingVisible = true
            scrollPane.scrollTo(
                livingByPlaceMarker.x - scrollPane.width / 2,
                livingByPlaceMarker.y + scrollPane.height / 2,
                scrollPane.width,
                scrollPane.height
            )
        } else {
            scrollPane.scrollTo(
                playerPlaceMarker.x - scrollPane.width / 2,
                playerPlaceMarker.y + scrollPane.height / 2,
                scrollPane.width,
                scrollPane.height
            )
        }

        currentPlaceMarkerWindow.refresh(gameState.player.place.name)
        //Refresh the place marker window.

    }

    fun convertToScreenCoords(x: Float, y: Float): Pair<Float, Float> {
        //Converts coordinates to screen coordinates. The Screen in centered at the player's location.
        var rel_x = x - gameState.player.place.coordinates.x
        var rel_y = y - gameState.player.place.coordinates.z
        if (gameState.player.place.name == "home_" + gameState.playerName) //Homes does not have coordinates, so we use the place the player is living by.
        {
            rel_x = x - gameState.places[gameState.player.livingBy]!!.coordinates.x
            rel_y = y - gameState.places[gameState.player.livingBy]!!.coordinates.z
        }
        return Pair(
            PADDING + (dataTable.width - 2 * PADDING) * (x - minX) / (maxX - minX), //We do absolute coordinates for now. We can replace x with rel_x.
            PADDING + (dataTable.height - 2 * PADDING) * (y - minY) / (maxY - minY)
        )
    }

    //DO not make this class singleton, as it is used in multiple places such as PlaceSelectionUI.

}