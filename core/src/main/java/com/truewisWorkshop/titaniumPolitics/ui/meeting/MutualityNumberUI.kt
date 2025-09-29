package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.label
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.log
import kotlin.math.sqrt

class MutualityNumberUI(
    val from: Actor,
    val to: Actor,
    val delta: Float
) : Table(Scene2DSkin.defaultSkin), KTable {
    init {
        color = if (delta > 0) Color.GREEN else Color.RED
        val startX = from.x + from.width / 2
        val startY = from.y + from.height / 2
        val endX = to.x + to.width / 2
        val endY = to.y + to.height / 2
        val angle = atan2(endY - startY, endX - startX)
        //setPosition((startX + endX) / 2, (startY + endY) / 2, Align.center)
        //rotation = Math.toDegrees(angle.toDouble()).toFloat()
        //height = log(abs(delta) + 1, 2f) * 10f // 로그 스케일로 높이 조정
        //width = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
        label(
            if (delta > 0) "+${"%.1f".format(delta)}" else "%.1f".format(delta),
            "docTitle"
        ) {
            setAlignment(Align.center)
            color = if (this@MutualityNumberUI.delta > 0) Color.GREEN else Color.RED
            setFontScale(0.3f + log(abs(this@MutualityNumberUI.delta) + 1, 2f) * 0.05f) // 로그 스케일로 폰트 크기 조정
        }
        addAction(
            Actions.sequence(
                Actions.moveTo(startX, startY),
                Actions.parallel(
                    Actions.moveBy(0f, 20f, 0.5f),
                    Actions.fadeOut(0.5f)
                )
            )
        )
    }

    var visibleForReplay: Boolean = true

}