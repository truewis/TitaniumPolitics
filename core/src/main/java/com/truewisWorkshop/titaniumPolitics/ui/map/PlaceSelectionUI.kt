package com.titaniumPolitics.game.ui.map

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.widget.WindowUI

class PlaceSelectionUI(gameState: GameState) : WindowUI("PlaceSelectionTitle") {
    val map = MapUI(gameState)

    init {
        instance = this
        isVisible = false
        content.add(map).grow()
    }

    var selectedPlaceCallback: (String) -> Unit = {}
    fun refresh() {
        map.refresh()
        map.currentPlaceMarkerWindow.mode = "PlaceSelection"
    }

    companion object {
        lateinit var instance: PlaceSelectionUI

    }
}