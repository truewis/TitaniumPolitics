package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.ClockUI.Companion.formatTime
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AvailableActionsUI(var gameState: GameState) : Table(defaultSkin)
{
    var titleLabel: Label
    private val docList = HorizontalGroup()
    private var isOpen = false;

    init
    {
        titleLabel = Label("AvailableActions", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        val docScr = ScrollPane(docList)
        docList.grow()

        add(docScr).grow()
        gameState.updateUI += { _ -> Gdx.app.postRunnable { refreshList(); } }
    }


    fun refreshList()
    {
        docList.clear()
        GameEngine.availableActions(
            gameState,
            gameState.characters[gameState.playerAgent]!!.place.name,
            gameState.playerAgent
        ).forEach { tobj ->
            val t = scene2d.button {

                label(tobj, "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                }
            }
            docList.addActor(t)
        }
        isVisible = !docList.children.isEmpty

    }


}