package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.S_PER_HR
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import java.util.*
import kotlin.math.exp
import kotlin.math.pow

/* Apparatus is a kind of building that can be used to produce and consume resources.
* It can also be used to absorb resources from the environment.
* It can also be used to distribute resources to the market.
* It belongs to a place.
* It has a durability. When durability is 0, it is broken. When durability is 100, it is brand new.
* It has a danger. When danger is 0, it is safe. When danger is 100, it creates accident every turn.
*
*
* */
@Serializable
class Apparatus {
    var laborValuePerHour = 1.0
    var plannedWorker = 0
    var currentWorker = 0
    var name = ""
    var ID = UUID.randomUUID().toString() //Since many apparatus have same name, they need an identifier.
    var durability = 0.0
        set(value) {
            field = when {
                value > const("DurabilityMax") -> const("DurabilityMax")
                value <= 0 -> 0.0//Damaged Apparatus Information is only stored in case of an accident.
                else -> value
            }
        }

    /**
     * Temperature of this apparatus. Updated every hour to be equal to the temperature of the place.
     * */
    var temperature = 300.0
    val statusText
        get() = status_durability + '\n' +
                status_temperature + '\n' +
                status_danger
    var status_durability = ""
    var status_temperature = ""
    var status_danger = ""

    val isStorage
        get() = jsonData.jsonObject["variables"]?.jsonObject?.get("storageType") != null
    val storageType: Pair<String, Double>
        get() {
            if (!isStorage) {
                Logger.write("$name is not a storage apparatus.")
                throw Exception("$name is not a storage apparatus.")
            }
            return jsonData.jsonObject["variables"]!!.jsonObject["storageType"]!!.jsonPrimitive.toString() to
                    jsonData.jsonObject["variables"]!!.jsonObject["storageAmount"]!!.jsonPrimitive.double
        }
    private val jsonData
        get() = ReadOnly.appJson[name] ?: throw Exception("$name not found in apparatus file.")
    private val baseDanger
        get() = jsonData.jsonObject["baseDanger"]!!.jsonPrimitive.double
    private val accidentTypes
        get() = jsonData.jsonObject["accidentType"]!!.jsonArray.map { AccidentType.valueOf(it.jsonPrimitive.content) }

    /**
     * Efficiency is multiplied by (T/300)^tempCoef.
     */
    private val tempCoef
        get() = jsonData.jsonObject["tempCoef"]?.jsonPrimitive?.double ?: .0

    /**
     * When T>Tmax, Efficiency is multiplied by exp[(1-T/Tmax)*30].  Damaged is scaled by 1+(T/Tmax). Danger is scaled by 1+(T/Tmax).
     */
    val maxTemp
        get() = jsonData.jsonObject["maxTemp"]?.jsonPrimitive?.double
            ?: 4000.0 //Default is 4000 K, which is the melting point of steel. Do not put infinite value here, as it will cause json parsing error.

    /**
     * When T<Tmin, Efficiency is multiplied by exp[(1-Tmin/T)*30].  Damaged is scaled by 1+(Tmin/T). Danger is scaled by 1+(Tmin/T).
     */
    val minTemp
        get() = jsonData.jsonObject["minTemp"]?.jsonPrimitive?.double ?: 4.0

    private val damageTempScale
        get() = if (temperature > maxTemp) 1 + temperature / maxTemp else if (temperature < minTemp) 1 + minTemp / temperature else 1.0

    val netEfficiency
        get() = (temperature / 300).pow(tempCoef) *
                (if (temperature > maxTemp) exp((1 - temperature / maxTemp) * 30) else if (temperature < minTemp) exp((1 - minTemp / temperature) * 30) else 1.0) *
                (if (durability <= 0) 0 else 1) *
                (if (idealWorker == 0) 1 else if (currentWorker <= idealWorker) currentWorker / idealWorker else 1 + (currentWorker - idealWorker) / idealWorker / 2)//Labor efficiency drops to 50% if overcrowded.

