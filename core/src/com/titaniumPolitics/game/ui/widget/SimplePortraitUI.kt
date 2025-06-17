package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.CharacterInteractionWindowUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.scene2d

class SimplePortraitUI(character: String, scale: Float, interactable: Boolean) : Table(Scene2DSkin.defaultSkin),
    KTable {
    init {
        background = skin.getDrawable("simpleBorder")
    }


    val portrait = scene2d.image("UserGrunge") {
        if (interactable)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    //Open Character Marker UI
                    CharacterInteractionWindowUI.Companion.instance.isVisible = true
                    val coord = localToStageCoordinates(Vector2(x, y))
                    CharacterInteractionWindowUI.Companion.instance.refresh(
                        coord.x,
                        coord.y,
                        this@SimplePortraitUI.tgtCharacter
                    )
                }
            })
    }
    var tgtCharacter = character
        set(value) {
            field = value
            try {
                portrait.drawable = TextureRegionDrawable(
                    CapsuleStage.Companion.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                        ReadOnly.charJson[tgtCharacter]!!.jsonObject["image"]!!.jsonPrimitive.content,
                        Texture::class.java
                    )!!
                )
            } catch (e: Exception) {
                println("Portrait Image Error: $value")
            }
        }

    init {
        tgtCharacter = character
        add(portrait).size(500f * scale, 700f * scale)
    }
}