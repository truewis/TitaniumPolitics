package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

/**
 * A small building icon shown on the building ring of a [PlaceMarker].
 * It is deliberately not a PlaceMarker: buildings have no map coordinates and own no buildings.
 */
class HomePlaceMarker(gameState: GameState, val owner: MapUI, val place: String) : Image() {
    init {
        drawable = if (place.contains("home_"))
            defaultSkin.getDrawable("HomeGrunge")
        else
            MapArt.buildingDrawable(place)
        setSize(PlaceMarker.CHIP_SIZE, PlaceMarker.CHIP_SIZE)
        if (gameState.player.place.name == place) color = Color.RED
        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                owner.currentPlaceMarkerWindow.refresh(place)
            }
        })
    }

    override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
        if (owner.currentPlaceMarkerWindow.placeDisplayed == place) {
            val d = drawable
            if (d != null) {
                val grow = width * 0.3f
                batch.setColor(SELECTION_COLOR.r, SELECTION_COLOR.g, SELECTION_COLOR.b, parentAlpha * color.a)
                d.draw(batch, x - grow / 2, y - grow / 2, width + grow, height + grow)
            }
        }
        super.draw(batch, parentAlpha)
    }

    companion object {
        val SELECTION_COLOR = Color(1f, 0.85f, 0.2f, 1f)
    }
}