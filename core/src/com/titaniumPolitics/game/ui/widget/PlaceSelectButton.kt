package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.KTable
import ktx.scene2d.label
import ktx.scene2d.scene2d

class PlaceSelectButton(skin: Skin, callback: (String) -> Unit): Button(skin, "default"), KTable {
    val placeLabel = label("Place:", "trnsprtConsole") { setFontScale(3f) }
    init {

    addListener(object : ClickListener()
    {
        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
        {
            PlaceSelectionUI.instance.isVisible = true
            PlaceSelectionUI.instance.refresh()
            PlaceSelectionUI.instance.selectedPlaceCallback = {
                PlaceSelectionUI.instance.isVisible = false
                placeLabel.setText("Place: $it")
                callback(it)
            }
        }
    })

    }
}