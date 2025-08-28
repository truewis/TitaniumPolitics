package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingAdapter
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.EventSystem
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.SimplePortraitUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class DialogueUI(val gameState: GameState) : Table(defaultSkin), KTable {
    private var currentDialogue = ""
    private var currentLineNumber = 0

    // Displays current dialogue line.
    private val currentTextDisplay = TypingLabel("", skin, "description").apply {
        setFontScale(0.5f)
        touchable = Touchable.disabled
        wrap = true
        typingListener = object : TypingAdapter() {
            // Sense TypingLabel animation end and play next log in queue.
            override fun end() {
                super.end()

            }
        }
    }

    private val speakerNameDisplay = Label("", skin, "docTitle").apply {
        setFontScale(0.7f)
        touchable = Touchable.disabled
        setAlignment(Align.bottomLeft)
    }
    private val ctnuButton = Label(">>>", skin, "consoleWhite")
    val donePlayingLine = ArrayList<(Int) -> Unit>()
    private val background = Image(defaultSkin, "BackgroundNoiseHD")

    //Logs to be played.
    // Called and cleared when the ctnuButton is pressed.
    private var ctnuCallback: () -> Unit = {}
    private val portraitsTable = Table(defaultSkin).also { it.add().grow() }

    init {
        isVisible = false
        instance = this
        EventSystem.onPlayDialogue += {
            playDialogue(it)
        }
        stack {
            it.grow()
            add(this@DialogueUI.background)
            add(this@DialogueUI.portraitsTable)
            table {
                add().grow()
                row()
                stack {
                    it.growX()
                    val t = table {
                        add(this@DialogueUI.speakerNameDisplay).fill().growX()
                        row()
                        add(this@DialogueUI.currentTextDisplay).fill().growX()
                        row()
                        add(this@DialogueUI.ctnuButton).fill()
                    }
//                    image("GradientBottom") {
//                        setSize(t.prefWidth, t.prefHeight)
//                    }
//                    image("BackgroundNoiseHD") {
//                        setSize(t.prefWidth, t.prefHeight)
//                    }
                }
            }
        }
        ctnuButton.setPosition(1800f, 0f)
        ctnuButton.setFontScale(2f)
        // Blinking ctnuButton
        ctnuButton.addAction(
            Actions.forever(
                Actions.sequence(
                    Actions.delay(0.5f),
                    AlphaAction().apply {
                        duration = 0.2f
                        alpha = 0f
                    },
                    AlphaAction().apply {
                        duration = 0.2f
                        alpha = 1f
                    }
                )))
        ctnuButton.isVisible = true
        ctnuButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                if (!currentTextDisplay.hasEnded())
                    currentTextDisplay.skipToTheEnd()
                else
                    nextLine()
                super.clicked(event, x, y)
            }
        })
        addListener(object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    if (!currentTextDisplay.hasEnded())
                        currentTextDisplay.skipToTheEnd()
                    else
                        nextLine()
                    return true
                }
                return super.keyDown(event, keycode)
            }
        })
    }

    override fun setVisible(visible: Boolean) {
        if (visible) {
            stage.keyboardFocus = this
        }
        super.setVisible(visible)
    }

    private var dialogueLines: List<String> = emptyList()
    private val activePortraits = mutableMapOf<String, SimplePortraitUI>()

    fun nextLine() {
        if (currentLineNumber < dialogueLines.lastIndex) {
            currentLineNumber++
            val line = dialogueLines[currentLineNumber]

            // Handle special commands before playing dialogue line
            if (line.startsWith("ENTER ")) {
                val name = line.removePrefix("ENTER ").trim()
                addPortrait(name)
                nextLine() // Skip to next real dialogue or command
                return
            }
            if (line.startsWith("EXIT ")) {
                val name = line.removePrefix("EXIT ").trim()
                removePortrait(name)
                nextLine()
                return
            }
            if (line.startsWith("BELL")) {
                TODO()
            }
            if (line.startsWith("SLAM")) {
                TODO()
            }

            playLine(currentLineNumber)
        } else {
            ctnuCallback()
            ctnuCallback = {}
            instance.isVisible = false
        }
    }

    fun playDialogue(dialogueKey: String) {
        isVisible = true

        val placeName = gameState.player.place.name
        val imageKey = if (placeName.contains("home")) "home" else placeName
        val imagePath = ReadOnly.mapJson[imageKey]!!
            .jsonObject["image"]!!
            .jsonPrimitive.content

        background.drawable = TextureRegionDrawable(
            (stage as CapsuleStage).assetManager.get(imagePath, Texture::class.java)
        )

        currentDialogue = dialogueKey
        dialogueLines = Gdx.files.internal("texts/$currentDialogue.txt")
            .readString()
            .split("\n")
            .filter { it.isNotBlank() }
        playDialogueLines()
    }

    fun generatePositionIntroduction(char: com.titaniumPolitics.game.core.Character): String {
        return ReadOnly.script("NewTalk-Unknown-workplaceInfo").format(
            char.generatePositionText()
        )
    }

    fun playMeetingDialogue(meeting: com.titaniumPolitics.game.core.Meeting) {
        isVisible = true
        dialogueLines = emptyList()
        dialogueLines = listOf(
            meeting.currentSpeaker!! + ": " + ReadOnly.script("MeetingDialogue")
                .format(gameState.meetingName(meeting))
        )
        addPortrait(meeting.currentSpeaker!!)
        playDialogueLines()
    }

    fun playTalkDialogue(
        char1: com.titaniumPolitics.game.core.Character,
        char2: com.titaniumPolitics.game.core.Character,
        hasAgenda: Boolean = false
    ) {
        isVisible = true
        dialogueLines = emptyList()
        addPortrait(char1.name)
        addPortrait(char2.name)
        val known = if (gameState.player == char1) char2.name in gameState.knownCharactersToPlayer
        else char1.name in gameState.knownCharactersToPlayer
        if (known) {
            when (gameState.getMutNorm(char1.name, char2.name)) {
                in 0.25..1.0 -> {
                    dialogueLines +=
                        char1.name + ": " + ReadOnly.script("NewTalk-KnownPositive")
                            .format(ReadOnly.charProp(char2.name))
                }

                in -1.0..-0.25 -> {
                    dialogueLines +=
                        char1.name + ": " + ReadOnly.script("NewTalk-KnownNegative")
                            .format(ReadOnly.charProp(char2.name))
                }

                else -> {
                    dialogueLines +=
                        char1.name + ": " + ReadOnly.script("NewTalk-KnownNeutral")
                            .format(ReadOnly.charProp(char2.name))
                }
            }
            when (gameState.getMutNorm(char2.name, char1.name)) {
                in 0.25..1.0 -> {
                    dialogueLines +=
                        char2.name + ": " + ReadOnly.script("NewTalk-KnownPositiveResponse")
                            .format(ReadOnly.charProp(char1.name))
                }

                in -1.0..-0.25 -> {
                    dialogueLines +=
                        char2.name + ": " + ReadOnly.script("NewTalk-KnownNegativeResponse")
                            .format(ReadOnly.charProp(char1.name))
                }

                else -> {
                    dialogueLines +=
                        char2.name + ": " + ReadOnly.script("NewTalk-KnownNeutralResponse")
                            .format(ReadOnly.charProp(char1.name))
                }
            }
        } else {
            val char1PositionIntroduction = generatePositionIntroduction(char1)
            val char2PositionIntroduction = generatePositionIntroduction(char2)
            dialogueLines = listOf(
                char1.name + ": " + ReadOnly.script("NewTalk-Unknown")
                    .format(char1.name) + " " + char1PositionIntroduction + if (hasAgenda) " " + ReadOnly.script("NewTalk-Unknown-agenda") else "",
                char2.name + ": " + ReadOnly.script("NewTalk-Unknown-response") + " " + char2PositionIntroduction + if (hasAgenda) " " + ReadOnly.script(
                    "NewTalk-Unknown-agendaResponse"
                ) else ""
            )
        }
        playDialogueLines()
    }

    fun playDialogueLines() {
        currentLineNumber = -1 // So first call to nextLine() gets line 0
        activePortraits.clear()
        portraitsTable.clear()
        nextLine()
    }

    fun playLine(lineNumber: Int) {
        if (lineNumber !in dialogueLines.indices) {
            Logger.write(
                "Warning: Dialogue line number $lineNumber out of range in '$currentDialogue'.",
                Logger.LogLevel.INFO
            )
            return
        }

        val line = dialogueLines[lineNumber]
        val parts = line.split(": ", limit = 2)
        if (parts.size < 2) {
            Logger.write(
                "Warning: Malformed dialogue line in '$currentDialogue' at index $lineNumber: '$line'",
                Logger.LogLevel.INFO
            )
            return
        }
        val prefix = parts[0].split(", ", limit = 2)

        val emotion = if (prefix.size < 2) {
            "idle"
        } else prefix[1]
        val speaker = prefix[0]
        val text = parts[1]

        if (speaker == "Narrator")
            speakerNameDisplay.setText("")
        else
            speakerNameDisplay.setText(ReadOnly.prop(speaker))
        currentTextDisplay.restart(text)

        // Bring the current speaker to the foreground
        bringPortraitToFront(speaker, emotion)
    }

    /** Adds a portrait to the scene */
    private fun addPortrait(name: String) {
        if (activePortraits.containsKey(name)) return
        val portrait = SimplePortraitUI(name, 1f, false).apply {
            color.a = 0f // Start transparent, fade in
            addAction(Actions.fadeIn(0.3f))
        }
        activePortraits[name] = portrait
        portraitsTable.addActor(portrait)
        portrait.setPosition(0f, 300f, Align.bottomLeft) // Adjust position as needed
    }

    /** Removes a portrait from the scene */
    private fun removePortrait(name: String) {
        activePortraits[name]?.let { portrait ->
            portrait.addAction(
                Actions.sequence(
                    Actions.fadeOut(0.3f),
                    Actions.removeActor()
                )
            )
            activePortraits.remove(name)
        }
    }

    /** Moves the given portrait to the foreground and pushes others back */
    private fun bringPortraitToFront(speaker: String, emotion: String) {
        // Split portraits into two stacks (left/right)
        val portraitsList = activePortraits.values.toList()
        val half = (portraitsList.size + 1) / 2
        val leftStack = portraitsList.take(half).toMutableList()
        val rightStack = portraitsList.drop(half).toMutableList()
        // Layout parameters
        val centerX = portraitsTable.width / 2f
        val spacing = if (activePortraits.size == 1) 0f else 150f // tweak stack spacing
        val leftStart = centerX - (leftStack.size * spacing)
        val rightStart = centerX + spacing

        // Helper to move speaker to top of their stack
        fun bringToTop(stack: MutableList<SimplePortraitUI>, speaker: String) {
            val idx = stack.indexOfFirst { it.tgtCharacter == speaker }
            if (idx != -1) {
                val portrait = stack.removeAt(idx)
                stack.add(portrait) // put on top
            }
        }

        // Position + zIndex assignment
        fun positionStack(stack: List<SimplePortraitUI>, startX: Float, spacing: Float) {
            stack.forEachIndexed { i, portrait ->
                val targetX = startX + i * spacing
                portrait.addAction(
                    Actions.parallel(
                        Actions.moveTo(targetX, portrait.y, 0.4f),
                        Actions.alpha(if (portrait.tgtCharacter == speaker) 1f else 0.5f, 0.4f)
                    )
                )
                portrait.zIndex = i // lower = behind, last = top
            }
        }

        bringToTop(leftStack, speaker)
        bringToTop(rightStack, speaker)
        positionStack(leftStack, leftStart, spacing)
        positionStack(rightStack, rightStart, spacing)
        activePortraits[speaker]?.setEmotion(emotion)
    }


    companion object {
        lateinit var instance: DialogueUI
    }
}