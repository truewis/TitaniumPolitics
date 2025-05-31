package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.random.Random

class MutualityMeter(var gameState: GameState, var tgtCharacter: String, var who: String) : Table(defaultSkin), KTable
{
    val bar1 = MeterOvalUI()
    val bar2 = MeterOvalUI()


    val refresh =
        { state: GameState ->
            setValue(
                state.getMutuality(tgtCharacter, who) / 100f,
                state.getMutuality(who, tgtCharacter) / 100f
            )
        }

    init
    {
        stack {
            it.grow()

            table {
                add(this@MutualityMeter.bar1).size(300f, 30f)
                row()
                add(this@MutualityMeter.bar2).size(300f, 30f)
            }
            container {
                size(50f, 50f)
                image("icon_gesture_58") {
                    color = Color(1f, 1f, 1f, 0.7f)
                }

            }
        }

//        textTooltip("${(bar.fill * 100).toInt()}", "default") {
//            this.setFontScale(2f)
//            it.manager.initialTime = 0.5f
//        }
        gameState.updateUI += refresh
        refresh(gameState)
        val tgtName = ReadOnly.charName(tgtCharacter)
        var text = if (gameState.getMutuality(tgtCharacter, who) > 75)
        {
            "You think of $tgtName as trustworthy.\n"
        } else if (gameState.getMutuality(tgtCharacter, who) > 50)
        {
            "You think of $tgtName as reasonable.\n"
        } else if (gameState.getMutuality(tgtCharacter, who) > 25)
        {
            "You think of $tgtName as untrustworthy.\n"
        } else
        {
            "You hates $tgtName.\n"
        }

        if (gameState.getMutuality(who, tgtCharacter) > 75)
        {
            text += "They think of you as trustworthy."
        } else if (gameState.getMutuality(who, tgtCharacter) > 50)
        {
            text += "They think of you as reasonable."
        } else if (gameState.getMutuality(who, tgtCharacter) > 25)
        {
            text += "They think of you as untrustworthy."
        } else
        {
            text += "They hates you."
        }
        addListener(SimpleTextTooltipUI(text))
    }


    override fun remove(): Boolean
    {
        gameState.updateUI -= refresh
        return super.remove()
    }

    fun setValue(value1: Double, value2: Double)
    {
        bar1.setValue(value1.toFloat())
        bar2.setValue(value2.toFloat())
    }
}
