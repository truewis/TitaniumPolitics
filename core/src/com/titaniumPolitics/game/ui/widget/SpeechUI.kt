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
        typingListener = object : TypingListener {
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
        }

    }

    /**
     * Display speech from the engine thread.
     */
    fun displaySpeech(action: GameAction) {
        runBlocking {
            bubble.isVisible = true
            var text: String
            when (action) {
                is NewAgenda -> {
                    when (action.agenda.type) {
                        AgendaType.PROOF_OF_WORK -> text = ReadOnly.script("NewAgenda-ProofOfWork")
                        AgendaType.NOMINATE -> text =
                            ReadOnly.script("NewAgenda-Nominate").format(action.agenda.subjectParams["character"])

                        AgendaType.REQUEST -> text = ReadOnly.script("NewAgenda-Request").format(
                            ReadOnly.prop(
                                action.agenda.attachedRequest!!
                                    .action::class.simpleName!!
                            ), action.agenda.attachedRequest!!.issuedTo.first()
                        )

                        AgendaType.PRAISE -> text =
                            ReadOnly.script("NewAgenda-Praise").format(action.agenda.subjectParams["character"])

                        AgendaType.DENOUNCE -> text =
                            ReadOnly.script("NewAgenda-Denounce").format(action.agenda.subjectParams["character"])

                        AgendaType.PRAISE_PARTY -> text =
                            ReadOnly.script("NewAgenda-PraiseParty").format(action.agenda.subjectParams["party"])

                        AgendaType.DENOUNCE_PARTY -> text =
                            ReadOnly.script("NewAgenda-DenounceParty").format(action.agenda.subjectParams["party"])

                        AgendaType.BUDGET_PROPOSAL -> text = ReadOnly.script("NewAgenda-BudgetProposal")
                        AgendaType.BUDGET_RESOLUTION -> text = ReadOnly.script("NewAgenda-BudgetResolution")
                        AgendaType.APPOINT_MEETING -> text =
                            ReadOnly.script("NewAgenda-AppointMeeting")

                        AgendaType.FIRE_MANAGER -> text =
                            ReadOnly.script("NewAgenda-FireManager").format(action.agenda.subjectParams["character"])
                    }
                }

                is AddInfo -> {
                    text = ReadOnly.script(action.effectivityReason).format(action.agenda.subjectParams["character"])
                }

                is EndSpeech -> {
                    text = ReadOnly.script("EndSpeech2").format(ReadOnly.charProp(action.nextSpeaker))
                }

                else -> {
                    text = ReadOnly.script(action.javaClass.simpleName, action)
                }
            }
            Gdx.app.postRunnable {
                println("Displaying speech: $text")
                speech.restart(text)
            }
            suspendCoroutine { continuation ->
                onSpeechEnd +=
                    {
                        try {
                            continuation.resume(Unit)
                            println("Continuation resumed.")
                        } catch (e: IllegalStateException) {
                            // This can happen if the coroutine was already resumed.
                            println("Continuation was already resumed!")
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
            EmojiType.NONE -> ""
        }
        if (emojiType == EmojiType.NONE) return
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
        HELP, LIGHT, HEART, NONE, TALK
    }
}