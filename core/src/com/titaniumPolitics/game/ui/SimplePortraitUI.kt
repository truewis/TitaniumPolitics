package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class SimplePortraitUI(character: String, var gameState: GameState, scale: Float) : Table(defaultSkin), KTable
{


    val portrait = scene2d.image("UserGrunge") {
        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Open Character Marker UI
                CharacterInteractionWindowUI.instance.isVisible = true
                val coord = localToStageCoordinates(Vector2(x, y))
                CharacterInteractionWindowUI.instance.refresh(coord.x, coord.y, this@SimplePortraitUI.tgtCharacter)
            }
        })
    }
    var tgtCharacter = character
        set(value)
        {
            field = value
            try
            {
                portrait.drawable = TextureRegionDrawable(
                    CapsuleStage.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                        ReadOnly.charJson[tgtCharacter]!!.jsonObject["image"]!!.jsonPrimitive.content,
                        Texture::class.java
                    )!!
                )
            } catch (e: Exception)
            {
                println("Portrait Image Error: $value")
            }
        }

    init
    {
        tgtCharacter = character
        add(portrait).size(500f * scale, 700f * scale)
    }
}