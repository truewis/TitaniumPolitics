package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.ui.CharactersInPlaceUI
import com.titaniumPolitics.game.ui.InfoCardUI
import ktx.scene2d.Scene2DSkin.defaultSkin

class AvailableInfoUI(var gameState: GameState) : Table(defaultSkin) {
    var titleLabel: Label
    private val docList = HorizontalGroup()

    init {
        titleLabel = Label("Options", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        docList.grow()

        add(docList).size(300f, 100f)
    }

    override fun setVisible(visible: Boolean) {
        CharactersInPlaceUI.instance.isVisible = !visible
        super.setVisible(visible)
    }

    fun refresh(meeting: Meeting) {
        docList.clear()
        gameState.player.preparedInfoKeys.filter { key -> meeting.agendas.none { it.informationKeys.contains(key) } }
            .forEach {
                val infoUI = InfoCardUI(gameState)
                infoUI.refresh(gameState.informations[it]!!)
                docList.addActor(infoUI)
            }
    }


}