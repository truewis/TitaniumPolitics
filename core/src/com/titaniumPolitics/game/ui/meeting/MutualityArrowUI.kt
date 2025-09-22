package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Align
import ktx.scene2d.Scene2DSkin
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.log
import kotlin.math.sqrt

class MutualityArrowUI(
    val from: Actor,
    val to: Actor,
    val delta: Float
) : Image(Scene2DSkin.defaultSkin, "Arrow2Right") {
    init {
        color = if (delta > 0) Color.GREEN else Color.RED
        val startX = from.x + from.width / 2
        val startY = from.y + from.height / 2
        val endX = to.x + to.width / 2
        val endY = to.y + to.height / 2
        val angle = atan2(endY - startY, endX - startX)
        setPosition(startX, startY)
        setPosition(startX, startY, Align.center)
        rotation = Math.toDegrees(angle.toDouble()).toFloat()
        height = log(abs(delta) + 1, 2f) * 10f // 로그 스케일로 높이 조정
        width = sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY))
        addAction(
            Actions.sequence(
                Actions.delay(1f),
                Actions.run { isVisible = false }
            )
        )
    }

    var visibleForReplay: Boolean = true

}