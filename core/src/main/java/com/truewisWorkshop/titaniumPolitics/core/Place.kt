package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.DT
import com.titaniumPolitics.game.core.ReadOnly.S_PER_HR
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.collections.forEach
import kotlin.compareTo
import kotlin.math.exp
import kotlin.math.min
import kotlin.text.get

@Serializable
class Place : GameStateElement() {
    private var _name: String? = null
    override val name: String
        get() = _name ?: parent.places.filter { it.value == this }.keys.first().also { _name = it }

    val connectedHomes: List<String>
        get() {
            if (name.contains("home"))
                return listOf<String>()
            return connectedPlaces.filter { !it.contains("home") }
        }

    /**Determines which division authorized to enter the place.
     * If empty, all characters are authorized, unless authorizedCharacters is not empty.
     * */
    val authorizedDivisions = hashSetOf<String>()

    /**Determines which characters are authorized to enter the place.
     * If empty, all characters are authorized.
     * Checked after authorizedDivision.
     * */
    val authorizedCharacters = hashSetOf<String>()
    val whoseHome: String?
        get() {
            if (name.contains("home_"))
                return name.substringAfter("home_")
            return null
        }

    /**
     * If this place is a building in a place, this is the name of the place it is in.
     */
    var isBuildingIn: String? = null

    /**
     * Index of the building in the place it is in. Null if not a building.
     */
    val buildingIndex
        get() = isBuildingIn?.let {
            parent.places[it]!!.connectedPlaces.filter { conn -> parent.places[conn]!!.isBuildingIn != null }
                .indexOf(name)
        } //

    /**
     * Number of buildings in this place.
     */
    val numberOfBuildings
        get() = connectedPlaces.count { conn -> parent.places[conn]!!.isBuildingIn == this.name }
    val manager: String?
        get() = workplaceParty?.leader

    val resources = Resources(positive = true)
    val maxResources: Resources
        get() {
            val result = Resources(positive = true)
            apparatuses.forEach {
                if (it.durability > .0 && it.isStorage)
                    result[it.storageType.first] += it.storageType.second
            }
            return result
        }

    /**
     * Market supply estimate is a weekly estimate of how much resources are produced in the place.
     */
    val marketSupplyEstimateWeekly = Resources(positive = true)

    /**
     * This is a constant that determines how much the market supply estimate is reduced every hour to form a time-averaged supply estimate.
     * It is used to smooth out the market supply estimate over time.
     * */
    val marketSupplyEstimateHours =
        168
    val marketSupplyEstimateR = 1 - 1.0 / marketSupplyEstimateHours
    var gasResources =
        Resources("oxygen" to 3000.0, "carbonDioxide" to 15.0, "nitrogen" to 9000.0).apply { positive = true }

    /**
     * Gas Pressure in units of Pa
     */
    fun gasPressure(gasName: String): Double =
        try {
            gasResources[gasName] / ((ReadOnly.gasJson[gasName]!!.jsonObject["density"]!!.jsonPrimitive.float)) * (temperature / 273.15) / volume * 101325
        } catch (e: Exception) {
            throw Exception("Gas $gasName density not found in gasJson.")
        }

    fun pressureToMass(gasName: String, pressure: Double): Double =
        try {
            pressure * ((ReadOnly.gasJson[gasName]!!.jsonObject["density"]!!.jsonPrimitive.float)) / (temperature / 273.15) * volume / 101325
        } catch (e: Exception) {
            throw Exception("Gas $gasName density not found in gasJson.")
        }


    var connectedPlaces = arrayListOf<String>()

    /**
     * Check if the character is authorized to enter the place.
     */
    fun isAuthorized(sbjCharacter: String): Boolean =
        ((authorizedDivisions.isEmpty() || parent.characters[sbjCharacter]!!.division?.name in authorizedDivisions)
            && (authorizedCharacters.isEmpty() || sbjCharacter in authorizedCharacters))
            || parent.characters[sbjCharacter]!!.trait.contains("robot") //Robots can enter anywhere.

    fun movableConnectedPlaces(sbjCharacter: String): List<String> = connectedPlaces.filter { placeTo ->
        parent.places[placeTo]!!.isAuthorized(sbjCharacter) &&
            // If this place is a corridor, you can move to any place connected to the corridor.
            // If this place is a building, you can move to the place it is in.
            // If this place is connected to a building, you can move to the building.
            // If this place is not a corridor, you can only move to corridors or manways with durability > 0.
            (name.contains("corridor") || isBuildingIn == placeTo || parent.places[placeTo]?.isBuildingIn == name || apparatuses.any {
                it.name == "manway" && it.ID == "manway_$placeTo" && it.durability > 0.0
            })
    }

    val plannedWorker: Int
        get() =
            apparatuses.sumOf { it.plannedWorker }
    var coordinates = Coordinate3D(0.0, 0.0, 0.0)

    /**
     * Ambient temperature of the place in Kelvin.
     */
    var temperature = 300.0

