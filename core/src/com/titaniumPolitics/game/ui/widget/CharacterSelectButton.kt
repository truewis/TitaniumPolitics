package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import ktx.scene2d.KTable
import ktx.scene2d.label

class CharacterSelectButton(skin: Skin, callback: (String) -> Unit): Button(skin, "default"), KTable {
    val placeLabel = label("Character:", "trnsprtConsole") { setFontScale(3f) }
    init {

        addListener(object : ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                CharacterSelectUI.instance.isVisible = true
                CharacterSelectUI.instance.refresh()
                CharacterSelectUI.instance.selectedCharacterCallback = {
                    CharacterSelectUI.instance.isVisible = false
                    placeLabel.setText("Character: $it")
                    callback(it)
                }
            }
        })

    }
}