    val requiredResourcePerRepair: ArrayList<Resources>
        get() {
            val res = arrayListOf<Resources>()
            jsonData.jsonObject["requiredResourcePerRepair"]!!.jsonArray.toList().forEach {
                res.add(
                    Json.decodeFromString(
                        Resources.serializer(), it.toString()
                    )
                )
            }


            return res
        }
    private val idealAbsorption: Resources
        get() = Resources(
            HashMap(
                Json.decodeFromString(
                    MapSerializer<String, Double>(String.serializer(), Double.serializer()),
                    (jsonData.jsonObject["idealAbsorption"]?.toString() ?: "{}")
                )
            )
        )
    private val idealProduction: Resources
        get() = Resources(
            HashMap(
                Json.decodeFromString(
                    MapSerializer<String, Double>(String.serializer(), Double.serializer()),
                    (jsonData.jsonObject["idealProduction"]?.toString() ?: "{}")
                )
            )
        )
    private val idealConsumption: Resources
        get() = Resources(
            HashMap(
                Json.decodeFromString(
                    MapSerializer<String, Double>(String.serializer(), Double.serializer()),
                    (jsonData.jsonObject["idealConsumption"]?.toString() ?: "{}")
                )
            )
        )
    private val idealDistribution: Resources
        get() = Resources(
            HashMap(
                Json.decodeFromString(
                    MapSerializer<String, Double>(String.serializer(), Double.serializer()),
                    (jsonData.jsonObject["idealDistribution"]?.toString() ?: "{}")
                )
            )
        ) //Converts resources into market resources.
    private val idealHeatProduction
        get() = jsonData.jsonObject["idealHeatProduction"]?.jsonPrimitive?.double ?: .0
    val idealWorker
        get() = jsonData.jsonObject["idealWorker"]?.jsonPrimitive?.int ?: 0
    val currentWages get() = currentWorker * laborValuePerHour * const("WorkerWaterConsumptionRate")

    val currentProduction: Resources
        get() = idealProduction * netEfficiency
    val currentConsumption: Resources
        get() = idealConsumption * netEfficiency + Resources("ration" to currentWages, "water" to currentWages)

    /**
     * Hourly operation budget of this apparatus, including the consumption of workers.
     * Only ration, water, and phosphorus are included in budget.
     * Other resources are assumed to be intermediary goods and are not included in budget.
     */
    val hourlyOperationBudget: Resources
        get() = hourlyOperationResource.filter { (string, _) ->
            string in listOf(
                "ration",
                "water",
                "phosphorus"
            )
        }
    val hourlyOperationResource: Resources
        get() = idealConsumption * 1 + Resources(
            "ration" to idealWorker * laborValuePerHour * const("WorkerWaterConsumptionRate"),
            "water" to idealWorker * laborValuePerHour * const("WorkerWaterConsumptionRate")
        )
    val currentAbsorption: Resources
        get() = idealAbsorption * netEfficiency
    val currentDistribution: Resources
        get() = idealDistribution * netEfficiency + Resources("ration" to currentWages, "water" to currentWages)
    val currentHeatProduction: Double
        get() = idealHeatProduction * netEfficiency + currentWorker * const("WorkingHumanHeatProduction")

    var currentDangerRecord = 0.0
    var currentGraveDangerRecord = 0.0

    /**
     * Current accident danger of this apparatus. Unit: 1/second.
     */
    fun currentDanger(type: AccidentType, place: Place): Double {
        if (type !in accidentTypes) return 0.0
        var typeFactor = 1.0
        when (type) {
            AccidentType.FIRE -> typeFactor *= place.gasPressure("oxygen") / 101325 * 4
            AccidentType.COLLAPSE -> {}
            AccidentType.EXPLOSION -> {}
            AccidentType.FLOODING -> {}
            AccidentType.FRAGMENTS -> {}
        }
        return if (currentWorker == 0 || idealWorker == 0) 0.0 else if (durability == .0) 0.0 else {
            if (currentWorker <= idealWorker)
                baseDanger * (2 - currentWorker / idealWorker) * 100 / durability / const("GlobalAccidentTau") * damageTempScale * typeFactor
            else
            //Danger increases when overcrewed or undercrewed.
                baseDanger * (2 * currentWorker / idealWorker - 1) * 100 / durability / const("GlobalAccidentTau") * damageTempScale * typeFactor
        }
    }

