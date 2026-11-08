package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.SpeechInterpreter
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.AlertUI
import com.titaniumPolitics.game.ui.BlockingWarningUI
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.DialogueUI
import com.titaniumPolitics.game.ui.HeadPortraitUI
import com.titaniumPolitics.game.ui.InformationViewUI
import com.titaniumPolitics.game.ui.PortraitUI
import com.titaniumPolitics.game.ui.widget.DivisionBannerUI
import kotlinx.coroutines.runBlocking
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import java.util.IdentityHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


//This UI is used for both meetings and conferences
class MeetingUI(var gameState: GameState) : Table(defaultSkin), KTable {
    val portraits = arrayListOf<HeadPortraitUI>()
    private val animationQueue = ArrayDeque<Action>()
    var onAnimationEnd: () -> Unit = {}

    /**
     * Block the game engine until the animation is done.
     */
    fun flushAnimation() {
        runBlocking {
            suspendCoroutine { continuation ->
                if (animationQueue.isNotEmpty()) {
                    addAction(animationQueue.removeFirst())
                }
                onAnimationEnd =
                    {
                        try {
                            continuation.resume(Unit)
                        } catch (e: IllegalStateException) {
                            // This can happen if the coroutine was already resumed.
                            println("Continuation was already resumed!")
                        }
                    }
            }
        }

    }

    fun addAnimation(action: Action) {
        animationQueue.add(
            Actions.sequence(
                action,
                Actions.run {
                    if (animationQueue.isNotEmpty())
                        addAction(animationQueue.removeFirst())
                    else
                        onAnimationEnd()
                }
            )
        )
    }

