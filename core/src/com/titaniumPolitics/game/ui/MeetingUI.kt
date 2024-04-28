package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d


//This UI is used for both meetings and conferences
class MeetingUI(var gameState: GameState) : Table(defaultSkin), KTable
{
    val portraits = arrayListOf<PortraitUI>()
    val availableInfos = HorizontalGroup()
    val currentAgendas = HorizontalGroup()
    val currentAttention = Label("0", defaultSkin, "trnsprtConsole")

    init
    {
        instance = this
        currentAgendas.pad(50f)
        //Red color for attention
        currentAttention.setColor(1f, 0f, 0f, 1f)


        gameState.updateUI.add {
            if (it.player.currentMeeting != null)
                refresh(it.player.currentMeeting!!)
        }
        add(currentAttention)
        row()
        label("Agendas", "trnsprtConsole") {
            setFontScale(2f)
        }
        row()
        add(currentAgendas)


    }

    //This function can be used for both meetings and conferences
    fun refresh(meeting: Meeting)
    {
        portraits.forEach { it.remove() }
        portraits.clear()
        meeting.currentCharacters.forEach {

            //Player can see themselves.
            addCharacterPortrait(it)
        }
        placeCharacterPortrait()
        currentAgendas.clear()
        meeting.agendas.forEach {
            val agendaUI = AgendaUI(gameState)
            agendaUI.refresh(meeting, it)
            currentAgendas.addActor(agendaUI)
        }
        availableInfos.clear()
        gameState.player.preparedInfoKeys.filter { key -> meeting.agendas.none { it.informationKeys.contains(key) } }
            .forEach {
                val infoUI = InfoCardUI(gameState)
                infoUI.refresh(gameState.informations[it]!!)
                availableInfos.addActor(infoUI)
            }
        currentAttention.setText(meeting.currentAttention.toString())
    }

    private fun addCharacterPortrait(characterName: String)
    {

        val portrait = PortraitUI(characterName, gameState)
        portraits.add(portrait)
        addActor(portrait)


    }

    //Cf. the same function in CharacterPortraitsUI
    private fun placeCharacterPortrait()
    {
        //reorder portraits so that the speaker is always on the center
        val speakerPortrait = portraits.find { it.tgtCharacter == gameState.player.currentMeeting!!.currentSpeaker }
        if (speakerPortrait != null)
        {
            portraits.remove(speakerPortrait)
            portraits.add(portraits.size / 2, speakerPortrait)
        }
        //Place portraits across the screen so they are not on top of each other.
        portraits.forEach {
            it.setPosition(
                (portraits.indexOf(it) + 0.5f) * CapsuleStage.instance.width / portraits.size + it.width / 2,
                300f
            )
        }


    }

    companion object
    {
        lateinit var instance: MeetingUI
    }
}