    /**
     * Current catastrophic accident danger of this apparatus. Unit: 1/second.
     */
    fun currentGraveDanger(type: AccidentType, place: Place): Double {
        if (type !in accidentTypes) return 0.0
        var typeFactor = 1.0
        when (type) {
            AccidentType.FIRE -> typeFactor *= place.gasPressure("oxygen") / 101325 * 4
            AccidentType.COLLAPSE -> {}
            AccidentType.EXPLOSION -> {}
            AccidentType.FLOODING -> {}
            AccidentType.FRAGMENTS -> {}
        }
        return if (currentWorker == 0 || idealWorker == 0) 0.0
        else if (durability == .0) 0.0
        //Nonzero only when very overcrewed or undercrewed.
        else if (currentWorker <= idealWorker * 4 / 5)
            baseDanger * (0.2 - currentWorker / 4 / idealWorker) * 100 / durability / const("GlobalAccidentTau") * damageTempScale * typeFactor
        else if (currentWorker >= idealWorker * 6 / 5)
            baseDanger * (2 * currentWorker / 3 / idealWorker - 0.8) * 100 / durability / const("GlobalAccidentTau") * damageTempScale * typeFactor
        else
            0.0
    }

    /**
     * Current time constant of durability decrease.
     * */
    val currentDurabilityTau
        get() =
            (jsonData.jsonObject["durabilityTau"]?.jsonPrimitive?.double ?: const("DurabilityTau")) / damageTempScale

    fun workHourly(place: Place) {
        temperature = place.temperature //Update the temperature of the apparatus to the ambient temperature.
        laborValuePerHour = place.parent.laborValuePerHour

        depreciateHourly()
        //Check if it is workable------------------------------------------------------------------------------
        if (durability <= .0) {
            durability = .0
            Logger.write(
                "${name} in $name is broken and cannot function.",
                Logger.LogLevel.APPARATUS_VERBOSE
            )
            return //Cannot work broken apparatus
        }


        var err = false
        currentProduction.forEach {
            if (place.maxResources[it.key] != 0.0 && place.resources[it.key] + it.value > place.maxResources[it.key])//If maxResources is zero, there are no limit on how much resource you can store.
            {
                Logger.write(
                    "${name} in $name is cannot produce ${it.key} because it is full and cannot function.",
                    Logger.LogLevel.APPARATUS_VERBOSE
                )
                err = true //If the resource is full, no one works.
            }
        }
        if (err) {
            return //If there is an error, no one works.
        }
        place.resourceShortOfHourly(this)?.also {
            Logger.write(
                "${name} in $name is short of $it and cannot function.",
                Logger.LogLevel.APPARATUS_VERBOSE
            )
            return //If there is not enough resources, no one works.
        }
        place.gasResourceShortOfHourly(this)?.also {
            Logger.write(
                "${name} in $name is short of $it gas and cannot function.",
                Logger.LogLevel.APPARATUS_VERBOSE
            )
            return //If there is not enough resources, no one works.
        }
        //-----------------------------------------------------------------------------------------------------
        currentProduction.forEach {
            place.resources[it.key] += it.value * S_PER_HR
        }
        currentConsumption.forEach {
            place.resources[it.key] = (place.resources[it.key]) - it.value * S_PER_HR
        }
        currentDistribution.forEach {
            place.workers!!.first().resources[it.key] += it.value * S_PER_HR
            place.marketSupplyEstimateWeekly[it.key] += it.value * S_PER_HR //Market supply estimate is updated.

        }
        currentAbsorption.forEach {
            place.gasResources[it.key] -= it.value * S_PER_HR
        }
        place.addHeat(currentHeatProduction * S_PER_HR)

        AccidentType.entries.forEach { accidentType ->
            if (currentGraveDanger(accidentType, place) * S_PER_HR > GameEngine.random.nextDouble()) {
                //Catastrophic accident occurred.
                Logger.write("!Catastrophic accident occurred at: ${name}", Logger.LogLevel.INFO)
                place.isAccidentScene = true
                generateCatastrophicAccident(place)

            } else if (currentDanger(accidentType, place) * S_PER_HR > GameEngine.random.nextDouble()) {
                //Accident occurred.
                Logger.write("!Accident occurred at: ${name}", Logger.LogLevel.INFO)
                place.isAccidentScene = true
                generateAccident(place)

            }
        }
        currentDangerRecord = AccidentType.entries.sumOf { type -> currentDanger(type, place) }
        currentGraveDangerRecord = AccidentType.entries.sumOf { type -> currentGraveDanger(type, place) }


    }

