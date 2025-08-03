package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.PortraitUI
import com.titaniumPolitics.game.ui.widget.SimpleHeadPortraitUI
import com.titaniumPolitics.game.ui.widget.SimplePortraitUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import java.lang.Thread.sleep


//This UI is used for both meetings and conferences
class MeetingUI(var gameState: GameState) : Table(defaultSkin), KTable {
    val portraits = arrayListOf<SimpleHeadPortraitUI>()
    val speakerPortrait = PortraitUI("", gameState, 1f)
    val deployedInfos = arrayListOf<InfoBubbleUI>()
    val currentAgendas = arrayListOf<AgendaBubbleUI>()
    val currentAttention = Label("0", defaultSkin, "docTitle")
    val discussionTable: Stack
    var previousMutualities = mutableMapOf<Pair<String, String>, Double>()
    val addAgendaButton = scene2d.button {
        stack {
            it.grow()
            image("BadgeRound")
        }
    }

    init {
        instance = this
        currentAttention.color = Color.RED
        currentAttention.setFontScale(0.6f)
        currentAttention.setAlignment(Align.center, Align.center)


        gameState.updateUI.add {
            if (it.player.currentMeeting != null) {
                println("MeetingUI: Refreshing meeting ${it.player.currentMeeting!!}")
                refresh(it.player.currentMeeting!!)
            }
        }
        GameEngine.onNonPlayerCharacterAction += { action ->
            if (isVisible && gameState.player.currentMeeting?.currentCharacters?.contains(action.sbjCharacter) ?: false) {
                //If the action is related to the current meeting, play the animation of mutuality arrows.
                sleep(200)//TODO:
            }
        }
        discussionTable = stack {
            it.grow()
            add(this@MeetingUI.speakerPortrait)
            container(this@MeetingUI.currentAttention) {
                padTop(300f)
            }
        }


    }

    //This function can be used for both meetings and conferences
    fun refresh(meeting: Meeting) {
        val newMutualities = meeting.currentCharacters.flatMap { char1 ->
            meeting.currentCharacters.map { char2 ->
                Pair(char1, char2) to gameState.getMutuality(char1, char2)
            }
        }.toMap().toMutableMap()
        //show mutuality arrows if there are any changes.
        val mutualityChanges = newMutualities.filter { (pair, value) ->
            previousMutualities[pair]?.let { it != value } ?: false
        }.mapValues {
            it.value - (previousMutualities[it.key] ?: 0.0)
        }
        if (mutualityChanges.isNotEmpty()) {
            showMutualityArrows(mutualityChanges)
            previousMutualities = newMutualities
        }
        placeCharacterPortrait()
        //Remove all bubbles before placing them again.
        removeBubbles()
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

    }

    val mutualityArrows = mutableListOf<MutualityArrowUI>()

    fun showMutualityArrows(mutualityChanges: Map<Pair<String, String>, Double>) {
        // 기존 화살표 제거(화면에서만)
        mutualityArrows.forEach { it.remove() }

        // 새 화살표 생성 및 애니메이션
        mutualityChanges.forEach { (pair, delta) ->
            val fromPortrait = portraits.find { it.tgtCharacter == pair.first } ?: return@forEach
            val toPortrait = portraits.find { it.tgtCharacter == pair.second } ?: return@forEach
            val arrow = MutualityArrowUI(fromPortrait, toPortrait, delta.toFloat(), skin)
            mutualityArrows.add(arrow)
            addActor(arrow)
            arrow.addAction(
                SequenceAction(
                    Actions.alpha(1f),
                    Actions.delay(2f),
                    Actions.alpha(0f),
                    Actions.run { arrow.visibleForReplay = false }
                )
            )
        }
    }

    fun replayMutualityArrows() {
        mutualityArrows.filter { it.visibleForReplay }.forEach { arrow ->
            arrow.clearActions()
            arrow.addAction(
                SequenceAction(
                    Actions.alpha(1f),
                    Actions.delay(2f),
                    Actions.alpha(0f)
                )
            )
            addActor(arrow)
        }
    }

    fun newMeeting(meeting: Meeting) {
        //If the meeting is a division leader election, add vote results to the stage if the meeting is over.
        if (meeting.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION) {
            meeting.onVoteResults += {
                val voteResultsTable = VoteResultWindowUI(meeting)
                CapsuleStage.instance.addActor(voteResultsTable)
                voteResultsTable.setPosition(
                    CapsuleStage.instance.width / 2 - voteResultsTable.width / 2,
                    CapsuleStage.instance.height / 2 - voteResultsTable.height / 2
                )
            }
        }

        previousMutualities = meeting.scheduledCharacters.flatMap { char1 ->
            meeting.currentCharacters.mapNotNull { char2 ->
                if (char1 != char2) {
                    val mutuality = gameState.getMutuality(char1, char2)
                    Pair(char1, char2) to mutuality
                } else null
            }
        }.toMap().toMutableMap()
    }

