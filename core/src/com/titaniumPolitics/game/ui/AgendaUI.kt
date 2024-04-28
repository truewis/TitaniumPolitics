package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


//This UI is used for both meetings and conferences
class AgendaUI(var gameState: GameState) : Table(defaultSkin), KTable
{

    val AddedInfos = HorizontalGroup()
    val AgendaTitle = Label("Agenda", defaultSkin, "trnsprtConsole")

    init
    {
        AgendaTitle.setFontScale(2f)
        stack {
            image("panel") {
            }
            table {
                add(this@AgendaUI.AgendaTitle)
                row()
                add(this@AgendaUI.AddedInfos)
            }
        }


    }

    //This function can be used for both meetings and conferences
    fun refresh(meeting: Meeting, agenda: MeetingAgenda)
    {
        AgendaTitle.setText(agenda.subjectType)
        AddedInfos.clear()
        agenda.informationKeys.forEach {
            val infoCardUI = InfoCardUI(gameState)
            infoCardUI.refresh(gameState.informations[it]!!)
            AddedInfos.addActor(infoCardUI)
        }
    }


}