    fun depreciateHourly() {
        //Consume durability, no matter it is currently being worked or not. For storages, keep the durability if they are fully staffed.
        if (!isStorage || currentWorker >= idealWorker)
            durability -= S_PER_HR / currentDurabilityTau * const("DurabilityMax")
        if (temperature > maxTemp)
            Logger.write("$name is overheated: $temperature K > $maxTemp K", Logger.LogLevel.APPARATUS_VERBOSE)
        if (temperature < minTemp)
            Logger.write("$name is freezing: $temperature K < $minTemp K", Logger.LogLevel.APPARATUS_VERBOSE)

    }

    fun generateAccident(place: Place) {
        //Generate casualties.
        val death = currentWorker / 100 + 1 //At least one worker dies.
        place.killWorkersInPlace(death)

        //Generate apparatus damage.
        durability -= 30
        getInformation(null, name, place.parent.time).also {
            place.parent.addInformation(it)
            //Add all people in the place to the known list.
            it.knownTo.addAll(place.characters)
            place.accidentInformationKeys += it.name
        }

    }


    fun generateCatastrophicAccident(place: Place) {
        //Generate casualties.
        val death = currentWorker / 5 + 1 //At least one worker dies.
        place.killWorkersInPlace(death)
        //Generate apparatus damage.
        durability -= 75
        getInformation(null, name, place.parent.time).also {
            place.parent.addInformation(it)
            //Add all people in the place to the known list.
            it.knownTo.addAll(place.characters)
            place.accidentInformationKeys += it.name
        }

    }

    fun getInformation(sbjCharacter: String?, tgtPlace: String, time: Int) = Information(
        author = sbjCharacter,
        creationTime = time,
        type = InformationType.APPARATUS,
        tgtTime = time,
        tgtPlace = tgtPlace,
        tgtApparatusName = name,
        tgtApparatusID = ID,
        amount = durability.toInt(),
        variables = hashMapOf(
            "durability" to durability,
            "maxTemp" to maxTemp,
            "temperature" to temperature,
            "minTemp" to minTemp,
            "currentWorker" to currentWorker.toDouble(),
            "idealWorker" to idealWorker.toDouble(),
            "efficiency" to netEfficiency,
            "danger" to currentDangerRecord,
            "graveDanger" to currentGraveDangerRecord

        )
    )

    override fun toString(): String {
        return "Apparatus(name='$name', durability=$durability, baseDanger=$baseDanger, idealProduction=$idealProduction, idealWorker=$idealWorker, currentWorker=$currentWorker, currentProduction=$currentProduction, currentDanger=$currentDangerRecord, currentGraveDanger=$currentGraveDangerRecord)"
    }

    enum class AccidentType {
        FIRE, COLLAPSE, EXPLOSION, FLOODING, FRAGMENTS
    }
}