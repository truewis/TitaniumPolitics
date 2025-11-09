package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.rafaskoberg.gdx.typinglabel.TypingListener
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.NewAgenda
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SpeechUI : Table(defaultSkin), KTable {

    private val onSpeechEnd = arrayListOf<() -> Unit>()
    private val speech = TypingLabel("", defaultSkin, "description").apply {
        setFontScale(0.4f)
        color = Color.WHITE
        wrap = true
        addTypingListener(object : TypingListener {
            override fun event(event: String?) {

            }

            override fun end() {
                if (this@apply.originalText.isEmpty()) return // If text is empty, do nothing. End event is triggered when label is initialized with empty text.
                addAction(
                    Actions.sequence(
                        Actions.delay(1f),
                        Actions.run {
                            clearSpeech()
                            onSpeechEnd.forEach { it() }
                        }
                    )
                )
            }

            override fun replaceVariable(variable: String?): String? {
                return null
            }

            override fun onChar(ch: Char?) {

            }
        })

    }

    /**
     * Display speech from the engine thread.
     */
    fun displaySpeech(text: String) {
        runBlocking {
            bubble.isVisible = true
            println("Displaying speech: $text")
            Gdx.app.postRunnable {
                speech.restart("$text ")// Add a space to ensure the last word is rendered.
            }
            suspendCoroutine { continuation ->
                onSpeechEnd.clear()
                onSpeechEnd +=
                    {
                        try {
                            continuation.resume(Unit)
                            println("Speech Continuation resumed.")
                        } catch (e: IllegalStateException) {
                            // This can happen if the coroutine was already resumed.
                            println("Speech Continuation was already resumed!")
                        }
                    }

            }
        }
    }

    val bubble = scene2d.stack {
        image("textbubble") {
            setColor(0f, 0f, 0f, 0.7f) // Semi-transparent bubble
        }
        container(this@SpeechUI.speech) {
            fill()
            pad(30f)
        }
    }
    val theEmoji = scene2d.image("HelpGrunge")

    fun displayEmoji(emojiType: EmojiType) {
        theEmoji.isVisible = emojiType != EmojiType.NONE
        val emojiTexture = when (emojiType) {
            EmojiType.HELP -> "HelpGrunge"
            EmojiType.LIGHT -> "LightGrunge"
            EmojiType.HEART -> "HeartGrunge"
            EmojiType.TALK -> "HelpGrunge"
            EmojiType.UNCONSCIOUS -> "icon_activity_105"
            EmojiType.NONE -> ""
        }
        if (emojiType == EmojiType.NONE) return
        theEmoji.color = when (emojiType) {
            EmojiType.HELP -> Color.WHITE
            EmojiType.LIGHT -> Color.YELLOW
            EmojiType.HEART -> Color.WHITE
            EmojiType.TALK -> Color.WHITE
            EmojiType.UNCONSCIOUS -> Color.RED
            else -> Color.WHITE
        }
        theEmoji.setDrawable(defaultSkin, emojiTexture)
        theEmoji.addListener(SimpleTextTooltipUI(ReadOnly.prop("EmojiTooltip-" + emojiType.name)))
    }

    fun displayActionEmoji(actionName: String) {
        //TODO: if the action is dangerous, make the emoji red.
//            if (dangerous) {
//                color = Color.RED
//            }
        theEmoji.isVisible = true
        theEmoji.clearListeners()
        try {
            theEmoji.setDrawable(
                defaultSkin,
                ReadOnly.actionJson[actionName]!!.jsonObject["image"]!!.jsonPrimitive.content
            )
            theEmoji.addListener(
                ActionTooltipUI(actionName)
            )
        } catch (e: Exception) {
            theEmoji.setDrawable(defaultSkin, "Help")
        }
    }

    fun clearSpeech() {
        bubble.isVisible = false
        //speech.setText("") Do not, as it will trigger the end event again immediately.
    }

    init {
        stack {
            it.size(450f, 150f).fill()
            container(this@SpeechUI.theEmoji) {
                size(100f)
                bottom()
                //Add blink action.
                addAction(
                    Actions.forever(
                        Actions.sequence(
                            Actions.alpha(0f, 0.5f),
                            Actions.alpha(1f, 0.5f)
                        )
                    )
                )
            }
            add(this@SpeechUI.bubble)
        }
    }

    enum class EmojiType {
        HELP, LIGHT, HEART, NONE, TALK, UNCONSCIOUS
    }
}
