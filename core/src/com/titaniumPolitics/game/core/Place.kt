package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.GameEngine.Companion.onAccident
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.dt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.math.exp
import kotlin.math.min

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
    val connectedHomes: List<String>
        get()
        {
            if (name.contains("home"))
                return listOf<String>()
            return connectedPlaces.filter { !it.contains("home") }
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
    val workForce: Int
        get() = characters.filter { it.contains("Anon") && it.contains(responsibleDivision) }
            .sumOf { parent.characters[it]!!.reliant }

    val currentTotalPop: Int
        //This number must be conserved.
        get()
        {
            return characters.sumOf { if (it.contains("Anon")) parent.characters[it]!!.reliant else 1 }
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
    val workHours: IntRange
        get() = workHoursStart..workHoursEnd
    var apparatuses = hashSetOf<Apparatus>()
    fun getApparatus(ID: String): Apparatus
    {
        return apparatuses.find { it.ID == ID }!!
    }

    var characters = hashSetOf<String>()
    var responsibleDivision = "" //Determines which party is responsible for the place.
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

    //Workers are assigned to apparatuses. If there is not enough workers, some apparatuses are not worked.
    fun distributeWorkers()
    {
        if (isAccidentScene)
        {
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
                apparatus.currentWorker = workForce - sum
            } else
            {
                if (idealWorker != 0)
                    apparatus.currentWorker =
                        workForce * apparatus.idealWorker / idealWorker//Distribute workers according to ideal worker
                sum += apparatus.currentWorker
            }
        }
    }

    fun workApparatusHourly()
    {
        val dth = 3600
        if (responsibleDivision == "") return //TODO: Is this true?
        if (isAccidentScene) return //If there is an accident, no one works until it is resolved.
        val worker = parent.characters.values.first {
            it.name.contains("Anon") && it.name.contains(
                name
            )
        }
        apparatuses.forEach app@{ apparatus ->
            //Consume durability, no matter it is currently being worked or not.
            apparatus.durability -= dth * const("DurabilityMax") / const("DurabilityTau")//Apparatuses are damaged over time. TODO: get rid of unexpected behaviors, if any
            //Check if it is workable------------------------------------------------------------------------------
            if (apparatus.durability <= .0)
            {
                apparatus.durability = .0
                return@app //Cannot work broken apparatus
            }


            apparatus.currentProduction.forEach {
                if ((resources[it.key]) + it.value > (maxResources[it.key]))
                    return@app //If the resource is full, no one works.
            }
            if (resourceShortOfHourly(apparatus) != null || gasResourceShortOfHourly(apparatus) != null)
                return@app //If there is not enough resources, no one works.
            //-----------------------------------------------------------------------------------------------------
            apparatus.currentProduction.forEach {
                resources[it.key] += it.value * dth
            }
            apparatus.currentConsumption.forEach {
                resources[it.key] = (resources[it.key]) - it.value * dth
            }
            apparatus.currentDistribution.forEach {
                worker.resources[it.key] += it.value * dth
            }
            apparatus.currentAbsorption.forEach {
                gasResources[it.key] -= it.value * dth
            }
            addHeat(apparatus.currentHeatProduction * dth)

            if (apparatus.currentGraveDanger > GameEngine.random.nextDouble())
            {
                //Catastrophic accident occurred.
                println("Catastrophic accident occurred at: ${name}")
                isAccidentScene = true
                generateCatastrophicAccidents()

            } else if (apparatus.currentDanger > GameEngine.random.nextDouble())
            {
                //Accident occurred.
                println("Accident occurred at: ${name}")
                isAccidentScene = true
                generateAccidents()

            }
            if (apparatus.name in Apparatus.storages
            )
            {
                apparatus.durability += dth * const("DurabilityMax") / const("DurabilityTau")//Storages are repaired if they are worked.
            }
        }
    }

    fun resourceShortOfHourly(app: Apparatus): String?
    {
        val dth = 3600
        app.currentConsumption.forEach {
            if ((resources[it.key]) < it.value * dth)
                return it.key //If the resource is less than an hour worth of consumption, return the resource name.
        }
        app.currentAbsorption.forEach {
            if ((gasResources[it.key]) < it.value * dth)
                return it.key //If the resource is less than a unit time worth of consumption, return the resource name.
        }
        return null

    }

    fun gasResourceShortOfHourly(app: Apparatus): String?
    {
        val dth = 3600
        app.currentAbsorption.forEach {
            if ((gasResources[it.key]) < it.value * dth)
                return it.key //If the resource is less than a unit time worth of consumption, return the resource name.
        }
        return null

    }

    fun generateAccidents()
    {
        //Generate casualties.
        val death = currentWorker / 100 + 1 //TODO: what about injuries?
        parent.parties[responsibleDivision]!!.causeDeaths(death)//TODO: we are assuming that all deaths are from the responsible party.
        Information(
            author = "",
            creationTime = parent.time,
            type = InformationType.CASUALTY,
            tgtPlace = name,
            auxParty = responsibleDivision,
            amount = death
        )/*store info*/.also {
            parent.informations[it.generateName()] = it
            //Add all people in the place to the known list.
            it.knownTo.addAll(characters)
            accidentInformationKeys += it.name
        }
        resources["corpse"] = (resources["corpse"]) + death

        //Generate resource loss.
        val loss = min(50.0, resources["water"])
        resources["water"] = (resources["water"]) - loss
        Information(
            author = "",
            creationTime = parent.time,
            type = InformationType.LOST_RESOURCES,
            tgtPlace = name,
            resources = Resources("water" to loss)
        )/*store info*/.also {
            parent.informations[it.generateName()] = it
            //Add all people in the place to the known list.
            it.knownTo.addAll(characters)
            accidentInformationKeys += it.name
        }

        //Generate apparatus damage.
        apparatuses.forEach { app ->
            val tmp = maxResources
            app.durability -= 30
            if (app.durability <= 0)
            {
                app.durability = .0
                //If storage durability = 0, lose resources.
                if (app.name in Apparatus.storages
                )
                {
                    //TODO: resources should be stored in storages, not in places.
                    val resourceName = app.storageType
                    resources[resourceName] = (resources[resourceName]
                            ) * (maxResources[resourceName]) / tmp[resourceName]
                    //For example, unbroken storage number 8->7 then lose 1/8 of the resource.
                    //TODO: generate information about the resource loss.
                }

                Information(
                    author = "",
                    creationTime = parent.time,
                    type = InformationType.DAMAGED_APPARATUS,
                    tgtPlace = name,
                    amount = death,
                    tgtApparatus = app.name
                )/*store info*/.also {
                    parent.informations[it.generateName()] = it
                    //Add all people in the place to the known list.
                    it.knownTo.addAll(characters)
                    accidentInformationKeys += it.name
                }
            }
            onAccident.forEach { it(name, death) }
        }//TODO: spread rumors. But think if it is a good game design.


    }

    fun generateCatastrophicAccidents()
    {
        //Generate casualties.
        val death = currentWorker / 5 + 1 //TODO: what about injuries?
        parent.parties[responsibleDivision]!!.causeDeaths(death)
        Information(
            author = "",
            creationTime = parent.time,
            type = InformationType.CASUALTY,
            tgtPlace = name,
            auxParty = responsibleDivision,
            amount = death
        )/*store info*/.also {
            parent.informations[it.generateName()] = it
            //Add all people in the place to the known list.
            it.knownTo.addAll(characters)
            accidentInformationKeys += it.name
        }
        resources["corpse"] = (resources["corpse"]) + death

        //Generate resource loss.
        val loss = min(50.0, resources["water"])
        resources["water"] = (resources["water"]) - loss
        Information(
            author = "",
            creationTime = parent.time,
            type = InformationType.LOST_RESOURCES,
            tgtPlace = name,
            resources = Resources("water" to loss)
        )/*store info*/.also {
            parent.informations[it.generateName()] = it
            //Add all people in the place to the known list.
            it.knownTo.addAll(characters)
            accidentInformationKeys += it.name
        }

        //Generate apparatus damage.
        apparatuses.forEach { app ->
            val tmp = maxResources
            app.durability -= 75
            if (app.durability <= 0)
            {
                app.durability = .0
                //If storage durability = 0, lose resources.
                if (app.name in Apparatus.storages
                )
                {
                    val resourceName = app.storageType
                    resources[resourceName] =
                        (resources[resourceName]
                                ) * maxResources[resourceName] / tmp[resourceName]
                    //For example, unbroken storage number 8->7 then lose 1/8 of the resource.
                    //TODO: generate information about the resource loss.
                }

                Information(
                    author = "",
                    creationTime = parent.time,
                    type = InformationType.DAMAGED_APPARATUS,
                    tgtPlace = name,
                    tgtApparatus = app.name
                )/*store info*/.also {
                    parent.informations[it.generateName()] = it
                    //Add all people in the place to the known list.
                    it.knownTo.addAll(characters)
                    accidentInformationKeys += it.name
                }
            }
        }
        onAccident.forEach { it(name, death) }
        //TODO: spread rumors. But think if it is a good game design.
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