    /**
     * Heat capacity of the place in J/K.
     * This is a constant that determines how much heat is needed to change the temperature of the place by 1 K.
     */
    var heatCapacity = 4.184e9
    fun addHeat(energy: Double) {
        temperature += energy / heatCapacity
        if (temperature < 4) temperature = 4.0 //TODO:temporary solution. Lowest temperature ~ 4K.
    }

    /**
     * Volume of the place in m^3.
     * This is a constant that determines how much gas can be stored in the place at a given pressure.
     */
    var volume = 1e4f
    val currentWorker: Int get() = apparatuses.sumOf { it.currentWorker }
    val currentAvailableLabor: Int
        get() = characters.filter {
            parent.characters[it]!!.type == Character.Type.ANON && it in workplaceParty!!.members
        }
            .sumOf { parent.characters[it]!!.reliant }
    val workers
        get() = workplaceParty?.members?.filter { parent.characters[it]!!.type == Character.Type.ANON }
            ?.map { parent.characters[it]!! }

    val currentTotalPop: Int
        get() {
            return characters.sumOf { if (it.contains("Anon")) parent.characters[it]!!.reliant else parent.characters[it]!!.reliant + 1 }
//            if (name.contains("home")) return 0 //Home populations are added to the places the home is in.
//
//            if (name == "squareSouth") return parent.idlePop + currentWorker//All idle people gather at the square.
//            if (responsibleParty == "") return 0
//            else if (parent.parties[responsibleParty]!!.home == name)
//                return parent.parties[responsibleParty]!!.size -
//                        parent.places.filter {
//                            it.value.responsibleParty == responsibleParty && it.key != name
//                        }.values.sumOf { it.currentWorker }//If this place is a guildhall, all workers stay here when they are not working. TODO: this is a simplification.
//            else return currentWorker


            //return characters.filter { it } + currentWorker + idler +
        }

    val workHoursLength get() = workHoursEnd - workHoursStart
    var workHoursStart = 0
    var workHoursEnd = 0
    val workHours: IntRange
        get() = workHoursStart..workHoursEnd
    var apparatuses = hashSetOf<Apparatus>()
    fun getApparatus(ID: String): Apparatus {
        return apparatuses.find { it.ID == ID }
            ?: throw Exception("Apparatus $ID not found in place $name")
    }

