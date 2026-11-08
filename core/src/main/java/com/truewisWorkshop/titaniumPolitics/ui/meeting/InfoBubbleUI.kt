package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import ktx.scene2d.KTable
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.stack

class InfoBubbleUI(
    val info: Information,
    presenter: String?,
    private val sharedBeforePlayerJoined: Boolean = false,
    onClick: () -> Unit
) : Table(), KTable {
    init {
        val presenterName = presenter?.let(ReadOnly::charProp)
            ?: ReadOnly.prop("InfoBubbleUI-tooltip-unknown-presenter")
        val tooltip = buildList {
            add(ReadOnly.prop("InfoBubbleUI-tooltip-presenter").format(presenterName))
            if (sharedBeforePlayerJoined) {
                add(ReadOnly.prop("InfoBubbleUI-tooltip-unknown"))
            } else {
                add(ReadOnly.prop("InfoBubbleUI-tooltip-type").format(ReadOnly.prop("InfoBubbleUI-type-${info.type}")))
                if (info.tgtPlace.isNotEmpty())
                    add(ReadOnly.prop("InfoBubbleUI-tooltip-place").format(ReadOnly.placeProp(info.tgtPlace)))
                info.tgtCharacter?.let {
                    add(ReadOnly.prop("InfoBubbleUI-tooltip-character").format(ReadOnly.charProp(it)))
                }
                info.author?.let {
                    add(ReadOnly.prop("InfoBubbleUI-tooltip-source").format(ReadOnly.charProp(it)))
                }
                when (info.type) {
                    InformationType.ACTION -> info.action?.let {
                        add(ReadOnly.prop("InfoBubbleUI-tooltip-action").format(ReadOnly.prop(it::class.simpleName!!)))
                    }
                    InformationType.RESOURCES -> {
                        val resources = info.resources.toHashMap().entries.sortedBy { it.key }
                        if (resources.isEmpty()) add(ReadOnly.prop("InfoBubbleUI-tooltip-no-resources"))
                        else resources.forEach { (name, amount) ->
                            add(ReadOnly.prop("InfoBubbleUI-tooltip-resource").format(ReadOnly.itemProp(name), amount))
                        }
                    }
                    InformationType.CASUALTY, InformationType.HUMAN_RESOURCES ->
                        add(ReadOnly.prop("InfoBubbleUI-tooltip-amount").format(info.amount))
                    InformationType.APPARATUS -> info.tgtApparatusName?.let {
                        add(ReadOnly.prop("InfoBubbleUI-tooltip-apparatus").format(ReadOnly.appProp(it)))
                    }
                    else -> Unit
                }
                add(ReadOnly.prop("InfoBubbleUI-tooltip-open"))
            }
        }.joinToString("\n")
        addListener(SimpleTextTooltipUI(tooltip))
        addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) = onClick()
        })
        stack {
            it.size(WIDTH, HEIGHT).fill()
            image("BubbleShade") { color = Color(0.18f, 0.56f, 0.95f, 0.95f) }
            container {
                image(
                    if (this@InfoBubbleUI.sharedBeforePlayerJoined) MeetingInformationVisuals.UNKNOWN_ICON
                    else MeetingInformationVisuals.icon(this@InfoBubbleUI.info.type)
                )
                size(32f)
            }
        }
        setSize(WIDTH, HEIGHT)
    }

    companion object {
        const val WIDTH = 54f
        const val HEIGHT = 54f
    }
}
