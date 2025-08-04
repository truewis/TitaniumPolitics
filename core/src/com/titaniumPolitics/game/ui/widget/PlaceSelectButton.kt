package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.KTable
import ktx.scene2d.label

class PlaceSelectButton(skin: Skin, callback: (String) -> Unit) : Button(skin, "default"), KTable {
    val placeLabel = label("Place:", "docTitle") { setFontScale(0.5f) }

    init {

        addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                PlaceSelectionUI.instance.isVisible = true
                PlaceSelectionUI.instance.refresh()
                PlaceSelectionUI.instance.selectedPlaceCallback = {
                    PlaceSelectionUI.instance.isVisible = false
                    placeLabel.setText(ReadOnly.prop(it))
                    callback(it)
                }
            }
        })

    }
}