    private fun addCharacterPortrait(characterName: String) {

        val portrait = SimpleHeadPortraitUI(characterName, 0.2f, true)
        portrait.setSize(200f, 200f)
        portraits.add(portrait)
        addActor(portrait)
        portrait.layout()


    }

    //Cf. the same function in CharacterPortraitsUI
    private fun placeCharacterPortrait() {
        //Adjust the number of portraits based on the current meeting's characters.
        val currentMeeting = gameState.player.currentMeeting!!
        //If there are more portraits than characters, remove the excess portraits.
        if (portraits.size > currentMeeting.currentCharacters.size - 1) {
            val excessPortraits = portraits.subList(currentMeeting.currentCharacters.size - 1, portraits.size)
            excessPortraits.forEach { it.remove() }
            portraits.removeAll(excessPortraits)
        }
        //If there are fewer portraits than characters, add the missing portraits.
        if (portraits.size < currentMeeting.currentCharacters.size - 1) {
            val missingCharacters = currentMeeting.currentCharacters.filter { char ->
                portraits.none { it.tgtCharacter == char }
            }
            missingCharacters.forEach { addCharacterPortrait(it) }
        }
        //Assign the speaker portrait to the current speaker.
        speakerPortrait.tgtCharacter = currentMeeting.currentSpeaker
        //Assign the rest of the characters to each portrait.
        currentMeeting.currentCharacters.filter { it != currentMeeting.currentSpeaker }
            .forEachIndexed { index, character ->
                portraits[index].tgtCharacter = character
            }
        val portraitUIWidth = 200f
        val portraitUIHeight = 200f
        // Place portraits along the left and right walls of the discussion table.
        val leftX = discussionTable.x + portraitUIWidth * 3 // Adjust as needed to offset from the table
        val rightX = discussionTable.x + discussionTable.width - portraitUIWidth * 3

        val leftSideCount = (portraits.size) / 2
        val rightSideCount = portraits.size - leftSideCount

        val leftPortraits = portraits.take(leftSideCount)
        val rightPortraits = portraits.takeLast(rightSideCount)

        fun distributeVertically(portraits: List<SimpleHeadPortraitUI>, x: Float) {
            portraits.forEachIndexed { index, portrait ->
                val y =
                    discussionTable.y + discussionTable.height / 2 + portraitUIHeight * (index + 0.5f - portraits.size / 2f)
                portrait.setPosition(x, y, Align.center)
            }
        }

        distributeVertically(leftPortraits, leftX)
        distributeVertically(rightPortraits, rightX)


    }

    fun placeBubbles() {
        //Place bubbles in a circle. Agenda bubbles are placed in the inner circle.
        val centerX = discussionTable.x + discussionTable.width / 2
        val centerY = discussionTable.y + discussionTable.height / 2
        val agendasToPlace = currentAgendas.take(4)//Display maximum of four agendas.

        // Define padding and positions relative to the discussion table
        val agendaMargin = 200f
        val infoSpacingY = InfoBubbleUI.HEIGHT // vertical space between info bubbles
        val infoSpacingX = InfoBubbleUI.WIDTH

        // Corner positions for agendas
        val positions = listOf(
            Pair(
                centerX + agendaMargin,
                centerY + agendaMargin
            ), // upper-right
            Pair(
                centerX - agendaMargin,
                centerY + agendaMargin
            ), // upper-left
            Pair(centerX + agendaMargin, centerY - agendaMargin), // lower-right
            Pair(
                centerX - agendaMargin,
                centerY - agendaMargin
            ) // lower-left
        )

        agendasToPlace.forEachIndexed { index, agendaUI ->
            val (agendaX, agendaY) = positions[index]

            // Center the agenda at the corner position
            agendaUI.setPosition(agendaX, agendaY, Align.center)

            // Stack associated info bubbles vertically to the right of the agenda
            val infoStartX =
                if (index == 0 || index == 2) agendaUI.x + agendaUI.width + 20f else agendaUI.x - infoSpacingX - 20f
            var infoStartY = agendaUI.y + agendaUI.height / 2 + agendaUI.agenda.informationKeys.size * infoSpacingY / 2

            agendaUI.agenda.informationKeys.forEachIndexed { i, key ->
                val info = deployedInfos.find { it.info.name == key } ?: return@forEachIndexed
                info.setPosition(
                    infoStartX,
                    infoStartY - i * infoSpacingY
                )
            }
        }

        if (agendasToPlace.size < 4) {
            //TODO: addActor(addAgendaButton), but we are using AvailableActionsUI for now.
            val (agendaX, agendaY) = positions[agendasToPlace.size]
            addAgendaButton.setPosition(agendaX, agendaY, Align.center)
        }


    }

    fun removeBubbles() {
        currentAgendas.forEach {
            it.remove()
        }
        deployedInfos.forEach {
            it.remove()
        }

        currentAgendas.clear()
        deployedInfos.clear()
        //TODO: removeActor(addAgendaButton), but we are using AvailableActionsUI for now.
    }

    companion object {
        lateinit var instance: MeetingUI
    }
}