    val speakerPortrait = PortraitUI("", gameState)
    val deployedInfos = arrayListOf<InfoBubbleUI>()
    val currentAgendas = arrayListOf<AgendaBubbleUI>()
    private val informationArms = InformationArmsLayer()
    private val informationPositions = hashMapOf<String, Pair<Float, Float>>()
    private var improvisedQuestUI: Table? = null
    val currentAgendaMarker = scene2d.image("BadgeRound") {
        setSize(110f, 110f)
        color = Color(1f, 0.85f, 0.2f, 0.9f)
        isVisible = false
    }
    private val currentAgendaMarkerBaseAlpha = 0.9f
    val currentAttention = Label("0", defaultSkin, "docTitle")
    val discussionTable: Stack
    val electionUIContainer = Container<ElectionUI>()
    private val registeredMeetingCallbacks = hashSetOf<String>()
    private val agendaPositions = IdentityHashMap<MeetingAgenda, Pair<Float, Float>>()
    private val agendaFeedbackActors = arrayListOf<Actor>()
    private var agendaPositionsMeetingId: String? = null
    private val meetingMutualitySnapshots = hashMapOf<String, Map<Pair<String, String>, Double>>()
    private val meetingKnownInfoSnapshots = hashMapOf<String, Set<String>>()
    val meetingInfoUI = scene2d.table {
    }
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
                Logger.write("MeetingUI: Refreshing meeting ${it.player.currentMeeting!!}", Logger.LogLevel.INFO)
                refresh(it.player.currentMeeting!!)
            }
        }
        gameState.onAgendaPersuasivenessChanged += { meeting, agenda, delta ->
            Gdx.app.postRunnable {
                if (isVisible && gameState.player.currentMeeting === meeting) {
                    showAgendaPersuasivenessFeedback(agenda, delta)
                }
            }
        }
        //Listen for mutuality changes between characters in the current meeting.
        //When a new mutuality reason is added, show the mutuality arrows if both characters are in the current meeting and they are not the same character.
        gameState.onNewMutualityReason += { char1, char2, delta, reason ->
            if (isVisible && gameState.player.currentMeeting?.currentCharacters?.contains(char1) == true && gameState.player.currentMeeting?.currentCharacters?.contains(
                    char2
                ) == true && char1 != char2
            ) {
                Logger.write(
                    "MeetingUI: New mutuality reason detected between $char1 and $char2: $reason ($delta)",
                    Logger.LogLevel.INFO
                )
                showMutualityArrows(char1, char2, delta, reason)
            }
        }
        GameEngine.onBeforeNonPlayerCharacterAction += { action ->
            if (isVisible && gameState.player.currentMeeting?.currentCharacters?.contains(action.sbjCharacter) ?: false && action !is Wait) {
                Logger.write(
                    "MeetingUI: Non-player character action detected: ${action.sbjCharacter} performed ${action::class.simpleName}",
                    Logger.LogLevel.INFO
                )
                refresh(gameState.player.currentMeeting!!)
                SpeechInterpreter.actionLines(action, gameState, gameState.player.currentMeeting).forEach { line ->
                    displaySpeechLine(line)
                }

            }
        }
        discussionTable = stack {
            it.grow().size(1920f, 1080f)
            add(this@MeetingUI.meetingInfoUI)
            add(this@MeetingUI.electionUIContainer)
            container(this@MeetingUI.speakerPortrait) {
                this.size(450f, 600f)
            }
            container(scene2d.table {
                label(ReadOnly.prop("meeting-attention"), "docTitle") {
                    setFontScale(0.2f)
                    color = Color.RED
                    setAlignment(Align.center, Align.center)
                }
                row()
                add(this@MeetingUI.currentAttention)
            }) {
                this.padTop(300f)
            }
        }

        addActorAt(0, informationArms)
        addActor(currentAgendaMarker)

    }

    //This function can be used for both meetings and conferences
    fun refresh(meeting: Meeting) {
        Gdx.app.postRunnable {
            if (agendaPositionsMeetingId != meeting.ID) {
                clearAgendaPersuasivenessFeedback()
                agendaPositions.clear()
                informationPositions.clear()
                agendaPositionsMeetingId = meeting.ID
            }
            meetingInfoUI.clear()
            meetingInfoUI.also {
                it.top()
                it.pad(10f)
                it.label(this@MeetingUI.gameState.meetingDisplayName(meeting), "docTitle")
                meeting.involvedParty?.run {
                    val party = this@MeetingUI.gameState.parties[this]!!
                    if (party.type == Party.Type.DIVISION) {
                        it.row()
                        it.add(DivisionBannerUI(party, 600f))
                    }
                }
                it.row()
                it.add().size(1920f, 600f)
            }

            //If the meeting is a division leader election, set the election UI after the candidates are set.
            if (meeting.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION)
                meeting.onCandidatesSet += {
                    electionUIContainer.setActor(
                        ElectionUI(
                            gameState,
                            gameState.parties[meeting.involvedParty]!!,
                            it
                        )
                    )
                }


            placeCharacterPortrait()
            updateImprovisedQuestDisplay(meeting)
            //Remove all bubbles before placing them again.
            removeBubbles()
            meeting.agendas.forEach {
                val agendaUI = AgendaBubbleUI(it)
                currentAgendas += agendaUI
                addActor(agendaUI)
                it.informationKeys.forEach { key ->
                    val info = gameState.informations[key]!!
                    val preJoin = key in meeting.infoKeysBeforePlayerJoined || gameState.playerName !in info.knownTo
                    val presenter = meeting.informationPresenters[key]
                    val infoUI = InfoBubbleUI(info, presenter, preJoin) {
                        if (!preJoin && gameState.playerName in info.knownTo)
                            InformationViewUI.displayInformation(info)
                        else
                            BlockingWarningUI.instance.display("unknownInfo", null)
                    }
                    deployedInfos += infoUI
                    val arms = InformationArmsUI(this@MeetingUI, infoUI, agendaUI, presenter?.let(::findPortrait), informationArms)
                    informationArms.addActor(arms)
                    addActor(infoUI)
                }
            }
            placeBubbles()
            bringAgendaPersuasivenessFeedbackToFront()

            currentAttention.setText(meeting.currentAttention.toString())
        }

    }

    val mutualityArrows = mutableListOf<MutualityArrowUI>()

    fun findPortrait(characterName: String): Actor? {
        return portraits.find { it.tgtCharacter == characterName }
            ?: speakerPortrait.takeIf { it.tgtCharacter == characterName }
    }

    fun showMutualityArrows(char1: String, char2: String, delta: Double, reason: String?) {
        // 기존 화살표 제거(화면에서만)
        mutualityArrows.forEach { it.remove() }

        // 새 화살표 생성 및 애니메이션
        val fromPortrait = findPortrait(char1) ?: return
        val toPortrait = findPortrait(char2) ?: return
        val arrow = MutualityArrowUI(fromPortrait, toPortrait, delta.toFloat())
        mutualityArrows.add(arrow)
        addActor(arrow)
        //There are gradually increasing mutuality changes that do not need speech display.
        if (delta > 1)
            reason?.let {
                //Split reason by semicolon, first element is the reason key, rest are parameters.
                val key = it.split(";")[0]
                val params = it.split(";").drop(1)
                SpeechInterpreter.scriptedLines(
                    primarySpeaker = char1,
                    otherSpeaker = char2,
                    key = "response-$key",
                    styleSpeaker = char1,
                    formatArgs = params.toTypedArray()
                ).forEach { line ->
                    displaySpeechLine(line)
                }
                if (ReadOnly.hasScript("response2-$key", char2)) {
                    SpeechInterpreter.scriptedLines(
                        primarySpeaker = char2,
                        otherSpeaker = char1,
                        key = "response2-$key",
                        styleSpeaker = char2,
                        formatArgs = params.toTypedArray()
                    ).forEach { line ->
                        displaySpeechLine(line)
                    }
                }
            }
//            addAnimation(
//                Actions.run {
//                    arrow.addAction(
//                        SequenceAction(
//                            Actions.fadeIn(0.2f),
//                            Actions.delay(0.4f),
//                            Actions.fadeOut(0.2f),
//                            Actions.run {
//                                arrow.visibleForReplay = false
//                            }
//                        )
//                    )
//                }
//            )


//        if (mutualityChanges.isNotEmpty())
//            flushAnimation()
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

    fun displayMeeting(meeting: Meeting) {
        registerMeetingCallbacks(meeting)

        if (gameState.time == meeting.startTime + 1) //Only display the meeting if it's right after the starting time. This works because updateUI is called after every timestep advance.
            if (meeting.type == Meeting.MeetingType.TALK && meeting.currentCharacters.size == 2) {
                val characters = meeting.currentCharacters.toList() - meeting.currentSpeaker!!
                val char1 = gameState.characters[meeting.currentSpeaker]!!
                val char2 = gameState.characters[characters.first()]!!
                DialogueUI.instance.playTalkDialogue(char1, char2)
            } else {
                DialogueUI.instance.playMeetingDialogue(meeting)
            }
    }

    private fun addCharacterPortrait(characterName: String) {
        val portrait = HeadPortraitUI(characterName, gameState)
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
            val missingCount = currentMeeting.currentCharacters.size - 1 - portraits.size
            repeat(missingCount) {
                addCharacterPortrait(currentMeeting.currentCharacters.first())
            }
        }
        //Assign the speaker portrait to the current speaker.
        speakerPortrait.tgtCharacter = currentMeeting.currentSpeaker!!
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

        fun distributeVertically(portraits: List<HeadPortraitUI>, x: Float) {
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
        val agendasToPlace = currentAgendas

        // Define padding and positions relative to the discussion table
        val agendaMargin = 200f

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

        var markerPlaced = false
        agendasToPlace.forEachIndexed { index, agendaUI ->
            val (agendaX, agendaY) = if (agendasToPlace.size <= positions.size) positions[index] else {
                val angle = 2.0 * Math.PI * index / agendasToPlace.size
                (centerX + 330f * kotlin.math.cos(angle).toFloat()) to
                        (centerY + 330f * kotlin.math.sin(angle).toFloat())
            }

            // Center the agenda at the corner position
            agendaUI.setPosition(agendaX, agendaY, Align.center)
            agendaPositions[agendaUI.agenda] = agendaX to agendaY
            if (agendaUI.agenda == gameState.player.currentMeeting?.currentAgenda) {
                currentAgendaMarker.setPosition(agendaX, agendaY, Align.center)
                currentAgendaMarker.isVisible = true
                currentAgendaMarker.zIndex = maxOf(0, agendaUI.zIndex - 1)
                ensureCurrentAgendaMarkerAnimation()
                markerPlaced = true
            }

        }

        val obstacles = agendasToPlace.map {
            MeetingInfoLayout.Box(it.x, it.y, it.width, it.height)
        }.toMutableList()
        val bounds = MeetingInfoLayout.Box(
            discussionTable.x + 100f, discussionTable.y + 100f,
            (discussionTable.width - 200f).coerceAtLeast(InfoBubbleUI.WIDTH),
            (discussionTable.height - 200f).coerceAtLeast(InfoBubbleUI.HEIGHT)
        )
        agendasToPlace.forEach { agendaUI ->
            agendaUI.agenda.informationKeys.forEach information@{ key ->
                val info = deployedInfos.find { it.info.name == key } ?: return@information
                val position = MeetingInfoLayout.place(
                    MeetingInfoLayout.Point(agendaUI.x + agendaUI.width / 2f, agendaUI.y + agendaUI.height / 2f),
                    bounds, obstacles, InfoBubbleUI.WIDTH, InfoBubbleUI.HEIGHT
                )
                val old = informationPositions[key]
                info.setPosition(old?.first ?: position.x, old?.second ?: position.y, Align.center)
                if (old != null) {
                    info.addAction(Actions.moveToAligned(position.x, position.y, Align.center, 0.25f))
                }
                informationPositions[key] = position.x to position.y
                obstacles.add(MeetingInfoLayout.Box(
                    position.x - InfoBubbleUI.WIDTH / 2f, position.y - InfoBubbleUI.HEIGHT / 2f,
                    InfoBubbleUI.WIDTH, InfoBubbleUI.HEIGHT
                ))
            }
        }

        if (agendasToPlace.size < 4) {
            //TODO: addActor(addAgendaButton), but we are using AvailableActionsUI for now.
            val (agendaX, agendaY) = positions[agendasToPlace.size]
            addAgendaButton.setPosition(agendaX, agendaY, Align.center)
        }

        if (!markerPlaced) {
            hideCurrentAgendaMarker()
        }

    }

    fun removeBubbles() {
        currentAgendas.forEach {
            it.remove()
        }
        deployedInfos.forEach {
            it.remove()
        }
        informationArms.clearChildren()

        currentAgendas.clear()
        deployedInfos.clear()
        hideCurrentAgendaMarker()
        //TODO: removeActor(addAgendaButton), but we are using AvailableActionsUI for now.
    }

    private fun updateImprovisedQuestDisplay(meeting: Meeting) {
        val quest = gameState.eventSystem.activeQuests.firstOrNull { it.isImprovised && it.meetingId == meeting.ID }
        if (quest == null) {
            improvisedQuestUI?.remove()
            improvisedQuestUI = null
            return
        }

        if (improvisedQuestUI == null) {
            improvisedQuestUI = scene2d.table {
                background = defaultSkin.newDrawable("white-pixel", Color(0.08f, 0.12f, 0.2f, 0.9f))
                pad(12f)
                defaults().left().pad(2f)
                add(scene2d.label(quest.name, "docTitle") {
                    setFontScale(0.28f)
                    color = Color.WHITE
                })
                row()
                add(scene2d.label(quest.description, "description") {
                    setWrap(true)
                    setFontScale(0.2f)
                    color = Color(0.9f, 0.92f, 0.95f, 1f)
                })
            }
            addActor(improvisedQuestUI)
        }

        val table = improvisedQuestUI!!
        table.clearChildren()
        table.background = defaultSkin.newDrawable("white-pixel", Color(0.08f, 0.12f, 0.2f, 0.9f))
        table.pad(12f)
        table.defaults().left().pad(2f)
        table.add(scene2d.label(quest.name, "docTitle") {
            setFontScale(0.28f)
            color = Color.WHITE
        })
        table.row()
        table.add(scene2d.label(quest.description, "description") {
            setWrap(true)
            setFontScale(0.2f)
            color = Color(0.9f, 0.92f, 0.95f, 1f)
        })
        table.setSize(700f, 150f)
        table.setPosition(
            discussionTable.x + discussionTable.width / 2 - table.width / 2,
            discussionTable.y + discussionTable.height - 180f,
            Align.center
        )
        table.clearActions()
        table.setScale(0.7f)
        table.addAction(Actions.sequence(Actions.scaleTo(1f, 1f, 0.25f)))
    }

    private fun ensureCurrentAgendaMarkerAnimation() {
        if (currentAgendaMarker.actions.size > 0) return
        currentAgendaMarker.setScale(1f)
        currentAgendaMarker.color.a = currentAgendaMarkerBaseAlpha
        currentAgendaMarker.addAction(
            Actions.forever(
                Actions.sequence(
                    Actions.parallel(
                        //Actions.scaleTo(1.12f, 1.12f, 0.55f),
                        Actions.alpha(0.45f, 0.55f)
                    ),
                    Actions.parallel(
                        //Actions.scaleTo(1f, 1f, 0.55f),
                        Actions.alpha(currentAgendaMarkerBaseAlpha, 0.55f)
                    )
                )
            )
        )
    }

    private fun hideCurrentAgendaMarker() {
        currentAgendaMarker.clearActions()
        currentAgendaMarker.setScale(1f)
        currentAgendaMarker.color.a = currentAgendaMarkerBaseAlpha
        currentAgendaMarker.isVisible = false
    }

    private fun showAgendaPersuasivenessFeedback(agenda: MeetingAgenda, delta: Double) {
        val center = agendaPositions[agenda]
            ?: currentAgendas.firstOrNull { it.agenda === agenda }?.let {
                (it.x + it.width / 2f) to (it.y + it.height / 2f)
            }
            ?: return
        val isSupport = delta > 0.0
        val effectPath = if (isSupport) {
            "meeting/effects/agenda-support.png"
        } else {
            "meeting/effects/agenda-attack.png"
        }
        val accentPath = if (isSupport) {
            "meeting/effects/kenney-spark-01.png"
        } else {
            "meeting/effects/kenney-slash-01.png"
        }
        val accentColor = if (isSupport) Color(0.42f, 1f, 0.68f, 0f) else Color(1f, 0.38f, 0.28f, 0f)
        val accent = Image(
            TextureRegionDrawable(CapsuleStage.instance.assetManager.get(accentPath, Texture::class.java))
        )
        accent.setSize(230f, 230f)
        accent.setPosition(center.first, center.second, Align.center)
        accent.setOrigin(Align.center)
        accent.setScale(0.45f)
        accent.color = accentColor
        accent.addAction(
            Actions.sequence(
                Actions.parallel(Actions.alpha(0.8f, 0.12f), Actions.scaleTo(1f, 1f, 0.2f)),
                Actions.parallel(Actions.fadeOut(0.62f), Actions.scaleTo(1.2f, 1.2f, 0.62f)),
                Actions.run { agendaFeedbackActors.remove(accent) },
                Actions.removeActor()
            )
        )
        addActor(accent)
        agendaFeedbackActors.add(accent)

        val effect = Image(
            TextureRegionDrawable(CapsuleStage.instance.assetManager.get(effectPath, Texture::class.java))
        )
        effect.setSize(180f, 180f)
        effect.setPosition(center.first, center.second, Align.center)
        effect.setOrigin(Align.center)
        effect.setScale(0.55f)
        effect.color = Color(1f, 1f, 1f, 0f)
        effect.addAction(
            Actions.sequence(
                Actions.parallel(Actions.fadeIn(0.1f), Actions.scaleTo(1f, 1f, 0.16f)),
                Actions.delay(0.08f),
                Actions.parallel(Actions.fadeOut(0.55f), Actions.scaleTo(1.35f, 1.35f, 0.55f)),
                Actions.run { agendaFeedbackActors.remove(effect) },
                Actions.removeActor()
            )
        )
        addActor(effect)
        agendaFeedbackActors.add(effect)

        val feedbackColor = if (isSupport) Color(0.35f, 1f, 0.55f, 1f) else Color(1f, 0.35f, 0.28f, 1f)
        val deltaLabel = Label(String.format(java.util.Locale.ROOT, "%+.1f", delta), defaultSkin, "docTitle")
        deltaLabel.setFontScale(0.25f)
        deltaLabel.color = feedbackColor
        deltaLabel.setPosition(center.first, center.second + 34f, Align.center)
        deltaLabel.addAction(
            Actions.sequence(
                Actions.parallel(Actions.moveBy(0f, 78f, 0.75f), Actions.fadeOut(0.75f)),
                Actions.run { agendaFeedbackActors.remove(deltaLabel) },
                Actions.removeActor()
            )
        )
        addActor(deltaLabel)
        agendaFeedbackActors.add(deltaLabel)
    }

    private fun bringAgendaPersuasivenessFeedbackToFront() {
        agendaFeedbackActors.toList().forEach { actor ->
            if (actor.parent === this) actor.toFront()
        }
    }

    private fun clearAgendaPersuasivenessFeedback() {
        agendaFeedbackActors.forEach(Actor::remove)
        agendaFeedbackActors.clear()
    }

    private fun registerMeetingCallbacks(meeting: Meeting) {
        if (!registeredMeetingCallbacks.add(meeting.ID)) return
        meetingMutualitySnapshots[meeting.ID] = captureMutualitySnapshot(meeting)
        meetingKnownInfoSnapshots[meeting.ID] = playerKnownInformationSnapshot()

        if (meeting.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION) {
            meeting.onVoteResults += {
                Gdx.app.postRunnable {
                    val voteResultsTable = VoteResultWindowUI(meeting)
                    CapsuleStage.instance.addActor(voteResultsTable)
                    voteResultsTable.setPosition(
                        CapsuleStage.instance.width / 2 - voteResultsTable.width / 2,
                        CapsuleStage.instance.height / 2 - voteResultsTable.height / 2
                    )
                }
            }
        }

        meeting.onMeetingEnded += {
            Gdx.app.postRunnable {
                val summaryUI = MeetingSummaryWindowUI.instance
                summaryUI.refresh(
                    gameState = gameState,
                    meeting = meeting,
                    previousMutuality = meetingMutualitySnapshots[meeting.ID].orEmpty(),
                    knownInformationBeforeMeeting = meetingKnownInfoSnapshots[meeting.ID].orEmpty()
                )
                AlertUI.instance.addAlert("meetingFinished", gameState.meetingDisplayName(meeting)) {
                    summaryUI.isVisible = true
                }
                registeredMeetingCallbacks.remove(meeting.ID)
                meetingMutualitySnapshots.remove(meeting.ID)
                meetingKnownInfoSnapshots.remove(meeting.ID)
            }
        }
    }

    private fun captureMutualitySnapshot(meeting: Meeting): Map<Pair<String, String>, Double> {
        val snapshot = hashMapOf<Pair<String, String>, Double>()
        meeting.currentCharacters.forEach { from ->
            meeting.currentCharacters.forEach { to ->
                if (from != to) {
                    snapshot[from to to] = gameState.getMutuality(from, to)
                }
            }
        }
        return snapshot
    }

    private fun playerKnownInformationSnapshot(): Set<String> {
        return gameState.informations.values
            .filter { gameState.playerName in it.knownTo }
            .map { it.name }
            .toSet()
    }

    private fun displaySpeechLine(line: SpeechInterpreter.SpeechLine) {
        if (speakerPortrait.tgtCharacter == line.speaker) {
            speakerPortrait.speechUI.displaySpeech(line.text, line.holdSeconds)
            return
        }
        portraits.firstOrNull { portrait -> portrait.tgtCharacter == line.speaker }?.also { portrait ->
            portrait.speechUI.displaySpeech(line.text, line.holdSeconds)
        }
    }

    companion object {
        lateinit var instance: MeetingUI
    }
}
