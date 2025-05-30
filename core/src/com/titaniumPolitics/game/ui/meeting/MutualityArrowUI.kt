package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import kotlin.math.atan2
import kotlin.math.sqrt

class MutualityArrowUI(
    val from: Actor,
    val to: Actor,
    val delta: Float
) : Actor() {
    var arrowColor: Color = if (delta > 0) Color.GREEN else Color.RED
    var arrowLength: Float = 100f + 200f * kotlin.math.abs(delta) // 기본 길이 + 변화량 비례
    var visibleForReplay: Boolean = true

    override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch?, parentAlpha: Float) {
        batch?.end()
        val renderer = stage.batch as? ShapeRenderer ?: return
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        renderer.color = arrowColor

        // from, to의 중심 좌표 계산
        val fromX = from.x + from.width / 2
        val fromY = from.y + from.height / 2
        val toX = to.x + to.width / 2
        val toY = to.y + to.height / 2

        // 방향 벡터
        val dx = toX - fromX
        val dy = toY - fromY
        val dist = sqrt(dx * dx + dy * dy)
        val normX = dx / dist
        val normY = dy / dist

        // 화살표 선
        val endX = fromX + normX * arrowLength
        val endY = fromY + normY * arrowLength
        renderer.rectLine(fromX, fromY, endX, endY, 8f + 12f * kotlin.math.abs(delta))

        // 화살촉
        val angle = atan2(dy, dx)
        val arrowHeadSize = 30f + 20f * kotlin.math.abs(delta)
        val leftX = endX - arrowHeadSize * kotlin.math.cos(angle - 0.4)
        val leftY = endY - arrowHeadSize * kotlin.math.sin(angle - 0.4)
        val rightX = endX - arrowHeadSize * kotlin.math.cos(angle + 0.4)
        val rightY = endY - arrowHeadSize * kotlin.math.sin(angle + 0.4)
        renderer.triangle(
            endX, endY,
            leftX.toFloat(), leftY.toFloat(),
            rightX.toFloat(), rightY.toFloat()
        )
        renderer.end()
        batch?.begin()
    }
}