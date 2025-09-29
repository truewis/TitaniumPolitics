package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.image
import ktx.scene2d.scene2d

class HomePlaceMarker(gameState: GameState, owner: MapUI, place: String) : PlaceMarker(gameState, owner, place) {
    init {
        if (place.contains("home_"))
            add(scene2d.image("HomeGrunge")).fill()//Home icon
        else
            add(scene2d.image("icon_common_18")).fill()//Generic building icon

    }


}