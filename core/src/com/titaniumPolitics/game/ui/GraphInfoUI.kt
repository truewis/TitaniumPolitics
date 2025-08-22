package com.titaniumPolitics.game.ui

import GraphScreen
import com.titaniumPolitics.game.ui.widget.WindowUI

class GraphInfoUI : WindowUI("GraphTitle") {
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
                )
            )
        ).grow()
    }

    fun refreshGraph(data: Map<Int, Float>) {
        content.clear()
        content.add(GraphScreen(data)).grow()
    }

    companion object {
        lateinit var instance: GraphInfoUI
    }
}
