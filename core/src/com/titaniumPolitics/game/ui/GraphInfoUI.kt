package com.titaniumPolitics.game.ui

import GraphScreen
import GraphScreen.LineAttributes
import com.badlogic.gdx.graphics.Color
import com.titaniumPolitics.game.ui.widget.WindowUI
import kotlin.collections.set

class GraphInfoUI : WindowUI("GraphTitle") {

    private val graph get() = (content.getChild(0) as GraphScreen)

    init {
        instance = this
        isVisible = false
        content.add(
            GraphScreen(
                hashMapOf(
                    1 to 2.0f,
                    2 to 3.5f,
                    3 to 2.8f,
                    4 to 4.0f,
                    5 to 3.6f,
                    6 to 5.0f,
                    7 to 4.8f,
                    8 to 6.0f,
                    9 to 5.5f,
                    10 to 7.0f
                ),
                GraphScreen.DataType.COUNT
            )
        ).grow()
    }

    fun refreshGraph(data: Map<Int, Float>, yDataType: GraphScreen.DataType) {
        graph.refresh(data, yDataType)
    }


    fun addHorizontalLine(yValue: Float, color: Color = Color.BLACK, thickness: Float = 2f, key: String) {
        graph.addHorizontalLine(yValue, color, thickness, key)
    }

    fun removeHorizontalLine(key: String) {
        graph.removeHorizontalLine(key)
    }

    fun addVerticalLine(xValue: Float, color: Color = Color.BLACK, thickness: Float = 2f, key: String) {
        graph.addVerticalLine(xValue, color, thickness, key)
    }

    fun removeVerticalLine(key: String) {
        graph.removeVerticalLine(key)
    }

    companion object {
        lateinit var instance: GraphInfoUI
    }
}
