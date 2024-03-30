package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.Place

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
            image("capsuleDevLabel1") {
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

            add(label("The Mechanic: ${gameState.characters.filter { it.value.trait.contains("mechanic") }.keys.first()}") {
                setAlignment(
                    Align.center
                )
            })
            row()
            add(label("Infrastructure Division Leader: ${gameState.characters.filter { it.value.trait.contains("infraMinister") }.keys.first()}") {
                setAlignment(
                    Align.center
                )
            })
            row()
            add(label("Interior Division Leader: ${gameState.characters.filter { it.value.trait.contains("interiorMinister") }.keys.first()}") {
                setAlignment(
                    Align.center
                )
            })
            row()
            add(label("Safety Division Leader: ${gameState.characters.filter { it.value.trait.contains("safetyMinister") }.keys.first()}") {
                setAlignment(
                    Align.center
                )
            })
            row()
            add(label("Education Division Leader: ${gameState.characters.filter { it.value.trait.contains("eduMinister") }.keys.first()}") {
                setAlignment(
                    Align.center
                )
            })
            row()
            add(label("Industry Division Leader: ${gameState.characters.filter { it.value.trait.contains("industryMinister") }.keys.first()}") {
                setAlignment(
                    Align.center
                )
            })
            row()
            add(label("Bioengineering Division Leader: ${gameState.characters.filter { it.value.trait.contains("bioMinister") }.keys.first()}") {
                setAlignment(
                    Align.center
                )
            })
            row()
            add(label("Mining Division Leader: ${gameState.characters.filter { it.value.trait.contains("miningMinister") }.keys.first()}") {
                setAlignment(
                    Align.center
                )
            })
        }

    }

    companion object
    {
        //Singleton
        lateinit var instance: PoliticiansInfoUI
    }


}