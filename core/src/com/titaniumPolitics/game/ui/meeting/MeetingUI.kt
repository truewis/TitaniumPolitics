package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.PortraitUI
import com.titaniumPolitics.game.ui.SimplePortraitUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.math.cos
import kotlin.math.sin


//This UI is used for both meetings and conferences
class MeetingUI(var gameState: GameState) : Table(defaultSkin), KTable
{
    val portraits = arrayListOf<SimplePortraitUI>()
    val speakerPortrait = PortraitUI("", gameState, 1f)
    val deployedInfos = arrayListOf<InfoBubbleUI>()
    val currentAgendas = arrayListOf<AgendaBubbleUI>()
    val currentAttention = Label("0", defaultSkin, "trnsprtConsole")
    val discussionTable: Stack
    val attentionMeter: Image = image("BadgeRound")

    init
    {
        instance = this
        currentAttention.setColor(0f, 0f, 0f, 1f)
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
            add(this@MeetingUI.attentionMeter)
            add(this@MeetingUI.currentAttention)
        }
        discussionTable.debug()


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
        val iterator = portraits.iterator()
        while (iterator.hasNext())
        {
            val portrait = iterator.next()
            if (!meeting.currentCharacters.contains(portrait.tgtCharacter))
            {
                portrait.remove()
                iterator.remove()
            }
        }
        placeCharacterPortrait()
        //Remove all bubbles before placing them again.
        removeBubbles()
        currentAgendas.clear()
        deployedInfos.clear()
        meeting.agendas.forEach {
            val agendaUI = AgendaBubbleUI(it)
            currentAgendas += agendaUI
            addActor(agendaUI)
            it.informationKeys.forEach { key ->
                val info = gameState.informations[key]!!
                val infoUI = InfoBubbleUI(info)
                deployedInfos += infoUI
                addActor(infoUI)
            }
        }
        placeBubbles()

        currentAttention.setText(meeting.currentAttention.toString())
        attentionMeter.color = Color(meeting.currentAttention.toFloat() / 100, 0f, 0f, 1f)
    }

    private fun addCharacterPortrait(characterName: String)
    {

        val portrait = SimplePortraitUI(characterName, gameState, 0.2f)
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
            val angle = 360f / portraits.size * portraits.indexOf(it)
            it.setPosition(
                centerX + radius * cos(Math.toRadians(angle.toDouble())).toFloat() - it.width / 2,
                centerY + radius * sin(Math.toRadians(angle.toDouble())).toFloat() - it.height / 2
            )

        }


    }

    fun placeBubbles()
    {
        //Place bubbles in a circle. Agenda bubbles are placed in the inner circle.
        val radius = 200f
        val centerX = discussionTable.x + discussionTable.width / 2
        val centerY = discussionTable.y + discussionTable.height / 2
        currentAgendas.forEach {
            val angle = 360f / currentAgendas.size * currentAgendas.indexOf(it)
            it.setPosition(
                centerX + radius / 2 * cos(Math.toRadians(angle.toDouble())).toFloat() - it.width / 2,
                centerY + radius / 2 * sin(Math.toRadians(angle.toDouble())).toFloat() - it.height / 2
            )
            //Place information bubbles around the corresponding agenda bubble.
            val infoRadius = 100f
            val infoCenterX = it.x + it.width / 2
            val infoCenterY = it.y + it.height / 2
            it.agenda.informationKeys.forEach { key ->
                val info = deployedInfos.find { it.info.name == key }!!
                val infoAngle = 360f / it.agenda.informationKeys.size * it.agenda.informationKeys.indexOf(key)
                info.setPosition(
                    infoCenterX + infoRadius * cos(Math.toRadians(infoAngle.toDouble())).toFloat() - info.width / 2,
                    infoCenterY + infoRadius * sin(Math.toRadians(infoAngle.toDouble())).toFloat() - info.height / 2
                )
            }
        }


    }

    fun removeBubbles()
    {
        currentAgendas.forEach {
            it.remove()
        }
        deployedInfos.forEach {
            it.remove()
        }
    }

    companion object
    {
        lateinit var instance: MeetingUI
    }
}