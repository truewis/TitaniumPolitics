package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.image
import ktx.scene2d.scene2d

class HomePlaceMarker(gameState: GameState, owner: MapUI, place: String) : PlaceMarker(gameState, owner, place)
{
    init
    {
        //Since place is set to home_characterName, we need to get the character's livingBy to get the actual place name.
        val place2 = gameState.characters[place.substring(5)]!!.livingBy
        val start: Pair<Float, Float> = owner.convertToScreenCoords(
            gameState.places[place2]!!.coordinates.x.toFloat(),
            gameState.places[place2]!!.coordinates.z.toFloat()
        )
        add(scene2d.image("HomeGrunge")).fill()
        //Set the position slightly to the right and down from the center of the place so that it does not overlap with the place marker.
        this.setPosition(start.first + this.width / 2, start.second - this.height)
    }


}