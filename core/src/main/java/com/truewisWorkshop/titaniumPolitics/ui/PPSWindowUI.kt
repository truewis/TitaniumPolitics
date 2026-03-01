package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.Apparatus
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.GraphWidget
import com.truewisWorkshop.titaniumPolitics.ui.GraphScreen
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.buttonGroup
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack

/**
 * PPS (Personnel Protection System) information window.
 *
 * Displays three tabs — Gas, Temperature, and Power — each showing
 * live life-support status for the player's current [Place].
 *
 * Based on the tab layout of [PoliticiansInfoUI].
 */
class PPSWindowUI(val gameState: GameState) : Table(defaultSkin), KTable {

    /** Severity level for a single life-support condition. */
    enum class ConditionStatus { GREEN, ORANGE, RED }

    var gasStatus = ConditionStatus.GREEN
        private set
    var temperatureStatus = ConditionStatus.GREEN
        private set
    var powerStatus = ConditionStatus.GREEN
        private set
    var radiationStatus = ConditionStatus.GREEN
        private set

    /** Invoked after every successful [refresh] with the latest statuses. */
    val onStatusChanged = arrayListOf<(ConditionStatus, ConditionStatus, ConditionStatus, ConditionStatus) -> Unit>()

    // ── Tab root tables (visibility toggled on tab switch) ──────────────────
    private val gasRootTable = Table()
    private val temperatureRootTable = Table()
    private val powerRootTable = Table()
    private val radiationRootTable = Table()

    // ── History buffers for the 24-hr graphs ───────────────────────────────
    private val temperatureHistory = LinkedHashMap<Int, Float>()
    private val energyHistory = LinkedHashMap<Int, Float>()
    private val radiationHistory = LinkedHashMap<Int, Float>()
    private val maxHistorySize get() = ReadOnly.IDTH * 24  // 24 in-game hours

    // ── Reusable graph widgets (created once, refreshed each update) ────────
    private val temperatureGraph = GraphWidget(mapOf(0 to 300f), GraphScreen.DataType.COUNT).also {
        it.setYAxisTitle(ReadOnly.prop("PPSWindowUI-CurrentTemp"))
    }
    private val powerGraph = GraphWidget(mapOf(0 to 0f), GraphScreen.DataType.COUNT).also {
        it.setYAxisTitle(ReadOnly.prop("PPSWindowUI-Consumption"))
    }
    private val radiationGraph = GraphWidget(mapOf(0 to 0f), GraphScreen.DataType.COUNT).also {
        it.setYAxisTitle(ReadOnly.prop("PPSWindowUI-RadiationLevel"))
    }

    init {
        refresh()
    }

    // ── Threshold constants ─────────────────────────────────────────────────
    companion object {
        private const val O2_WARNING_PA = 18000f   // orange below this

        private val HARMFUL_GAS_ORANGE = mapOf(
            "carbonDioxide" to 3000f,
            "hydrogen" to 500f,
            "methane" to 500f,
            "ammonia" to 300f
        )
        private val HARMFUL_GAS_RED = mapOf(
            "carbonDioxide" to 8000f,
            "hydrogen" to 2000f,
            "methane" to 2000f,
            "ammonia" to 1500f
        )

        private const val ENERGY_RED_FRACTION = 0.10f
        private const val ENERGY_ORANGE_FRACTION = 0.25f
        private const val TEMP_WARNING_MARGIN = 10f  // Degrees
    }

    // ── Status evaluation ───────────────────────────────────────────────────

    private fun evaluateGasStatus(place: Place): ConditionStatus {
        val critO2 = ReadOnly.const("CriticalOxygenPressure").toFloat()
        val o2 = place.gasPressure("oxygen").toFloat()
        if (o2 < critO2) return ConditionStatus.RED
        if (o2 < O2_WARNING_PA) return ConditionStatus.ORANGE

        for ((gas, redThreshold) in HARMFUL_GAS_RED) {
            val orangeThreshold = HARMFUL_GAS_ORANGE[gas] ?: continue
            val pressure = try {
                place.gasPressure(gas).toFloat()
            } catch (e: Exception) {
                Logger.write("PPS: could not get pressure for $gas: ${e.message}", Logger.LogLevel.WARNING)
                continue
            }
            if (pressure > redThreshold) return ConditionStatus.RED
            if (pressure > orangeThreshold) return ConditionStatus.ORANGE
        }
        return ConditionStatus.GREEN
    }

