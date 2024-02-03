package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class PlaceMarker(var gameState: GameState, place: String) : Button()
{
    init
    {
        //Set style of the button to the default skin.
        style = defaultSkin.get("diamond", Button.ButtonStyle::class.java)
        val start: Pair<Float, Float> = MapUI.convertToScreenCoords(
            gameState.places[place]!!.coordinates.x.toFloat(),
            gameState.places[place]!!.coordinates.y.toFloat()
        )
        //Set the size of the connection to the length of the line.
        setSize(30f, 30f)
        //Set the position of the connection to the start of the line.
        setPosition(start.first - width / 2, start.second - height / 2)
        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Open Place UI
                MapUI.instance.currentPlaceMarkerWindow.show(getX(), getY(), place)
            }
        })
    }


}