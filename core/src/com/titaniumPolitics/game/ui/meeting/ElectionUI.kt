package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.*

class ElectionUI(val gameState: GameState, val party: Party, val candidates: Set<String>) : Table(), KTable {
    val characterBases = candidates.map { CharacterBase(it) }
    val characterBubbles =
        candidates.map { CharBubble(it, party, characterBases.first { base -> base.name == it }) }

    init {
        //Do not use scrollPane, we don't want to hide candidates.
        table {
            it.fill()
            add("Candidate").expand().fill()
            add("Support").expand().fill()
        }
        characterBases.forEach {
            add(it).size(WIDTH, HEIGHT).fill()
        }
        characterBubbles.forEach {
            addActor(it)
        }

    }

    fun refresh() {
        characterBubbles.forEach { bubble ->
            val support = party.getVotes(candidates)
            bubble.refresh((support[bubble.name]!! * 1.0 / party.totalVotes))
        }

    }

    companion object {
        const val WIDTH = 300f
        const val HEIGHT = 50f
    }


    //This bubble is floating above the character base, showing the current support of the character.
    class CharBubble(val name: String, val party: Party, val base: CharacterBase) : Table(), KTable {
        val nameLabel = scene2d.label(ReadOnly.prop(name), "docTitle") {
            setFontScale(0.7f)
        }
        val supportLabel: Label = scene2d.label("", "docTitle") {
            setFontScale(0.7f)
        }

        init {
            table {
                it.grow()
                add(this@CharBubble.nameLabel).expand().fill()
                add(this@CharBubble.supportLabel).expand().fill()
            }
            addAction(
                Actions.forever(
                    Actions.moveTo(
                        base.x + base.width / 2 - WIDTH / 2,
                        base.y + base.height + 10f,
                        0.5f, //Duration of the movement
                    )
                )
            )
        }

        fun refresh(support: Double) {
            supportLabel.setText(
                "${String.format("%.3f", support)}%"
            )
        }
    }

    class CharacterBase(val name: String) : Table(), KTable {
        init {
        }
    }
}