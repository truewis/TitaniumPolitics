package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.button
import ktx.scene2d.label

class PlaceSelectionUI(gameState: GameState) : MapUI(gameState) {
    init {
        instance = this
        isVisible = false
    }

    var selectedPlaceCallback: (String) -> Unit = {}
    override fun refresh() {
        super.refresh()
        currentPlaceMarkerWindow.mode = "PlaceSelection"
    }

    companion object {
        lateinit var instance: PlaceSelectionUI

    }
}