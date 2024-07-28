package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.*

class AllRealCharSelectionUI(var gState: GameState) : Table(), KTable
{
    init
    {
        verticalGroup {
            it.fill()
            with(this@AllRealCharSelectionUI.gState) {
                realCharList.forEach { realChar ->

                    container {
                        fill().size(100f)
                        button {
                            image(realChar) {
                                it.size(100f)

                            }
                        }
                    }
                }

            }
        }
    }
}