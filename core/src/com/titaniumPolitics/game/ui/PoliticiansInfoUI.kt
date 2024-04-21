package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

//Human Resource Management is currently done without information. The report is instant.
class PoliticiansInfoUI : Table(defaultSkin), KTable
{
    private val dataTable = Table()

    init
    {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        stack {
            it.grow()
            image("panel") {
            }
            add(informationPane)

        }
        row()
        button {
            it.fill()
            label("Close") {
                setAlignment(Align.center)
                setFontScale(2f)

            }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    this@PoliticiansInfoUI.isVisible = false
                }
            })
        }


    }

    fun refresh(gameState: GameState)
    {
        dataTable.clear()
        dataTable.apply {

            add(label("The Mechanic: ${gameState.characters.filter { it.value.trait.contains("mechanic") }.keys.firstOrNull() ?: "None"}") {
                setAlignment(
                    Align.center
                )
            })
            row()
            gameState.parties.filter { it.value.type == "division" }.forEach {
                add(label("${it.key} Division Leader: ${it.value.leader}") {
                    setAlignment(
                        Align.center
                    )
                })
                row()
            }

        }

    }

    companion object
    {
        //Singleton
        lateinit var instance: PoliticiansInfoUI
    }


}