package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.PortraitUI
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image
import ktx.scene2d.stack


//This UI is used for both meetings and conferences
class MeetingUI(var gameState: GameState) : Table(defaultSkin), KTable
{
    val portraits = arrayListOf<PortraitUI>()
    val speakerPortrait = PortraitUI("", gameState)
    val deployedInfos = arrayListOf<InfoBubbleUI>()
    val currentAgendas = arrayListOf<AgendaBubbleUI>()
    val currentAttention = Label("0", defaultSkin, "trnsprtConsole")
    val discussionTable: Stack

    init
    {
        instance = this
        //Red color for attention
        currentAttention.setColor(1f, 0f, 0f, 1f)


        gameState.updateUI.add {
            if (it.player.currentMeeting != null)
                refresh(it.player.currentMeeting!!)
        }
        add(speakerPortrait).grow()
        discussionTable = stack {
            it.size(800f, 800f)
            image("BadgeRound") {

            }
            add(this@MeetingUI.currentAttention)
        }


    }

    //This function can be used for both meetings and conferences
    fun refresh(meeting: Meeting)
    {
        meeting.currentCharacters.forEach {
            if (portraits.none { portrait -> portrait.tgtCharacter == it })
            {
                //Player can see themselves.
                addCharacterPortrait(it)
            }
        }
        placeCharacterPortrait()
        currentAgendas.clear()
//        meeting.agendas.forEach {
//            val agendaUI = AgendaUI(gameState)
//            agendaUI.refresh(meeting, it)
//            currentAgendas.addActor(agendaUI)
//        }

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

        //reorder portraits so that the speaker is always on a different position.
        if (speakerPortrait.tgtCharacter != gameState.player.currentMeeting!!.currentSpeaker)
        {
            val oldSpeakerPortrait =
                portraits.find { it.tgtCharacter == gameState.player.currentMeeting!!.currentSpeaker }!!
            val oldCharacter = oldSpeakerPortrait.tgtCharacter
            oldSpeakerPortrait.tgtCharacter = gameState.player.currentMeeting!!.currentSpeaker
            speakerPortrait.tgtCharacter = oldCharacter
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