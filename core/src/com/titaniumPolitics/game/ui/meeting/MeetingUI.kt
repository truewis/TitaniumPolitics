package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.PortraitUI
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image
import ktx.scene2d.stack
import kotlin.math.cos
import kotlin.math.sin


//This UI is used for both meetings and conferences
class MeetingUI(var gameState: GameState) : Table(defaultSkin), KTable
{
    val portraits = arrayListOf<PortraitUI>()
    val speakerPortrait = PortraitUI("", gameState, 1f)
    val deployedInfos = arrayListOf<InfoBubbleUI>()
    val currentAgendas = arrayListOf<AgendaBubbleUI>()
    val currentAttention = Label("0", defaultSkin, "trnsprtConsole")
    val discussionTable: Stack

    init
    {
        instance = this
        //Red color for attention
        currentAttention.setColor(1f, 0f, 0f, 1f)
        currentAttention.setFontScale(3f)
        currentAttention.setAlignment(Align.center, Align.center)


        gameState.updateUI.add {
            if (it.player.currentMeeting != null)
            {
                println("MeetingUI: Refreshing meeting ${it.player.currentMeeting!!}")
                refresh(it.player.currentMeeting!!)
            }
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
        portraits.forEach {
            if (!meeting.currentCharacters.contains(it.tgtCharacter))
            {
                it.remove()
                portraits.remove(it)
            } else
                it.refresh(gameState)
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

        val portrait = PortraitUI(characterName, gameState, 0.2f)
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
        //Place portraits in a circle.
        val radius = 300f
        val centerX = discussionTable.x + discussionTable.width / 2
        val centerY = discussionTable.y + discussionTable.height / 2
        println("Center: $centerX, $centerY")
        portraits.forEach {
            if (it != speakerPortrait)
            {
                val angle = 360f / portraits.size * portraits.indexOf(it)
                it.setPosition(
                    centerX + radius * cos(Math.toRadians(angle.toDouble())).toFloat() - it.width / 2,
                    centerY + radius * sin(Math.toRadians(angle.toDouble())).toFloat() - it.height / 2
                )
            }
        }


    }

    companion object
    {
        lateinit var instance: MeetingUI
    }
}