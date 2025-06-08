package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.SimplePortraitUI
import ktx.scene2d.KTable
import ktx.scene2d.label

class CharacterSelectButton(skin: Skin, callback: (String) -> Unit) : Button(skin, "default"), KTable {
    val charPortrait: SimplePortraitUI
    val charLabel: Label
    var availableCharacters: Set<String>? = null

    init {
        charPortrait = SimplePortraitUI("", 0.15f)
        add(charPortrait).size(100f)
        row()
        charLabel = label("", "trnsprtConsole") { setFontScale(3f) }
        addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                CharacterSelectUI.instance.isVisible = true
                availableCharacters?.also {
                    CharacterSelectUI.instance.refresh("", it)
                } ?: run {
                    // If no characters are specified, use all characters from the game state.
                    CharacterSelectUI.instance.refresh()
                }
                CharacterSelectUI.instance.selectedCharacterCallback = {
                    CharacterSelectUI.instance.isVisible = false
                    setCharacter(it)
                    callback(it)
                }
            }
        })

    }

    fun setCharacter(characterName: String) {
        charLabel.setText(ReadOnly.prop(characterName))
    }
}