    private fun evaluateTemperatureStatus(place: Place): ConditionStatus {
        val temp = place.temperature.toFloat()
        var worst = ConditionStatus.GREEN
        for (app in place.apparatuses) {
            val minT = app.minTemp.toFloat()
            val maxT = app.maxTemp.toFloat()
            val range = (maxT - minT).coerceAtLeast(1f)
            when {
                temp !in minT..maxT -> return ConditionStatus.RED
                temp > maxT - TEMP_WARNING_MARGIN ||
                    temp < minT + TEMP_WARNING_MARGIN -> worst = ConditionStatus.ORANGE
            }
        }
        return worst
    }

    private fun evaluatePowerStatus(place: Place): ConditionStatus {
        val maxEnergy = place.maxResources["energy"].toFloat()
        if (maxEnergy <= 0f) return ConditionStatus.GREEN
        val fraction = place.resources["energy"].toFloat() / maxEnergy
        return when {
            fraction < ENERGY_RED_FRACTION -> ConditionStatus.RED
            fraction < ENERGY_ORANGE_FRACTION -> ConditionStatus.ORANGE
            else -> ConditionStatus.GREEN
        }
    }

    private fun evaluateRadiationStatus(place: Place): ConditionStatus {
        //TODO
        val maxEnergy = place.maxResources["energy"].toFloat()
        if (maxEnergy <= 0f) return ConditionStatus.GREEN
        val fraction = place.resources["energy"].toFloat() / maxEnergy
        return when {
            fraction < ENERGY_RED_FRACTION -> ConditionStatus.RED
            fraction < ENERGY_ORANGE_FRACTION -> ConditionStatus.ORANGE
            else -> ConditionStatus.GREEN
        }
    }

    // ── Row-level colour helpers ────────────────────────────────────────────

    private fun statusColor(status: ConditionStatus): Color = when (status) {
        ConditionStatus.GREEN -> Color.GREEN
        ConditionStatus.ORANGE -> Color.ORANGE
        ConditionStatus.RED -> Color.RED
    }

    private fun gasRowColor(gasName: String, place: Place): Color {
        if (gasName == "oxygen") {
            val critO2 = ReadOnly.const("CriticalOxygenPressure").toFloat()
            val o2 = place.gasPressure("oxygen").toFloat()
            return when {
                o2 < critO2 -> Color.RED
                o2 < O2_WARNING_PA -> Color.ORANGE
                else -> Color.GREEN
            }
        }
        val pressure = try {
            place.gasPressure(gasName).toFloat()
        } catch (e: Exception) {
            Logger.write("PPS: could not get pressure for $gasName: ${e.message}", Logger.LogLevel.WARNING)
            return Color.WHITE
        }
        val red = HARMFUL_GAS_RED[gasName]
        val orange = HARMFUL_GAS_ORANGE[gasName]
        return when {
            red != null && pressure > red -> Color.RED
            orange != null && pressure > orange -> Color.ORANGE
            else -> Color.WHITE
        }
    }

    private fun tempRowColor(app: Apparatus, currentTemp: Float): Color {
        val minT = app.minTemp.toFloat()
        val maxT = app.maxTemp.toFloat()
        val range = (maxT - minT).coerceAtLeast(1f)
        return when {
            currentTemp > maxT || currentTemp < minT -> Color.RED
            currentTemp > maxT - TEMP_WARNING_MARGIN ||
                currentTemp < minT + TEMP_WARNING_MARGIN -> Color.ORANGE

            else -> Color.WHITE
        }
    }

