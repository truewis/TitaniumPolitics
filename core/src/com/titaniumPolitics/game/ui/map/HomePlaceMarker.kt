package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.image
import ktx.scene2d.scene2d

class HomePlaceMarker(gameState: GameState, owner: MapUI, place: String) : PlaceMarker(gameState, owner, place) {
    init {
        //Since place is set to home_characterName, we need to get the character's livingBy to get the actual place name.
        val place2 = gameState.places[place]!!
            .isBuildingIn!!
        val start: Pair<Float, Float> = owner.convertToScreenCoords(
            gameState.places[place2]!!.coordinates.x.toFloat(),
            gameState.places[place2]!!.coordinates.z.toFloat()
        )
        if (place.contains("home_"))
            add(scene2d.image("HomeGrunge")).fill()//Home icon
        else
            add(scene2d.image("icon_common_18")).fill()//Generic building icon
        //Set the position slightly to the right and down from the center of the place so that it does not overlap with the place marker.
        val angleDivision = gameState.places[place2]!!.numberOfBuildings
        val angleIndex = gameState.places[place]!!.buildingIndex!!
        val radius = 40f
        this.setPosition(
            start.first + radius * kotlin.math.cos(
                Math.toRadians(
                    90.0 + 360.0 / angleDivision * angleIndex
                )
            ).toFloat(),
            start.second + radius * kotlin.math.sin(
                Math.toRadians(
                    90.0 + 360.0 / angleDivision * angleIndex
                )
            ).toFloat(),
            Align.center
        )
    }


}