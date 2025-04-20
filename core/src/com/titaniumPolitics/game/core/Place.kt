package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.dt
import kotlinx.serialization.Serializable
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
    var gasResources = Resources("oxygen" to 20000.0, "carbonDioxide" to 100.0)
    fun gasPressure(gasName: String): Double =
        gasResources[gasName] / ((ReadOnly.gasJson[gasName]!!.jsonObject["density"]!!.jsonPrimitive.float)) * (temperature / 273.15) / volume * 101325

    fun pressureToMass(gasName: String, pressure: Double): Double =
        pressure * ((ReadOnly.gasJson[gasName]!!.jsonObject["density"]!!.jsonPrimitive.float)) / (temperature / 273.15) * volume / 101325

    var connectedPlaces = arrayListOf<String>()
    var plannedWorker = 0
    var coordinates = Coordinate3D(0, 0, 0)
    var temperature = 300 //Ambient temperature in Kelvin.
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
    var characters = hashSetOf<String>()
    var responsibleParty = "" //Determines which party is responsible for the place.
    var isAccidentScene =
        false //If true, the place is closed and no one can enter. Can be cleared by clearAccidentScene.
    var accidentInformationKeys =
        hashSetOf<String>()//Information about the last accident. Non empty only when isAccidentScene is true.

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        plannedWorker =
            apparatuses.sumOf { it.idealWorker }//TODO: this is a temporary solution to set up the planned worker. It should be set by division leaders.
    }

    //Check the gas pressure of the connected places and slowly equalize it. This function is called every time change.
    fun diffuseGas()
    {
        connectedPlaces.forEach {
            val place = parent.places[it]!!
            //For each gas type, use the coordinates and the density in gasJson to distribute the gas according to the boltzmann distribution.
            gasResources.forEach { (key, _) ->
                val mass =
                    (ReadOnly.gasJson[key]!!.jsonObject["density"]!!.jsonPrimitive.float) * 22.4f / ReadOnly.NA
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

                gasResources[key] += flowAmount / volume

                place.gasResources[key] -= flowAmount / place.volume
            }
        }


    }

    companion object
    {
        fun timeBetweenPlaces(place1: String, place2: String): Int
        {
            if (place1 == place2) return 0
            return ReadOnly.constInt("MoveDuration")
        }
    }

}