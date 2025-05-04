package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.dt
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.math.exp

@Serializable
class Place : GameStateElement()
{
    override val name: String
        get() = parent.places.filter { it.value == this }.keys.first()
    var resources = Resources()
        get()
        {
            //If the place is a home, return the resources of the character living there.
            if (name.contains("home_"))
                return parent.characters[name.substringAfter("home_")]!!.resources
            return field
        }
        set(value)
        {
            //If the place is a home, set the resources of the character living there.
            if (name.contains("home_"))
                parent.characters[name.substringAfter("home_")]!!.resources = value
            field = value
        }
    val whoseHome: String?
        get()
        {
            if (name.contains("home_"))
                return name.substringAfter("home_")
            return null
        }
    var manager = ""
    var gasResources = Resources("oxygen" to 20000.0, "carbonDioxide" to 100.0)
    fun gasPressure(gasName: String): Double =
        gasResources[gasName] / ((ReadOnly.gasJson[gasName]!!.jsonObject["density"]!!.jsonPrimitive.float)) * (temperature / 273.15) / volume * 101325

    fun pressureToMass(gasName: String, pressure: Double): Double =
        pressure * ((ReadOnly.gasJson[gasName]!!.jsonObject["density"]!!.jsonPrimitive.float)) / (temperature / 273.15) * volume / 101325

    var connectedPlaces = arrayListOf<String>()
    val plannedWorker: Int
        get() =
            apparatuses.sumOf { it.plannedWorker }
    var coordinates = Coordinate3D(0, 0, 0)
    var temperature = 300.0 //Ambient temperature in Kelvin.
    var heatCapacity = 4.184e8//J/K
    fun addHeat(energy: Double)
    {
        temperature += energy / heatCapacity
        if (temperature < 4) temperature = 4.0 //TODO:temporary solution. Lowest temperature ~ 4K.
    }

    var volume = 1000f //Volume in m^3.
    val currentWorker: Int get() = apparatuses.sumOf { it.currentWorker }

    val currentTotalPop: Int
        //This number must be conserved.
        get()
        {
            if (name == "squareSouth") return parent.idlePop + currentWorker//All idle people gather at the square.
            if (responsibleParty == "") return 0
            else if (parent.parties[responsibleParty]!!.home == name)
                return parent.parties[responsibleParty]!!.size -
                        parent.places.filter {
                            it.value.responsibleParty == responsibleParty && it.key != name
                        }.values.sumOf { it.currentWorker }//If this place is a guildhall, all workers stay here when they are not working. TODO: this is a simplification.
            else return currentWorker


            //return characters.filter { it } + currentWorker + idler +
        }
    val maxResources: Resources
        get()
        {
            val result = Resources()
            apparatuses.forEach {
                if (it.durability > .0)
                    when (it.name)
                    {
                        "waterStorage" -> result["water"] += 30000.0
                        "oxygenStorage" -> result["oxygen"] += 3000.0
                        "metalStorage" -> result["metal"] += 30000.0
                        "componentStorage" -> result["component"] += 30000.0
                        "rationStorage" -> result["ration"] += 30000.0
                        "energyStorage" -> result["energy"] += 1.0e11
                        else ->//concatenate string
                        {
                            if (it.name.contains("Storage"))
                            {
                                val resource = it.name.substringBefore("Storage").lowercase(Locale.getDefault())
                                result[resource] += 30000.0
                            }
                        }
                    }
            }
            return result
        }

    var workHoursStart = 0
    var workHoursEnd = 0
    var apparatuses = hashSetOf<Apparatus>()
    fun getApparatus(ID: String): Apparatus
    {
        return apparatuses.find { it.ID == ID }!!
    }

    var characters = hashSetOf<String>()
    var responsibleParty = "" //Determines which party is responsible for the place.
    var isAccidentScene =
        false //If true, the place is closed and no one can enter. Can be cleared by clearAccidentScene.
    var accidentInformationKeys =
        hashSetOf<String>()//Information about the last accident. Non empty only when isAccidentScene is true.

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        apparatuses.forEach { it.plannedWorker = it.idealWorker }
    }

    //Check the gas pressure of the connected places and slowly equalize it. This function is called every time change.
    fun diffuseGasAndTemp()
    {
        connectedPlaces.forEach {
            val place = parent.places[it]!!
            //For each gas type, use the coordinates and the density in gasJson to distribute the gas according to the boltzmann distribution.
            gasResources.forEach { (key, _) ->
                try
                {
                    val mass =
                        (ReadOnly.gasJson[key]!!.jsonObject["density"]!!.jsonPrimitive.float) * 0.0224f / ReadOnly.NA

                    val potentialDiff = coordinates.z - place.coordinates.z
                    val ratio = exp(
                        -(ReadOnly.GA * mass * potentialDiff) / (ReadOnly.KB * temperature) //[J] = [kg*m^2/s^2]
                    ) //Boltzmann distribution. TODO: reflect the volume of the place.
                    val equilabriumPressure =
                        (gasPressure(key) * volume + place.gasPressure(key) * place.volume) / (volume + ratio * place.volume)

                    val flowAmount = pressureToMass(
                        key,
                        (equilabriumPressure - gasPressure(key)) * dt / const("GasDiffusionTau")
                    )

                    gasResources[key] += flowAmount

                    place.gasResources[key] -= flowAmount
                } catch (e: Exception)
                {
                    throw Exception("$key, ${e}")
                }
            }
            val equilabriumTemp =
                (temperature * heatCapacity + place.temperature * place.heatCapacity) / (heatCapacity + place.heatCapacity)

            val flowAmount =
                (equilabriumTemp - temperature) * (heatCapacity + place.heatCapacity) * dt / const("TemperatureDiffusionTau")


            temperature += flowAmount / heatCapacity

            place.temperature -= flowAmount / place.heatCapacity
        }


    }

    companion object
    {
        val publicPlaces = setOf<String>("market", "squareNorth", "squareSouth")
        fun timeBetweenPlaces(place1: String, place2: String): Int
        {
            if (place1 == place2) return 0
            return ReadOnly.constInt("MoveDuration")
        }

        fun whoseHome(place: String): String?
        {
            if (place.contains("home_"))
                return place.substringAfter("home_")
            return null
        }
    }

}