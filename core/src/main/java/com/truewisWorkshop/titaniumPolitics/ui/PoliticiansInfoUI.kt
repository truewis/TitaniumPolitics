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
import com.titaniumPolitics.game.ui.actions.ApparatusSelectUI
import com.titaniumPolitics.game.ui.widget.BudgetDisplayUI
import com.titaniumPolitics.game.ui.widget.DivisionBannerUI
import com.titaniumPolitics.game.ui.widget.ResourceDisplayUI
import com.titaniumPolitics.game.ui.widget.TitleLabel
import com.truewisWorkshop.titaniumPolitics.ui.widget.DivisionSelectButtonGroup
import ktx.actors.alpha
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.buttonGroup
import ktx.scene2d.container
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.scrollPane
import ktx.scene2d.stack
import ktx.scene2d.table

//Human Resource Management is currently done without information. The report is instant.
class PoliticiansInfoUI(val gameState: GameState) : Table(defaultSkin), KTable {
    private val peopleDataTable = Table()
    private val peopleInformationPane = ScrollPane(peopleDataTable)
    private val divisionSelection = DivisionSelectButtonGroup { divisionName ->
        // Handle division selection change if needed
        selectedDivision = divisionName
        refresh()
    }
    private val peopleInformationRootTable = scene2d.table {
        add(this@PoliticiansInfoUI.divisionSelection).growX()
        row()
        add(this@PoliticiansInfoUI.peopleInformationPane)
    }
    private val divisionDataTable = scene2d.table()
    var selectedDivision: String = gameState.player.division?.name ?: ""

    //private val divisionInformationPane = ScrollPane(divisionDataTable)
    private val workplaceDataTables = hashMapOf<String, KTable>()
    //private val workplaceInformationPane = ScrollPane(workplaceDataTable)

    init {
        refresh()
    }

