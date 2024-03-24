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
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d


//This UI is used for both meetings and conferences
class AgendaUI(var gameState: GameState) : Table(defaultSkin), KTable
{

    val AddedInfos = HorizontalGroup()
    val AgendaTitle = Label("Agenda", defaultSkin, "trnsprtConsole")
    val AgendaAgreement = Label("0", defaultSkin, "trnsprtConsole")
    val AgendaDeltaAgreement = Label("+0", defaultSkin, "trnsprtConsole")

    init
    {
        AgendaTitle.setFontScale(2f)
        AgendaAgreement.setFontScale(2f)
        AgendaDeltaAgreement.setFontScale(2f)



        add(AgendaTitle).colspan(2)
        row()
        add(AgendaAgreement)
        add(AgendaDeltaAgreement)
    }

    //This function can be used for both meetings and conferences
    fun refresh(meeting: Meeting, agenda: MeetingAgenda)
    {
        AgendaTitle.setText(agenda.subjectType)
    }


}