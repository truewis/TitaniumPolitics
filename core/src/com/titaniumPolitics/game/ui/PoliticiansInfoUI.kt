package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.ResourceDisplayUI
import com.titaniumPolitics.game.ui.widget.TitleLabel
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.buttonGroup
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

//Human Resource Management is currently done without information. The report is instant.
class PoliticiansInfoUI(val gameState: GameState) : Table(defaultSkin), KTable {
    private val peopleDataTable = Table()
    private val peopleInformationPane = ScrollPane(peopleDataTable)
    private val divisionDataTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("PoliticiansInfoUI-DivisionInfo"))).colspan(2).pad(10f)
        row()
        val division =
            this@PoliticiansInfoUI.gameState.parties.filter { it.value.type == "division" && this@PoliticiansInfoUI.gameState.playerName in it.value.members }
                .values.first()
        table {
            it.grow()
            label(ReadOnly.prop(division.name), "docTitle").apply {
                setFontScale(0.7f)
                setAlignment(Align.left)
            }
            row()
            //Division headquarter, useless info
//            label(ReadOnly.placeProp(division.home!!), "docTitle").apply {
//                setFontScale(0.5f)
//                setAlignment(Align.left)
//            }
//            row()
            label(
                ReadOnly.prop("PoliticiansInfoUI-DivisionLeader") + ": " + ReadOnly.charProp(
                    (division.leader ?: "NotAssigned")
                ), "docTitle"
            ).apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(
                ReadOnly.prop("PoliticiansInfoUI-Size").format(division.size), "docTitle"
            ).apply {
                setFontScale(0.3f)
                setAlignment(Align.left)
            }
        }
        table {
            it.grow()
            label(ReadOnly.prop("PoliticiansInfoUI-ManagingPlace"), "docTitle").apply {
                setFontScale(0.3f)
                setAlignment(Align.left)
            }
            label(ReadOnly.prop("PoliticiansInfoUI-Manager"), "docTitle").apply {
                setFontScale(0.3f)
                setAlignment(Align.left)
            }
            row()
            division.divisionPlaces.forEach { pl ->
                label(ReadOnly.placeProp(pl.name), "docTitle").apply {
                    setFontScale(0.5f)
                    setAlignment(Align.left)
                }

                label(ReadOnly.charProp(pl.manager ?: "NotAssigned"), "docTitle").apply {
                    setFontScale(0.5f)
                    setAlignment(Align.left)
                }
                row()
            }
        }
        row()
        table {
            it.grow()
            label(ReadOnly.prop("PoliticiansInfoUI-CurrentQuarterBudget"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            add(ResourceDisplayUI(division.budget.sum())).grow()
        }
        table {
            it.grow()
            label(ReadOnly.prop("PoliticiansInfoUI-CurrentQuarterSalary"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label(ReadOnly.prop(division.isSalaryPaid.toString()), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(ReadOnly.prop("PoliticiansInfoUI-SalaryPaidDate"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label("N/A", "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(ReadOnly.prop("PoliticiansInfoUI-BudgetProposed"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label(ReadOnly.prop(division.isBudgetProposed.toString()), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(ReadOnly.prop("PoliticiansInfoUI-BudgetProposedDate"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label("N/A", "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(ReadOnly.prop("PoliticiansInfoUI-BudgetResolved"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label(ReadOnly.prop(division.isBudgetResolved.toString()), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(ReadOnly.prop("PoliticiansInfoUI-BudgetResolvedDate"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label("N/A", "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(ReadOnly.prop("PoliticiansInfoUI-Integrity"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label("%.1f%%".format(division.integrity), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
        }
    }
    private val divisionInformationPane = ScrollPane(divisionDataTable)
    private val workplaceDataTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("PoliticiansInfoUI-WorkplaceInfo"))).colspan(2).pad(10f)
        row()
        val workplace =
            this@PoliticiansInfoUI.gameState.parties.filter { it.value.type == "workplace" && this@PoliticiansInfoUI.gameState.playerName == it.value.leader }
                .values.first()
        table {
            it.grow()
            label(ReadOnly.placeProp(workplace.home!!), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(
                ReadOnly.prop("PoliticiansInfoUI-Manager") + ": " + ReadOnly.charProp(
                    workplace.leader ?: "NotAssigned"
                ), "docTitle"
            ).apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(
                ReadOnly.prop("PoliticiansInfoUI-Size").format(workplace.size), "docTitle"
            ).apply {
                setFontScale(0.3f)
                setAlignment(Align.left)
            }
        }
        table {
            it.grow()
            label(ReadOnly.prop("PoliticiansInfoUI-Role"), "docTitle").apply {
                setFontScale(0.3f)
                setAlignment(Align.left)
            }
            label(ReadOnly.prop("PoliticiansInfoUI-Employee"), "docTitle").apply {
                setFontScale(0.3f)
                setAlignment(Align.left)
            }
            row()
            listOf("administrator", "treasurer", "overseer").forEach { role ->
                label(ReadOnly.prop(role), "docTitle").apply {
                    setFontScale(0.5f)
                    setAlignment(Align.left)
                }

                val char = when (role) {
                    "administrator" -> workplace.administrator
                    "treasurer" -> workplace.treasurer
                    "overseer" -> workplace.overseer
                    else -> null
                }

                label(ReadOnly.charProp(char ?: "NotAssigned"), "docTitle").apply {
                    setFontScale(0.5f)
                    setAlignment(Align.left)
                }
                row()
            }
        }
        row()
        table {
            it.grow()
            label(ReadOnly.prop("PoliticiansInfoUI-CurrentQuarterBudget"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            add(ResourceDisplayUI(workplace.budget.sum())).grow()
        }
        table {
            it.grow()
            label(ReadOnly.prop("PoliticiansInfoUI-CurrentQuarterSalary"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label(ReadOnly.prop(workplace.isSalaryPaid.toString()), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(ReadOnly.prop("PoliticiansInfoUI-SalaryPaidDate"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label("N/A", "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            row()
            label(ReadOnly.prop("PoliticiansInfoUI-Integrity"), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
            label("%.1f%%".format(workplace.integrity), "docTitle").apply {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }
        }
    }
    private val workplaceInformationPane = ScrollPane(workplaceDataTable)

    init {
        peopleInformationPane.setScrollingDisabled(false, false)
        buttonGroup(1, 1).also {
            it.inCell.size(600f, 100f)
            it.add(scene2d.button {
                label(ReadOnly.prop("PoliticiansInfoUI-People"), "docTitle").apply { setFontScale(0.5f) }
                addListener(
                    object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            if (!isChecked) return
                            this@PoliticiansInfoUI.peopleDataTable.isVisible = true
                            this@PoliticiansInfoUI.divisionInformationPane.isVisible = false
                            this@PoliticiansInfoUI.workplaceInformationPane.isVisible = false
                        }
                    }
                )
            }).size(200f, 100f).fill()
            it.add(scene2d.button {
                label(ReadOnly.prop("PoliticiansInfoUI-Division"), "docTitle").apply { setFontScale(0.5f) }
                addListener(
                    object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            if (!isChecked) return
                            this@PoliticiansInfoUI.peopleDataTable.isVisible = false
                            this@PoliticiansInfoUI.divisionInformationPane.isVisible = true
                            this@PoliticiansInfoUI.workplaceInformationPane.isVisible = false
                        }
                    }
                )
            }).size(200f, 100f).fill()
            it.add(scene2d.button {
                label(ReadOnly.prop("PoliticiansInfoUI-Workplace"), "docTitle").apply { setFontScale(0.5f) }
                addListener(
                    object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            if (!isChecked) return
                            this@PoliticiansInfoUI.peopleDataTable.isVisible = false
                            this@PoliticiansInfoUI.divisionInformationPane.isVisible = false
                            this@PoliticiansInfoUI.workplaceInformationPane.isVisible = true
                        }
                    }
                )
            }).size(200f, 100f).fill()
        }
        row()
        stack {
            it.grow()
            add(this@PoliticiansInfoUI.peopleInformationPane)
            add(this@PoliticiansInfoUI.divisionInformationPane)
            add(this@PoliticiansInfoUI.workplaceInformationPane)
        }


    }

    fun refresh() {
        peopleDataTable.clear()

        // Header row
        peopleDataTable.add(Label("Name", defaultSkin, "docTitle").apply { setFontScale(0.5f) }).width(400f).left()
        peopleDataTable.add(Label("Position", defaultSkin, "docTitle").apply { setFontScale(0.5f) }).width(400f).left()
        peopleDataTable.add(
            Label(
                "Mutuality",
                defaultSkin,
                "docTitle"
            ).apply { setFontScale(0.5f); setAlignment(Align.center) }).width(600f).center()
        peopleDataTable.row()

        // List all characters except the player
        val player = gameState.playerName
        val allCharacters =
            gameState.characters.filter { it.key != player && it.key in gameState.knownCharactersToPlayer }
        for (character in allCharacters) {
            // Name
            peopleDataTable.add(
                Label(
                    ReadOnly.charName(character.key),
                    defaultSkin,
                    "docTitle"
                ).apply { setFontScale(0.5f) }).width(400f).left()

            // Position (replace with your own logic)
            val position = getCharacterPosition(character.value) // Implement this method as needed
            peopleDataTable.add(Label(position, defaultSkin, "docTitle").apply { setFontScale(0.5f) }).width(400f)
                .left()

            // Mutuality Meter
            val meter = MutualityMeter(gameState, character.key, player)
            peopleDataTable.add(meter).width(600f).pad(30f)
            peopleDataTable.row()
        }
    }

    private fun getCharacterPosition(character: Character): String {
        // Replace this with your own logic to determine the character's position
        return when {
            character.trait.contains("ctrler") -> "The Controller"
            character.trait.contains("observer") -> "The Observer"
            character.trait.contains("mechanic") -> "The Mechanic"
            character.trait.any { it.contains("DivisionLeader") } -> character.trait.first { it.contains("DivisionLeader") }
                .replace("DivisionLeader", "") + " Division Leader"

            character.trait.contains("engineer") -> "Engineer"
            character.trait.contains("soldier") -> "Soldier"
            else -> ""
        }
    }


}