    fun refresh() {
        with(this) {
            clear()
            peopleInformationPane.setScrollingDisabled(false, false)
            buttonGroup(1, 1).also {
                it.inCell.size(600f, 100f)
                it.add(scene2d.button {
                    label(ReadOnly.prop("PoliticiansInfoUI-People"), "docTitle").apply { setFontScale(0.5f) }
                    addListener(
                        object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                            override fun changed(event: ChangeEvent?, actor: Actor?) {
                                if (!isChecked) return
                                this@PoliticiansInfoUI.peopleInformationRootTable.isVisible = true
                                this@PoliticiansInfoUI.divisionDataTable.isVisible = false
                                this@PoliticiansInfoUI.workplaceDataTables.values.forEach {
                                    (it as Table).isVisible = false
                                }
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
                                this@PoliticiansInfoUI.peopleInformationRootTable.isVisible = false
                                this@PoliticiansInfoUI.divisionDataTable.isVisible = true
                                this@PoliticiansInfoUI.workplaceDataTables.values.forEach {
                                    (it as Table).isVisible = false
                                }
                            }
                        }
                    )
                }).size(200f, 100f).fill()
                gameState.parties.values.filter { it.type == Party.Type.WORKPLACE && it.leader == gameState.playerName }
                    .forEach { party ->
                        it.add(scene2d.button {
                            label(ReadOnly.prop("PoliticiansInfoUI-Workplace"), "docTitle").apply { setFontScale(0.5f) }
                            row()
                            label(ReadOnly.placeProp(party.workplace.name), "docTitle").apply { setFontScale(0.2f) }
                            addListener(
                                object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                                        if (!isChecked) return
                                        this@PoliticiansInfoUI.peopleInformationRootTable.isVisible = false
                                        this@PoliticiansInfoUI.divisionDataTable.isVisible = false
                                        this@PoliticiansInfoUI.workplaceDataTables.values.forEach {
                                            (it as Table).isVisible = false
                                        }
                                        (this@PoliticiansInfoUI.workplaceDataTables[party.name]!! as Table).isVisible =
                                            true
                                    }
                                }
                            )
                        }).size(200f, 100f).fill()
                    }
            }
            row()
            stack {
                it.grow()
                add(this@PoliticiansInfoUI.peopleInformationRootTable)
                add(this@PoliticiansInfoUI.divisionDataTable)
                this@PoliticiansInfoUI.workplaceDataTables.forEach {
                    add(it.value as Table)
                }
            }
        }
        peopleDataTable.clear()

        // Header row
        peopleDataTable.add(Label("Name", defaultSkin, "docTitle").apply { setFontScale(0.5f) }).width(500f).left()
        peopleDataTable.add(Label("Position", defaultSkin, "docTitle").apply { setFontScale(0.5f) }).width(400f).left()
        peopleDataTable.add(Label("Qualification", defaultSkin, "docTitle").apply { setFontScale(0.5f) }).width(400f)
            .left()
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
            gameState.characters.filter {
                it.key != player && it.key in gameState.knownCharactersToPlayer && (selectedDivision == "" || it.value.division?.name == selectedDivision
                    /**/)
            }
                .toSortedMap()
        for (character in allCharacters) {
            // Name
            peopleDataTable.add(
                Label(
                    ReadOnly.charName(character.key),
                    defaultSkin,
                    "docTitle"
                ).apply { setFontScale(0.5f) }).width(500f).left()

            // Position (replace with your own logic)
            val position = character.value.generatePositionText()
            peopleDataTable.add(Label(position, defaultSkin, "docTitle").apply { setFontScale(0.3f) }).width(400f)
                .left()

            val qual = getCharacterQualification(character.value)
            peopleDataTable.add(Label(qual, defaultSkin, "docTitle").apply { setFontScale(0.3f) }).width(400f)
                .left()

            // Mutuality Meter
            val meter = MutualityMeter(gameState, character.key, player)
            peopleDataTable.add(meter).width(600f).pad(30f)
            peopleDataTable.row()
        }



        divisionDataTable.clear()
        with(divisionDataTable) {
            add(TitleLabel(ReadOnly.prop("PoliticiansInfoUI-DivisionInfo"))).colspan(2).pad(10f)
            row()
            val division =
                this@PoliticiansInfoUI.gameState.parties.filter { it.value.type == Party.Type.DIVISION && this@PoliticiansInfoUI.gameState.playerName in it.value.members }
                    .values.first()
            stack {
                it.grow()
                table {
                    //Division name, already shown in banner
//                    label(ReadOnly.prop(division.name), "docTitle").apply {
//                        setFontScale(0.7f)
//                        setAlignment(Align.left)
//                    }
//                    row()
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
                container(DivisionBannerUI(division, size = 380f)) {
                    center()
                    alpha = 0.1f
                }
            }
            scrollPane {
                it.grow()
                table {
                    label(ReadOnly.prop("PoliticiansInfoUI-ManagingPlace"), "docTitle") {
                        it.left()
                        setFontScale(0.3f)
                        setAlignment(Align.left)
                    }
                    label(ReadOnly.prop("PoliticiansInfoUI-Director"), "docTitle") {
                        it.left()
                        setFontScale(0.3f)
                        setAlignment(Align.left)
                    }
                    row()
                    division.divisionPlaces.forEach { pl ->
                        label(ReadOnly.placeProp(pl.name), "docTitle") {
                            it.left()
                            it.padRight(10f)
                            setFontScale(0.5f)
                            setAlignment(Align.left)
                        }

                        label(ReadOnly.charProp(pl.manager ?: "NotAssigned"), "docTitle") {
                            it.left()
                            setFontScale(0.5f)
                            setAlignment(Align.left)
                        }
                        row()
                    }
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
                add(BudgetDisplayUI(division)).grow()
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

        workplaceDataTables.forEach { (it.value as Table).clear() }
        gameState.parties.values.filter { it.type == Party.Type.WORKPLACE && it.leader == gameState.playerName }
            .forEach { workplaceParty ->
                if (!workplaceDataTables.containsKey(workplaceParty.name))
                    workplaceDataTables[workplaceParty.name] =
                        object : Table(defaultSkin), KTable {}.apply { isVisible = false }
                with(workplaceDataTables[workplaceParty.name]!!) {
                    this.add(TitleLabel(ReadOnly.prop("PoliticiansInfoUI-WorkplaceInfo"))).colspan(2).pad(10f)
                    (this as Table).row()
                    table {
                        it.grow()
                        table {
                            it.grow()
                            label(ReadOnly.placeProp(workplaceParty.home!!), "docTitle").apply {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                            }
                            row()
                            label(
                                ReadOnly.prop("PoliticiansInfoUI-Director") + ": " + ReadOnly.charProp(
                                    workplaceParty.leader ?: "NotAssigned"
                                ), "docTitle"
                            ).apply {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                            }
                            row()
                            label(
                                ReadOnly.prop("PoliticiansInfoUI-Size").format(workplaceParty.size), "docTitle"
                            ).apply {
                                setFontScale(0.3f)
                                setAlignment(Align.left)
                            }
                        }
                        table {
                            it.grow()
                            label(ReadOnly.prop("PoliticiansInfoUI-Role"), "docTitle") {
                                it.left()
                                setFontScale(0.3f)
                                setAlignment(Align.left)
                            }
                            label(ReadOnly.prop("PoliticiansInfoUI-Employee"), "docTitle") {
                                it.left()
                                setFontScale(0.3f)
                                setAlignment(Align.left)
                            }
                            row()
                            Party.Role.entries.forEach { role ->
                                label(ReadOnly.prop(role.toString()), "docTitle") {
                                    it.left()
                                    it.padRight(10f)
                                    setFontScale(0.5f)
                                    setAlignment(Align.left)
                                }

                                val char = workplaceParty.getCharByRole(role)

                                label(ReadOnly.charProp(char ?: "NotAssigned"), "docTitle") {
                                    it.left()
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
                            if (this@PoliticiansInfoUI.gameState.parties[workplaceParty.workplace.responsibleDivision]?.isBudgetResolved == false) {

                                row()
                                label(ReadOnly.prop("BudgetDisplayUI-notResolved"), "docTitle").apply {
                                    setFontScale(0.4f)
                                    setAlignment(Align.left)
                                }
                            } else {

                                row()
                                add(ResourceDisplayUI(workplaceParty.budget.sum())).grow()
                            }
                        }
                        table {
                            it.grow()
                            label(ReadOnly.prop("PoliticiansInfoUI-CurrentQuarterSalary"), "docTitle").apply {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                            }
                            label(ReadOnly.prop(workplaceParty.isSalaryPaid.toString()), "docTitle").apply {
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
                            label("%.1f%%".format(workplaceParty.integrity), "docTitle").apply {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                            }
                            row()
                            label(ReadOnly.prop("PoliticiansInfoUI-WorkHours"), "docTitle").apply {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                            }
                            label(
                                "%02d00 - %02d00".format(
                                    workplaceParty.workplace.workHoursStart,
                                    workplaceParty.workplace.workHoursEnd
                                ), "docTitle"
                            ).apply {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                            }
                            row()
                            label(ReadOnly.prop("PoliticiansInfoUI-CurrentEfficiency"), "docTitle").apply {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                            }
                            val averageCurrentEfficiency =
                                if (workplaceParty.workplace.apparatuses.isEmpty()) 0.0 else workplaceParty.workplace.apparatuses.sumOf {
                                    it.netEfficiency
                                } / workplaceParty.workplace.apparatuses.size
                            label("%.1f %%".format(averageCurrentEfficiency * 100.0), "docTitle").apply {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                            }
                        }
                    }
                    table {
                        it.top()
                        it.fill()
                        label(ReadOnly.prop("PoliticiansInfoUI-Apparatus"), "docTitle") {
                            setFontScale(0.5f)
                            setAlignment(Align.left)
                            it.fill().left()
                        }
                        row()
                        add(ApparatusSelectUI(this@PoliticiansInfoUI.gameState) {
                            ApparatusInfoUI.instance.display(it)
                        }.also {
                            it.refresh(workplaceParty.workplace.name)
                        }).fill()
                    }
                }
            }
    }

    private fun getCharacterQualification(character: Character): String {
        // Replace this with your own logic to determine the character's position
        return when {
            //character.trait.contains("ctrler") -> "The Controller"
            //character.trait.contains("observer") -> "The Observer"
            //character.trait.contains("mechanic") -> "The Mechanic"
            //character.trait.any { it.contains("DivisionLeader") } -> character.trait.first { it.contains("DivisionLeader") }
            //    .replace("DivisionLeader", "") + " Division Leader"

            character.trait.contains("engineer") -> "Engineer"
            character.trait.contains("soldier") -> "Soldier"
            else -> ""
        }
    }


}