    // ── Tab content builders ────────────────────────────────────────────────

    private fun buildGasTab(place: Place) {
        gasRootTable.clear()
        val dataTable = Table()
        dataTable.add(
            Label(ReadOnly.prop("PPSWindowUI-GasName"), defaultSkin, "docTitle")
                .apply { setFontScale(0.45f) }).width(400f).left()
        dataTable.add(
            Label(ReadOnly.prop("PPSWindowUI-Pressure"), defaultSkin, "docTitle")
                .apply { setFontScale(0.45f) }).width(500f).left()
        dataTable.row()
        for (gasName in place.gasResources.keys) {
            val pressure = try {
                place.gasPressure(gasName)
            } catch (e: Exception) {
                Logger.write("PPS: could not get pressure for $gasName: ${e.message}", Logger.LogLevel.WARNING)
                continue
            }
            val c = gasRowColor(gasName, place)
            dataTable.add(Label(gasName, defaultSkin, "docTitle").apply {
                setFontScale(0.4f); color = c; setAlignment(Align.left)
            }).width(400f).left()
            dataTable.add(Label("%.1f".format(pressure), defaultSkin, "docTitle").apply {
                setFontScale(0.4f); color = c; setAlignment(Align.left)
            }).width(500f).left()
            dataTable.row()
        }
        gasRootTable.add(ScrollPane(dataTable)).grow()
    }

    private fun buildTemperatureTab(place: Place) {
        temperatureRootTable.clear()
        temperatureRootTable.add(temperatureGraph).grow().minHeight(220f)
        temperatureRootTable.row()
        val appTable = Table()
        appTable.add(
            Label(ReadOnly.prop("PPSWindowUI-ApparatusName"), defaultSkin, "docTitle")
                .apply { setFontScale(0.4f) }).width(400f).left()
        appTable.add(
            Label(ReadOnly.prop("PPSWindowUI-MinTemp"), defaultSkin, "docTitle")
                .apply { setFontScale(0.4f) }).width(300f).left()
        appTable.add(
            Label(ReadOnly.prop("PPSWindowUI-MaxTemp"), defaultSkin, "docTitle")
                .apply { setFontScale(0.4f) }).width(300f).left()
        appTable.add(
            Label(ReadOnly.prop("PPSWindowUI-CurrentTemp"), defaultSkin, "docTitle")
                .apply { setFontScale(0.4f) }).width(300f).left()
        appTable.row()
        val currentTemp = place.temperature.toFloat()
        for (app in place.apparatuses) {
            val c = tempRowColor(app, currentTemp)
            appTable.add(Label(app.name, defaultSkin, "docTitle").apply {
                setFontScale(0.35f); color = c; setAlignment(Align.left)
            }).width(400f).left()
            appTable.add(Label("%.1f K".format(app.minTemp), defaultSkin, "docTitle").apply {
                setFontScale(0.35f); color = c; setAlignment(Align.left)
            }).width(300f).left()
            appTable.add(Label("%.1f K".format(app.maxTemp), defaultSkin, "docTitle").apply {
                setFontScale(0.35f); color = c; setAlignment(Align.left)
            }).width(300f).left()
            appTable.add(Label("%.1f K".format(currentTemp), defaultSkin, "docTitle").apply {
                setFontScale(0.35f); color = c; setAlignment(Align.left)
            }).width(300f).left()
            appTable.row()
        }
        temperatureRootTable.add(ScrollPane(appTable)).growX().top()
    }

