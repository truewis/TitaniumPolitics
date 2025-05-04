package com.titaniumPolitics.game.core

import kotlinx.serialization.encodeToString
import org.jetbrains.kotlinx.dataframe.api.add
import java.io.BufferedWriter
import java.io.File
import java.lang.Double.parseDouble
import java.lang.Float.parseFloat
import java.nio.file.Files
import java.nio.file.Paths

class GameDataHandler(val directoryName: String)
{
    init
    {
        Files.createDirectories(Paths.get(directoryName))
    }

    val resourceMap = HashMap<String, GameDataFrame>()

    /*
    GameState will be too bloated if we try to store all time series there.
    0. While the game is running, don't write anything. Write and load at the same time as GameState.
    1. Write the following in separate files in time series:
    distribution of each gas, column: place
    distribution of each resource, column: place
    production of each resource, column: place, including sum
    ....

    Temperature map, column: place
    health of each character, column: character
    ownership of each resource, column: character
    mutuality of each character, column: character pair




     */
    fun initializeColumns()
    {
        resourceMap["temperature"] = GameDataFrame("$directoryName/temperature.csv")
        resourceMap["health"] = GameDataFrame("$directoryName/health.csv")
        resourceMap["currentWorkerPerPlace"] = GameDataFrame("$directoryName/currentWorkerPerPlace.csv")
        resourceMap["plannedWorkerPerPlace"] = GameDataFrame("$directoryName/plannedWorkerPerPlace.csv")
        resourceMap["currentWorkerPerParty"] = GameDataFrame("$directoryName/currentWorkerPerParty.csv")
        resourceMap["plannedWorkerPerParty"] = GameDataFrame("$directoryName/plannedWorkerPerParty.csv")
        resourceMap["currentPop"] = GameDataFrame("$directoryName/currentPop.csv")

    }

    fun createIfNull(dfName: String)
    {
        if (!resourceMap.contains(dfName))
            resourceMap[dfName] = GameDataFrame("$directoryName/$dfName.csv")
    }

    fun writeEveryTurn(gState: GameState)
    {
        resourceMap["temperature"]!![gState.time] = hashMapOf(*(gState.places.map { (pName, place) ->
            pName to place.temperature.toFloat()
        }.filter { !it.first.contains("Anon") }.toTypedArray()))
        resourceMap["health"]!![gState.time] = hashMapOf(*(gState.characters.map { (cName, char) ->
            cName to char.health.toFloat()
        }.filter { !it.first.contains("Anon") }.toTypedArray()))

        resourceMap["currentWorkerPerPlace"]!![gState.time] = hashMapOf(*(gState.places.map { (pName, place) ->
            pName to place.currentWorker.toFloat()
        }.filter { !it.first.contains("home") }.toTypedArray()))
        resourceMap["plannedWorkerPerPlace"]!![gState.time] = hashMapOf(*(gState.places.map { (pName, place) ->
            pName to place.plannedWorker.toFloat()
        }.filter { !it.first.contains("home") }.toTypedArray()))

        resourceMap["currentWorkerPerParty"]!![gState.time] = hashMapOf(*(gState.parties.map { (pName, party) ->
            pName to party.currentWorker.toFloat()
        }.toTypedArray()))
        resourceMap["plannedWorkerPerParty"]!![gState.time] = hashMapOf(*(gState.parties.map { (pName, party) ->
            pName to party.plannedWorker.toFloat()
        }.toTypedArray()))


        resourceMap["currentPop"]!![gState.time] = hashMapOf(*(gState.places.map { (pName, place) ->
            pName to place.currentTotalPop.toFloat()
        }.filter { !it.first.contains("home") }.toTypedArray()))

        gState.existingResourceList.forEach {
            createIfNull("${it}Storage")
            resourceMap["${it}Storage"]!![gState.time] = hashMapOf(*(gState.places.map { (pName, place) ->
                pName to place.resources[it].toFloat()
            }.filter { !it.first.contains("Anon") }
                .toTypedArray()))//Do not save anonymous character's data: there are too many
        }
        gState.existingGasList.forEach {
            createIfNull("${it}GasStorage")
            resourceMap["${it}GasStorage"]!![gState.time] = hashMapOf(*(gState.places.map { (pName, place) ->
                pName to place.gasResources[it].toFloat()//TODO: store pressure instead?
            }.filter { !it.first.contains("Anon") }.toTypedArray()))
        }
    }

    fun close()
    {
        resourceMap.values.forEach { it.close() }
    }
}

data class GameDataFrame(var fName: String)
{
    private var file: File = File(fName)
    private var writer: BufferedWriter = file.bufferedWriter()

    var numRows = 0
    var allKeys = arrayListOf<String>()
//    operator fun get(time: Int): HashMap<String, Float>
//    {
//        return data[time] ?: hashMapOf()
//    }

    operator fun set(time: Int = numRows, datum: HashMap<String, Float>)
    {
        if (!allKeys.containsAll(datum.keys))
        {

            writer.write("keys")
            allKeys.addAll(datum.keys - allKeys)

            allKeys.forEach {
                writer.write("," + it)
            }
            writer.newLine()
        }


        writer.write(time.toString())
        allKeys.forEach {
            writer.write("," + (datum[it] ?: .0))
        }
        writer.newLine()

    }

    fun column(name: String): HashMap<Int, Float>
    {
        writer.close()

        val res = hashMapOf<Int, Float>()
        val s = file.readText()
        val rows = s.split('\n')
        var keyRow = -1
        var keyColumn = 0

        //Search for the key.
        do
        {
            keyRow++
            if (keyRow == rows.size)
                return res

            val keys = rows[keyRow].split(',')
            if (keys[0] != "keys") continue
            keyColumn = keys.takeLast(keys.size - 1).indexOf(name) + 1//keyColumn remains 0 if the key is not found
        } while (keyColumn == 0)
        for (i in keyRow..rows.size)
        {
            var values = rows[i].split(',')
            if (values[0] == "keys") continue
            res[values[0].toInt()] = values[keyColumn].toFloat()
        }




        writer = file.bufferedWriter()
        return res
    }

    fun columnSum(): HashMap<Int, Float>
    {
        writer.close()

        val res = hashMapOf<Int, Float>()
        val s = file.readText()
        val rows = s.split('\n')

        for (i in 1..rows.size)
        {
            var values = rows[i].split(',')
            if (values[0] == "keys") continue
            res[values[0].toInt()] = values.takeLast(values.size - 1).sumOf { it.toDouble() }.toFloat()
        }




        writer = file.bufferedWriter()
        return res
    }

    fun close()
    {
        writer.close()
    }

//    fun deserialize(s: String)
//    {
//        val rows = s.split('\n')
//        var keys = rows[0].split(',')
//        keys = keys.takeLast(keys.size - 1)
//        for (i in 1..rows.size)
//        {
//            var values = rows[i].split(',')
//            values = values.takeLast(values.size - 1)
//            val pairs = keys.mapIndexed { i, key -> Pair(key, parseFloat(values[i])) }.toTypedArray()
//            set(i, hashMapOf(*pairs))
//        }
//    }
}
