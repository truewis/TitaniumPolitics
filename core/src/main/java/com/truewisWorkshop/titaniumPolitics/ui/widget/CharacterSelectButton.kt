package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.label

class CharacterSelectButton(
    var availableCharacters: Set<String>? = null, callback: (String) -> Unit
) : Button(Scene2DSkin.defaultSkin, "default"), KTable {
    val charPortrait: SimpleHeadPortraitUI
    val charLabel: Label

    init {
        val defaultChar = availableCharacters?.firstOrNull() ?: "Someone"
        charPortrait = SimpleHeadPortraitUI(defaultChar, false)
        add(charPortrait).size(100f)
        row()
        charLabel =
            label(ReadOnly.charProp(defaultChar), "docTitle") { setFontScale(0.2f) }
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
                    setLabel(it)
                    charPortrait.tgtCharacter = it
                    callback(it)
                }
            }
        })

    }

    fun setLabel(characterName: String) {
        charLabel.setText(ReadOnly.charProp(characterName))
    }

    fun clearSelection() {
        charLabel.setText("")
        charPortrait.tgtCharacter = ""
    }
}