    private fun buildPowerTab(place: Place) {
        powerRootTable.clear()
        powerRootTable.add(powerGraph).grow().minHeight(220f)
        powerRootTable.row()
        val appTable = Table()
        appTable.add(
            Label(ReadOnly.prop("PPSWindowUI-ApparatusName"), defaultSkin, "docTitle")
                .apply { setFontScale(0.4f) }).width(400f).left()
        appTable.add(
            Label(ReadOnly.prop("PPSWindowUI-Consumption"), defaultSkin, "docTitle")
                .apply { setFontScale(0.4f) }).width(400f).left()
        appTable.row()
        val maxEnergy = place.maxResources["energy"].toFloat()
        for (app in place.apparatuses) {
            val rowColor: Color = if (maxEnergy > 0f) {
                val fraction = place.resources["energy"].toFloat() / maxEnergy
                when {
                    fraction < ENERGY_RED_FRACTION -> Color.RED
                    fraction < ENERGY_ORANGE_FRACTION -> Color.ORANGE
                    else -> Color.WHITE
                }
            } else Color.WHITE
            val consumption = app.hourlyOperationResource["energy"].toFloat() / 3.600 //Kilo-Watts
            appTable.add(Label(app.name, defaultSkin, "docTitle").apply {
                setFontScale(0.35f); color = rowColor; setAlignment(Align.left)
            }).width(400f).left()
            appTable.add(Label("%.2f / hr".format(consumption), defaultSkin, "docTitle").apply {
                setFontScale(0.35f); color = rowColor; setAlignment(Align.left)
            }).width(400f).left()
            appTable.row()
        }
        powerRootTable.add(ScrollPane(appTable)).growX().top()
    }

    private fun buildRadiationTab(place: Place) {
        radiationRootTable.clear()
        radiationRootTable.add(radiationGraph).grow().minHeight(220f)
        radiationRootTable.row()
        val appTable = Table()
        appTable.add(
            Label(ReadOnly.prop("PPSWindowUI-ApparatusName"), defaultSkin, "docTitle")
                .apply { setFontScale(0.4f) }).width(400f).left()
        appTable.add(
            Label(ReadOnly.prop("PPSWindowUI-Consumption"), defaultSkin, "docTitle")
                .apply { setFontScale(0.4f) }).width(400f).left()
        appTable.row()
        val maxEnergy = place.maxResources["energy"].toFloat()
        for (app in place.apparatuses) {
            val rowColor: Color = if (maxEnergy > 0f) {
                val fraction = place.resources["energy"].toFloat() / maxEnergy
                when {
                    fraction < ENERGY_RED_FRACTION -> Color.RED
                    fraction < ENERGY_ORANGE_FRACTION -> Color.ORANGE
                    else -> Color.WHITE
                }
            } else Color.WHITE
            val consumption = app.hourlyOperationResource["energy"].toFloat()
            appTable.add(Label(app.name, defaultSkin, "docTitle").apply {
                setFontScale(0.35f); color = rowColor; setAlignment(Align.left)
            }).width(400f).left()
            appTable.add(Label("%.2f / hr".format(consumption), defaultSkin, "docTitle").apply {
                setFontScale(0.35f); color = rowColor; setAlignment(Align.left)
            }).width(400f).left()
            appTable.row()
        }
        radiationRootTable.add(ScrollPane(appTable)).growX().top()
    }

    // ── History management ──────────────────────────────────────────────────

    private fun updateHistories(place: Place) {
        val time = gameState.time
        temperatureHistory[time] = place.temperature.toFloat()
        energyHistory[time] = place.resources["energy"].toFloat()
        while (temperatureHistory.size > maxHistorySize) temperatureHistory.remove(temperatureHistory.keys.first())
        while (energyHistory.size > maxHistorySize) energyHistory.remove(energyHistory.keys.first())
    }

    // ── Main refresh ────────────────────────────────────────────────────────

    fun refresh() {
        val place = try {
            gameState.player.place
        } catch (e: Exception) {
            Logger.write("PPS: player place not yet available: ${e.message}", Logger.LogLevel.INFO)
            return
        }

        updateHistories(place)

        gasStatus = evaluateGasStatus(place)
        temperatureStatus = evaluateTemperatureStatus(place)
        powerStatus = evaluatePowerStatus(place)
        radiationStatus = evaluateRadiationStatus(place)

        buildGasTab(place)
        buildTemperatureTab(place)
        buildPowerTab(place)
        buildRadiationTab(place)


        buildLayout()

        onStatusChanged.forEach { it(gasStatus, temperatureStatus, powerStatus, radiationStatus) }
    }

