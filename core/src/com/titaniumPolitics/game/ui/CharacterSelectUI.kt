package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.map.*
import ktx.scene2d.*

class CharacterSelectUI(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable
{
    val currentConnections = arrayListOf<Connection>()
    val currentMarkers = arrayListOf<PlaceMarker>()
    val currentCharMarkerWindow = CharacterInteractionWindowUI(gameState, this)


    init
    {
        instance = this
        isVisible = false

        stack { cell ->
            cell.grow()
            table {
                image("capsuleDevLabel1") {
                    it.grow()
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            //Close Place Marker UI
                            this@CharacterSelectUI.currentCharMarkerWindow.isVisible = false
                        }
                    }
                    )
                }
            }

            container {
                align(Align.topRight)
                button {
                    image("close-square-line-icon") {
                        it.size(50f)
                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            this@CharacterSelectUI.isVisible = false
                        }
                    })
                }
            }


        }
        currentCharMarkerWindow.isVisible = false
        this.addActor(currentCharMarkerWindow)


    }


    open fun refresh()
    {
        removeActor(currentCharMarkerWindow)
        currentConnections.forEach { removeActor(it) }
        currentConnections.clear()
        //Draw connections between places
        gameState.places.forEach { (placeName, place) ->
            if (!placeName.contains("home"))
            {
                place.connectedPlaces.forEach { connection ->
                    if (!connection.contains("home"))
                    {
                        addActor(Connection(gameState, placeName, connection).also {
                            it.color = Color.RED
                            currentConnections.add(it)
                        })
                    }
                }
            }
        }
        currentMarkers.forEach { removeActor(it) }
        currentMarkers.clear()
        addActor(currentCharMarkerWindow)
    }

    companion object
    {
        lateinit var instance: CharacterSelectUI

    }

}