package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.AlertUI
import com.titaniumPolitics.game.ui.AssistantUI
import com.titaniumPolitics.game.ui.AvailableActionsUI
import com.titaniumPolitics.game.ui.CharStatusUI
import ktx.scene2d.*

class MapUI(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable
{
    val currentConnections = arrayListOf<Connection>()

    init
    {
        instance = this
        stack { cell ->
            cell.grow()
            image("capsuleDevLabel1") {
                setFillParent(true)
            }
            //A button to hide this UI on the bottom left of the screen.
            button {
                cell.align(Align.bottomLeft)
                cell.pad(10f)
                cell.expandY()
                cell.fill()
                label("Hide Map") {
                    setAlignment(Align.center)
                }
                addListener(object : ClickListener()
                {
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                    {
                        instance.isVisible = false
                    }
                })
            }


        }
        //Draw connections between places
        gameState.places.forEach { (placeName, place) ->
            place.connectedPlaces.forEach { connection ->
                addActor(Connection(gameState, placeName, connection).also {
                    it.color = Color.RED
                    currentConnections.add(it)
                })
            }
        }


    }

    fun refresh()
    {
        currentConnections.forEach { removeActor(it) }
        currentConnections.clear()
        //Draw connections between places
        gameState.places.forEach { (placeName, place) ->
            place.connectedPlaces.forEach { connection ->
                addActor(Connection(gameState, placeName, connection).also {
                    it.color = Color.RED
                    currentConnections.add(it)
                })
            }
        }
    }

    companion object
    {
        //Singleton
        lateinit var instance: MapUI

        fun convertToScreenCoords(x: Float, y: Float): Pair<Float, Float>
        {
            return Pair(x * 32f, y * 32f)
        }
    }

}