    // ── Layout builder (called on every refresh so tab colours stay current) ─

    private fun buildLayout() {
        clear()
        buttonGroup(1, 1).also { bg ->
            bg.inCell.size(600f, 100f)

            bg.add(scene2d.button {
                label(ReadOnly.prop("PPSWindowUI-GasTab"), "docTitle").apply {
                    setFontScale(0.5f)
                    color = this@PPSWindowUI.statusColor(this@PPSWindowUI.gasStatus)
                }
                addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        if (!isChecked) return
                        this@PPSWindowUI.gasRootTable.isVisible = true
                        this@PPSWindowUI.temperatureRootTable.isVisible = false
                        this@PPSWindowUI.powerRootTable.isVisible = false
                        this@PPSWindowUI.radiationRootTable.isVisible = false
                    }
                })
            }).size(300f, 100f).fill()

            bg.add(scene2d.button {
                label(ReadOnly.prop("PPSWindowUI-TemperatureTab"), "docTitle").apply {
                    setFontScale(0.5f)
                    color = this@PPSWindowUI.statusColor(this@PPSWindowUI.temperatureStatus)
                }
                addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        if (!isChecked) return
                        if (this@PPSWindowUI.temperatureHistory.isNotEmpty())
                            this@PPSWindowUI.temperatureGraph.refresh(
                                this@PPSWindowUI.temperatureHistory, GraphScreen.DataType.COUNT
                            )
                        this@PPSWindowUI.gasRootTable.isVisible = false
                        this@PPSWindowUI.temperatureRootTable.isVisible = true
                        this@PPSWindowUI.powerRootTable.isVisible = false
                        this@PPSWindowUI.radiationRootTable.isVisible = false
                    }
                })
            }).size(300f, 100f).fill()

            bg.add(scene2d.button {
                label(ReadOnly.prop("PPSWindowUI-PowerTab"), "docTitle").apply {
                    setFontScale(0.5f)
                    color = this@PPSWindowUI.statusColor(this@PPSWindowUI.powerStatus)
                }
                addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        if (!isChecked) return
                        if (this@PPSWindowUI.energyHistory.isNotEmpty())
                            this@PPSWindowUI.powerGraph.refresh(
                                this@PPSWindowUI.energyHistory,
                                GraphScreen.DataType.COUNT
                            )
                        this@PPSWindowUI.gasRootTable.isVisible = false
                        this@PPSWindowUI.temperatureRootTable.isVisible = false
                        this@PPSWindowUI.powerRootTable.isVisible = true
                        this@PPSWindowUI.radiationRootTable.isVisible = false
                    }
                })
            }).size(300f, 100f).fill()

            bg.add(scene2d.button {
                label(ReadOnly.prop("PPSWindowUI-RadiationTab"), "docTitle").apply {
                    setFontScale(0.5f)
                    color = this@PPSWindowUI.statusColor(this@PPSWindowUI.powerStatus)
                }
                addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        if (!isChecked) return
                        if (this@PPSWindowUI.radiationHistory.isNotEmpty())
                            this@PPSWindowUI.radiationGraph.refresh(
                                this@PPSWindowUI.radiationHistory,
                                GraphScreen.DataType.COUNT
                            )
                        this@PPSWindowUI.gasRootTable.isVisible = false
                        this@PPSWindowUI.temperatureRootTable.isVisible = false
                        this@PPSWindowUI.powerRootTable.isVisible = false
                        this@PPSWindowUI.radiationRootTable.isVisible = true
                    }
                })
            }).size(300f, 100f).fill()
        }

        row()
        stack {
            it.grow()
            add(this@PPSWindowUI.gasRootTable)
            add(this@PPSWindowUI.temperatureRootTable)
            add(this@PPSWindowUI.powerRootTable)
        }

        // Gas tab visible by default
        gasRootTable.isVisible = true
        temperatureRootTable.isVisible = false
        powerRootTable.isVisible = false
    }
}
