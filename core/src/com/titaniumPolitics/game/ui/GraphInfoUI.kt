package com.titaniumPolitics.game.ui

import GraphScreen
import com.titaniumPolitics.game.ui.widget.WindowUI

class GraphInfoUI : WindowUI("GraphTitle") {
    init {
        content.add(
            GraphScreen(
                hashMapOf(
                    1 to 2.0,
                    2 to 3.5,
                    3 to 2.8,
                    4 to 4.0,
                    5 to 3.6,
                    6 to 5.0,
                    7 to 4.8,
                    8 to 6.0,
                    9 to 5.5,
                    10 to 7.0
                )
            )
        ).grow()
    }

}
