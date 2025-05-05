package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

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
class Apparatus
{
    var laborValuePerHour = 1.0
    var plannedWorker = 0
    var currentWorker = 0
    var name = ""
    var ID = UUID.randomUUID().toString() //Since many apparatus have same name, they need an identifier.
    var durability = 0.0
        set(value)
        {
            field = when
            {
                value > const("DurabilityMax") -> const("DurabilityMax")
                value < 0 -> 0.0
                else -> value
            }
        }
    private val jsonData
        get() = ReadOnly.appJson[name] ?: throw Exception("$name not found in apparatus file.")
    private val baseDanger
        get() = jsonData.jsonObject["baseDanger"]!!.jsonPrimitive.double
    val requiredResourcePerRepair: ArrayList<Resources>
        get()
        {
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
    private val idealAbsorption: HashMap<String, Double>
        get() = HashMap(
            Json.decodeFromString(
                MapSerializer<String, Double>(String.serializer(), Double.serializer()),
                (jsonData.jsonObject["idealAbsorption"]?.toString() ?: "{}")
            )
        )
    private val idealProduction: HashMap<String, Double>
        get() = HashMap(
            Json.decodeFromString(
                MapSerializer<String, Double>(String.serializer(), Double.serializer()),
                (jsonData.jsonObject["idealProduction"]?.toString() ?: "{}")
            )
        )
    private val idealConsumption: HashMap<String, Double>
        get() = HashMap(
            Json.decodeFromString(
                MapSerializer<String, Double>(String.serializer(), Double.serializer()),
                (jsonData.jsonObject["idealConsumption"]?.toString() ?: "{}")
            )
        )
    private val idealDistribution: HashMap<String, Double>
        get() = HashMap(
            Json.decodeFromString(
                MapSerializer<String, Double>(String.serializer(), Double.serializer()),
                (jsonData.jsonObject["idealDistribution"]?.toString() ?: "{}")
            )
        ) //Converts resources into market resources.
    private val idealHeatProduction
        get() = jsonData.jsonObject["idealHeatProduction"]?.jsonPrimitive?.double ?: .0
    val idealWorker
        get() = jsonData.jsonObject["idealWorker"]?.jsonPrimitive?.int ?: 0

    val storageType: String
        get()
        {
            if (name !in storages)
            {
                Logger.warning("$name is not a storage apparatus.")
                throw Exception("$name is not a storage apparatus.")
            }
            return name.substring(0, name.length - 7)
        }
    val currentProduction: Map<String, Double>
        get()
        {
            val result = idealProduction
            if (durability == .0) return result//No production if broken.

            if (idealWorker != 0)//Modify results based on the number of current worker.
            {
                idealProduction.forEach {
                    if (currentWorker <= idealWorker)
                        result[it.key] = it.value * currentWorker / idealWorker
                    else
                        result[it.key] =
                            it.value + it.value * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

                }
            }
            return result
        }
    val currentConsumption: Map<String, Double>
        get()
        {
            val result = idealConsumption
            if (durability == .0) return result//No production if broken.

            if (idealWorker != 0)//Modify results based on the number of current worker.
            {
                idealConsumption.forEach {
                    if (currentWorker <= idealWorker)
                        result[it.key] = it.value * currentWorker / idealWorker
                    else
                        result[it.key] =
                            it.value + it.value * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

                }
            }
            //Distribute wages
            result["ration"] =
                (result["ration"] ?: .0) + currentWorker * laborValuePerHour * const("WorkerWaterConsumptionRate")
            return result
        }
    val currentAbsorption: Map<String, Double>
        get()
        {
            val result = idealAbsorption
            if (durability == .0) return result//No production if broken.

            if (idealWorker != 0)//Modify results based on the number of current worker.
            {
                idealAbsorption.forEach {
                    if (currentWorker <= idealWorker)
                        result[it.key] = it.value * currentWorker / idealWorker
                    else
                        result[it.key] =
                            it.value + it.value * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

                }
            }
            return result
        }
    val currentDistribution: Map<String, Double>
        get()
        {
            val result = idealDistribution
            if (durability == .0) return result//No production if broken.

            if (idealWorker != 0)//Modify results based on the number of current worker.
            {
                idealDistribution.forEach {
                    if (currentWorker <= idealWorker)
                        result[it.key] = it.value * currentWorker / idealWorker
                    else
                        result[it.key] =
                            it.value + it.value * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

                }

                //Distribute wages
                result["ration"] =
                    (result["ration"] ?: .0) + currentWorker * laborValuePerHour * const("WorkerWaterConsumptionRate")
            }
            return result
        }
    val currentHeatProduction: Double
        get()
        {
            var result = .0
            if (idealWorker == 0) return result//No production if no worker.
            if (durability == .0) return result//No production if broken.
            if (currentWorker <= idealWorker)
                result = idealHeatProduction * currentWorker / idealWorker
            else
                result =
                    idealHeatProduction + idealHeatProduction * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

            //Each worker adds a bit of heat as they work.
            result += currentWorker * const("WorkingHumanHeatProduction")

            return result
        }
    val currentDanger: Double
        get()
        {
            return if (currentWorker == 0 || idealWorker == 0) 0.0 else if (durability == .0) 0.0 else
            {
                if (currentWorker <= idealWorker)
                    baseDanger * (2 - currentWorker / idealWorker) * 100 / durability * const("GlobalAccidentRate")
                else
                    baseDanger * (2 * currentWorker / idealWorker - 1) * 100 / durability * const("GlobalAccidentRate")//Danger increases when overcrewed or undercrewed.
            }
        }
    val currentGraveDanger: Double
        get()
        {
            return if (currentWorker == 0 || idealWorker == 0) 0.0
            else if (durability == .0) 0.0
            else if (currentWorker <= idealWorker * 4 / 5)
                baseDanger * (0.2 - currentWorker / 4 / idealWorker) * 100 / durability * const("GlobalAccidentRate")
            else if (currentWorker >= idealWorker * 6 / 5)
                baseDanger * (2 * currentWorker / 3 / idealWorker - 0.8) * 100 / durability * const("GlobalAccidentRate") //Nonzero only when very overcrewed or undercrewed.
            else
                0.0
        }

    override fun toString(): String
    {
        return "Apparatus(name='$name', durability=$durability, baseDanger=$baseDanger, idealProduction=$idealProduction, idealWorker=$idealWorker, currentWorker=$currentWorker, currentProduction=$currentProduction, currentDanger=$currentDanger, currentGraveDanger=$currentGraveDanger)"
    }

    companion object
    {
        val storages = listOf(
            "waterStorage",
            "oxygenStorage",
            "lightMetalStorage",
            "componentStorage",
            "rationStorage",
            "energyStorage"
        )
    }

}