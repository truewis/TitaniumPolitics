package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.gameActions.Examine
import com.titaniumPolitics.game.ui.CharacterPortraitsUI
import com.titaniumPolitics.game.ui.InfoCardUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AvailableInfoUI(var gameState: GameState) : Table(defaultSkin)
{
    var titleLabel: Label
    private val docList = HorizontalGroup()

    init
    {
        titleLabel = Label("Options", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        docList.grow()

        add(docList).size(300f, 100f)
    }

    override fun setVisible(visible: Boolean)
    {
        CharacterPortraitsUI.instance.isVisible = !visible
        super.setVisible(visible)
    }

    fun refresh(meeting: Meeting)
    {
        docList.clear()
        gameState.player.preparedInfoKeys.filter { key -> meeting.agendas.none { it.informationKeys.contains(key) } }
            .forEach {
                val infoUI = InfoCardUI(gameState)
                infoUI.refresh(gameState.informations[it]!!)
                docList.addActor(infoUI)
            }
    }


}