    var characters = hashSetOf<String>()
    val realCharacters
        get() = characters.filter { !it.contains("Anon") }.toHashSet() //Characters that are not anonymous.
    var responsibleDivision: String? = null //Determines which party is responsible for the place.
    val workplaceParty: Party?
        get() = parent.parties["workplace_$name"]
    var isAccidentScene =
        false //If true, the place is closed and no one can enter. Can be cleared by clearAccidentScene.
    var accidentInformationKeys =
        hashSetOf<String>()//Information about the last accident. Non empty only when isAccidentScene is true.

    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        apparatuses.forEach { it.plannedWorker = it.idealWorker }
    }

    /**Check the gas pressure of the connected places and slowly equalize it. This function is called every time step.*/
    fun diffuseGasAndTemp() {
        connectedPlaces.forEach {
            val place = parent.places[it]!!
            //For each gas type, use the coordinates and the density in gasJson to distribute the gas according to the boltzmann distribution.
            gasResources.forEach { (key, _) ->
                val mass =
                    (ReadOnly.gasJson[key]!!.jsonObject["density"]!!.jsonPrimitive.float) * 0.0224f / ReadOnly.NA

                val potentialDiff = coordinates.z - place.coordinates.z
                val ratio = exp(
                    -(ReadOnly.GA * mass * potentialDiff) / (ReadOnly.KB * temperature) //[J] = [kg*m^2/s^2]
                ) //Boltzmann distribution.
                val equilabriumPressure =
                    (gasPressure(key) * volume + place.gasPressure(key) * place.volume) / (volume + ratio * place.volume)

                val flowAmount = pressureToMass(
                    key,
                    (equilabriumPressure - gasPressure(key)) * DT / const("GasDiffusionTau")
                )

                gasResources[key] += flowAmount

                place.gasResources[key] -= flowAmount
            }
            val equilabriumTemp =
                (temperature * heatCapacity + place.temperature * place.heatCapacity) / (heatCapacity + place.heatCapacity)

            val flowAmount =
                (equilabriumTemp - temperature) * (heatCapacity + place.heatCapacity) * DT / const("TemperatureDiffusionTau")


            addHeat(flowAmount)
            place.addHeat(-flowAmount)
        }


    }

    //Workers are assigned to apparatuses. If there is not enough workers, some apparatuses are not worked.
    fun distributeWorkers() {
        if (isAccidentScene) {
            apparatuses.forEach { it.currentWorker = 0 }
            return
        } //If there is an accident, no one works until it is resolved.
        val workableApparatus = apparatuses.filter {
            resourceShortOfHourly(it) == null && gasResourceShortOfHourly(it) == null && it.durability > 0
        }
        apparatuses.forEach {
            it.currentWorker = 0
        }//Reset the currentWorkers. Note that this must come after the condition check, because wages are included in required resources.
        val idealWorker = workableApparatus.sumOf { apparatus -> apparatus.idealWorker }
        var sum = 0
        workableApparatus.forEachIndexed lambda@{ index, apparatus ->
            if (index == workableApparatus.size - 1)//If last apparatus in the place, we have to allocate the rest of the worker.
            {
                apparatus.currentWorker = currentAvailableLabor - sum
            } else {
                if (idealWorker != 0)
                    apparatus.currentWorker =
                        currentAvailableLabor * apparatus.idealWorker / idealWorker//Distribute workers according to ideal worker
                sum += apparatus.currentWorker
            }
        }
    }

    fun workApparatusHourly() {
        if (responsibleDivision == null) return //TODO: Is this true?
        if (isAccidentScene) return //If there is an accident, no one works until it is resolved.
        apparatuses.forEach {
            it.workHourly(this)
        }
        marketSupplyEstimateWeekly *= marketSupplyEstimateR
    }

    fun resourceShortOfHourly(app: Apparatus): String? {
        var ret: String? = null
        (app.currentConsumption + app.currentDistribution).forEach {
            if ((resources[it.key]) < it.value * S_PER_HR)
                ret = it.key //If the resource is less than an hour worth of consumption, return the resource name.
        }
        return ret

    }

    fun gasResourceShortOfHourly(app: Apparatus): String? {
        var ret: String? = null
        app.currentAbsorption.forEach {
            if ((gasResources[it.key]) < it.value * S_PER_HR)
                ret = it.key //If the resource is less than a unit time worth of consumption, return the resource name.
        }
        return ret

    }

    fun killWorkersInPlace(death: Int) {
        //Kill workers in the place.
        var sum = death
        workers?.forEach { worker ->
            if (sum <= 0) return@forEach
            val killed = min(sum, worker.reliant)
            worker.killReliant(killed)
            sum -= killed
        }
        if (sum > 0) {
            Logger.write("Warning: $sum workers were not killed in $name", Logger.LogLevel.WARNING)
        }
    }

    fun distanceTo(targetName: String): Int? {
        return if (connectedPlaces.contains(targetName)) (parent.places[targetName]!!.coordinates - coordinates).amplitude.toInt() + 1 else null
    }

    fun generateSound(intensity: Int) {
        //Generate sound information around a certain radius.
        parent.places.values.forEach { otherPlace ->
            distanceTo(otherPlace.name)?.let { distance ->
                if (distance <= intensity) {
                    if (otherPlace.characters.isNotEmpty()) {
                        otherPlace.parent.addInformation(
                            Information(
                                author = null,
                                creationTime = parent.time,
                                type = InformationType.SOUND,
                                tgtTime = parent.time,
                                tgtPlace = otherPlace.name,
                                amount = intensity - distance
                            ).also {
                                it.knownTo.addAll(otherPlace.characters)
                            }
                        )
                    }
                }
            }
        }
    }

    private val shortestPathCache = mutableMapOf<String, Pair<List<String>, Int>?>()
    fun clearShortestPathCache() {
        shortestPathCache.clear()
    }

    fun shortestPathAndTimeTo(targetName: String, sbjChar: String): Pair<List<String>, Int>? {
        shortestPathCache[targetName]?.let { return it }
        val distances = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        val previous = mutableMapOf<String, String?>()
        val visited = mutableSetOf<String>()

        val comparator = compareBy<Pair<String, Int>> { it.second }
        val queue = PriorityQueue(comparator)

        distances[this.name] = 0
        queue.add(this.name to 0)

        while (queue.isNotEmpty()) {
            val (currentName, currentDistance) = queue.poll()

            if (currentName in visited) continue
            visited.add(currentName)

            if (currentName == targetName) break

            val currentPlace = parent.places[currentName] ?: continue

            for (neighborName in currentPlace.movableConnectedPlaces(sbjChar)) {
                if (neighborName in visited) continue

                val weight = currentPlace.distanceTo(neighborName) ?: continue
                val newDistance = currentDistance + weight

                if (newDistance < distances.getValue(neighborName)) {
                    distances[neighborName] = newDistance
                    previous[neighborName] = currentName
                    queue.add(neighborName to newDistance)
                }
            }
        }

        val finalCost = distances.getValue(targetName)
        if (finalCost == Int.MAX_VALUE) return null

        val path = mutableListOf<String>()
        var current: String? = targetName
        while (current != null) {
            path.add(0, current)
            current = previous[current]
        }
        val result = path to finalCost * ReadOnly.constInt("MoveDuration")
        shortestPathCache[targetName] = result
        return result
    }

    companion object {
        val publicPlaces = setOf<String>("market", "squareNorth", "squareSouth")

        fun whoseHome(place: String): String? {
            if (place.contains("home_"))
                return place.substringAfter("home_")
            return null
        }
    }

}
