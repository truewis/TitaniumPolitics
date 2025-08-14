package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.S_PER_HR
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import java.lang.Math.pow
import java.util.*
import kotlin.math.exp
import kotlin.math.pow

/* Apparatus is a kind of building that can be used to produce and consume resources.
* It can also be used to absorb resources from the environment.
* It can also be used to distribute resources to the market.
* It belongs to a place.
* It has a durability. When durability is 0, it is broken. When durability is 100, it is brand new.
* It has a danger. When danger is 0, it is safe. When danger is 100, it is very dangerous.
* Accidents may happen when danger is high.
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

    /**
     * Efficiency is multiplied by (T/300)^tempCoef.
     */
    private val tempCoef
        get() = jsonData.jsonObject["tempCoef"]?.jsonPrimitive?.double ?: .0

    /**
     * When T>Tmax, Efficiency is multiplied by exp[(1-T/Tmax)*10].  Damaged is scaled by 1+(T/Tmax). Danger is scaled by 1+(T/Tmax).
     */
    private val maxTemp
        get() = jsonData.jsonObject["maxTemp"]?.jsonPrimitive?.double ?: Double.POSITIVE_INFINITY

    /**
     * When T<Tmin, Efficiency is multiplied by exp[(1-Tmin/T)*10].  Damaged is scaled by 1+(Tmin/T). Danger is scaled by 1+(Tmin/T).
     */
    private val minTemp
        get() = jsonData.jsonObject["minTemp"]?.jsonPrimitive?.double ?: 4.0

    private val damageTempScale
        get() = if (temperature > maxTemp) 1 + temperature / maxTemp else if (temperature < minTemp) 1 + minTemp / temperature else 1.0

    val netEfficiency
        get() = (temperature / 300).pow(tempCoef) *
                (if (temperature > maxTemp) exp((1 - temperature / maxTemp) * 10) else if (temperature < minTemp) exp((1 - minTemp / temperature) * 10) else 1.0) *
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
    val wages get() = currentWorker * laborValuePerHour * const("WorkerWaterConsumptionRate")

    val currentProduction: Resources
        get() = idealProduction * netEfficiency
    val currentConsumption: Resources
        get() = idealConsumption * netEfficiency + Resources("ration" to wages)
    val currentAbsorption: Resources
        get() = idealAbsorption * netEfficiency
    val currentDistribution: Resources
        get() = idealDistribution * netEfficiency + Resources("ration" to wages)
    val currentHeatProduction: Double
        get() = idealHeatProduction * netEfficiency + currentWorker * const("WorkingHumanHeatProduction")
    val currentDanger: Double
        get() {
            return if (currentWorker == 0 || idealWorker == 0) 0.0 else if (durability == .0) 0.0 else {
                if (currentWorker <= idealWorker)
                    baseDanger * (2 - currentWorker / idealWorker) * 100 / durability / const("GlobalAccidentTau") * damageTempScale
                else
                    baseDanger * (2 * currentWorker / idealWorker - 1) * 100 / durability / const("GlobalAccidentTau") * damageTempScale//Danger increases when overcrewed or undercrewed.
            }
        }
    val currentGraveDanger: Double
        get() {
            return if (currentWorker == 0 || idealWorker == 0) 0.0
            else if (durability == .0) 0.0
            else if (currentWorker <= idealWorker * 4 / 5)
                baseDanger * (0.2 - currentWorker / 4 / idealWorker) * 100 / durability / const("GlobalAccidentTau") * damageTempScale
            else if (currentWorker >= idealWorker * 6 / 5)
                baseDanger * (2 * currentWorker / 3 / idealWorker - 0.8) * 100 / durability / const("GlobalAccidentTau") * damageTempScale //Nonzero only when very overcrewed or undercrewed.
            else
                0.0
        }

    /**
     * Current time constant of durability decrease.
     * */
    val currentDurabilityTau
        get() =
            (jsonData.jsonObject["durabilityTau"]?.jsonPrimitive?.double ?: const("DurabilityTau")) / damageTempScale

    fun depreciateHourly() {
        //Consume durability, no matter it is currently being worked or not. For storages, keep the durability if they are fully staffed.
        if (!isStorage || currentWorker >= idealWorker)
            durability -= S_PER_HR / currentDurabilityTau
        if (temperature > maxTemp)
            Logger.write("$name is overheated: $temperature K > $maxTemp K", Logger.LogLevel.APPARATUS_VERBOSE)
        if (temperature < minTemp)
            Logger.write("$name is freezing: $temperature K < $minTemp K", Logger.LogLevel.APPARATUS_VERBOSE)

    }

    override fun toString(): String {
        return "Apparatus(name='$name', durability=$durability, baseDanger=$baseDanger, idealProduction=$idealProduction, idealWorker=$idealWorker, currentWorker=$currentWorker, currentProduction=$currentProduction, currentDanger=$currentDanger, currentGraveDanger=$currentGraveDanger)"
    }

}