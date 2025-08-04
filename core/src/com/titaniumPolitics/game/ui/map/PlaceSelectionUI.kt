package com.titaniumPolitics.game.ui.map

import com.titaniumPolitics.game.core.GameState

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