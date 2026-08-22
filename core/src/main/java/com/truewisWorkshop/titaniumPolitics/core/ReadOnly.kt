package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.CharacterGenerator.Companion.generatedCharJson
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.toString

object ReadOnly {
    // --- Constants ---
    /** Boltzmann constant in J/K */
    val KB = 1.380649e-23

    /** Avogadro's number */
    val NA = 6.02214076e23

    /** Standard gravity in m/s² */
    val GA = 9.8

    // --- Localization Setup ---
    private val DEFAULT_LOCALE = Locale.ENGLISH

    // Current locale, defaulting to system default or DEFAULT_LOCALE
    var currentLocale: Locale = Locale.getDefault() ?: DEFAULT_LOCALE
        private set // Allow external read, but only internal modification via setLocale

    // Map to hold loaded Properties objects for the current locale (key is the base file name, e.g., "ui")
    private val propertyBundles = mutableMapOf<String, Properties>()

    // List of base names for all property files
    private val PROPERTY_BASE_NAMES = listOf(
        "ui", "apparatus", "place", "character", "resources", "DefaultCharacter",
        "DefaultCharacters",
        "maleNames", "femaleNames", "nameModifier", // ADDED for name generation
        "quests"
    )
    private val SPEECH_STYLE_FOLDERS = listOf(
        "texts/speechStyles",
        "texts/speechStyle",
        "texts/characterSpeech"
    )
    private val speechStyleBundles = mutableMapOf<String, Properties?>()

    // --- JSON Loading (No change, as JSON is typically non-localized) ---
    private fun loadJson(path: String): JsonObject {
        val content = Gdx.files?.internal(path)?.readString()
            ?: File("../assets/$path").readText()
        return Json.parseToJsonElement(content).jsonObject
    }

    @Deprecated("Use getMapData function instead for better encapsulation and potential future dynamic generation.")
    val mapJson = loadJson("json/map.json")

    /**
     * Get map data for a given place name. Handles homes, corridors, etc.
     */
    fun getMapData(placeName: String): JsonElement {
        val key =
            if (placeName.contains("home")) "home" else if (placeName.contains("corridor")) "corridor" else placeName
        return mapJson[key]!!

    }

    val charJson get() = generatedCharJson
    val actionJson = loadJson("json/action.json")
    val constJson = loadJson("json/consts.json")
    val gasJson = loadJson("json/gas.json")
    val appJson = loadJson("json/apparatus.json")
    val resJson = loadJson("json/resources.json")

    // --- Properties Loading & Locale Management ---

    /**
     * Loads property files for a given base name and locale, merging locale-specific
     * properties over the default properties.
     */
    private fun loadLocalizedProperties(baseName: String, locale: Locale): Properties {
        // Base path for default file: texts/ui.properties
        if (Gdx.files != null) {
            val defaultPath = Gdx.files.internal("texts/$baseName.properties")

            // Localized path: texts/ui_fr.properties (simple language matching)
            val localeTag = if (locale.language.isEmpty()) "" else "_${locale.language}"
            val localizedPath = Gdx.files.internal("texts/$baseName$localeTag.properties")

            val mergedProps = Properties()
            if (!defaultPath.exists()) {
                Logger.write(
                    "Default properties file not found for $baseName: ${defaultPath.path()}",
                    Logger.LogLevel.WARNING
                )
                return mergedProps
            }

            // 1. Load default properties (the base) with UTF-8 encoding
            defaultPath.reader("UTF-8").use { mergedProps.load(it) }

            // 2. Overwrite/merge with localized properties if they exist and are not the default path
            if (localizedPath.exists() && localizedPath.path() != defaultPath.path()) {
                val localizedProps = Properties().apply {
                    localizedPath.reader("UTF-8").use { load(it) }
                }
                localizedProps.forEach { (k, v) -> mergedProps.setProperty(k.toString(), v.toString()) }
                Logger.write("Loaded localized properties for $baseName: $localizedPath", Logger.LogLevel.INFO)
            }

            Logger.write("Loaded default properties for $baseName: $defaultPath", Logger.LogLevel.INFO)
            return mergedProps
        } else {
            // Fallback for non-Gdx environments (e.g., testing), using standard file I/O
            val defaultFile = File("../assets/texts/$baseName.properties")
            val localeTag = if (locale.language.isEmpty()) "" else "_${locale.language}"
            val localizedFile = File("../assets/texts/$baseName$localeTag.properties")

            val mergedProps = Properties()
            // 1. Load default properties (the base) with UTF-8 encoding
            if (defaultFile.exists()) {
                defaultFile.reader(StandardCharsets.UTF_8).use { mergedProps.load(it) }
                Logger.write("Loaded default properties for $baseName: ${defaultFile.path}", Logger.LogLevel.INFO)
            } else {
                Logger.write(
                    "Default properties file not found for $baseName: ${defaultFile.path}",
                    Logger.LogLevel.WARNING
                )
            }

            // 2. Overwrite/merge with localized properties if they exist and are not the default file
            if (localizedFile.exists() && localizedFile.path != defaultFile.path) {
                val localizedProps =
                    Properties().apply { localizedFile.reader(StandardCharsets.UTF_8).use { load(it) } }
                localizedProps.forEach { (k, v) -> mergedProps.setProperty(k.toString(), v.toString()) }
                Logger.write("Loaded localized properties for $baseName: ${localizedFile.path}", Logger.LogLevel.INFO)
            } else {
                Logger.write(
                    "Localized properties file not found for $baseName and locale $locale: ${localizedFile.path}",
                    Logger.LogLevel.INFO
                )
            }

            return mergedProps

        }

    }

    private fun loadSpeechStyleProperties(character: String, locale: Locale): Properties? {
        val localeTag = if (locale.language.isEmpty()) "" else "_${locale.language}"
        val mergedProps = Properties()
        if (Gdx.files != null) {
            for (folder in SPEECH_STYLE_FOLDERS) {
                val defaultPath = Gdx.files.internal("$folder/$character.properties")
                if (defaultPath.exists()) {
                    defaultPath.reader("UTF-8").use { mergedProps.load(it) }
                    val localizedPath = Gdx.files.internal("$folder/$character$localeTag.properties")
                    if (localizedPath.exists() && localizedPath.path() != defaultPath.path()) {
                        val localizedProps = Properties().apply {
                            localizedPath.reader("UTF-8").use { load(it) }
                        }
                        localizedProps.forEach { (k, v) -> mergedProps.setProperty(k.toString(), v.toString()) }
                    }
                    return mergedProps
                }
            }
        } else {
            for (folder in SPEECH_STYLE_FOLDERS) {
                val defaultFile = File("../assets/$folder/$character.properties")
                if (defaultFile.exists()) {
                    defaultFile.reader(StandardCharsets.UTF_8).use { mergedProps.load(it) }
                    val localizedFile = File("../assets/$folder/$character$localeTag.properties")
                    if (localizedFile.exists() && localizedFile.path != defaultFile.path) {
                        val localizedProps = Properties().apply {
                            localizedFile.reader(StandardCharsets.UTF_8).use { load(it) }
                        }
                        localizedProps.forEach { (k, v) -> mergedProps.setProperty(k.toString(), v.toString()) }
                    }
                    return mergedProps
                }
            }
        }
        return null
    }

    /**
     * Ensures all localized property bundles are loaded for the currentLocale.
     * Called lazily by accessor functions.
     */
    private fun ensureBundlesLoaded() {
        if (propertyBundles.isEmpty()) {
            Logger.write("Initializing property bundles for locale: $currentLocale", Logger.LogLevel.INFO)
            PROPERTY_BASE_NAMES.forEach { baseName ->
                propertyBundles[baseName] = loadLocalizedProperties(baseName, currentLocale)
            }
        }
    }

    /**
     * Public function to change the application's locale and reload all property bundles.
     */
    fun setLocale(newLocale: Locale) {
        if (currentLocale != newLocale) {
            currentLocale = newLocale
            propertyBundles.clear() // Force reload on next access
            speechStyleBundles.clear()
            ensureBundlesLoaded()
            Logger.write("Locale successfully set to: $newLocale", Logger.LogLevel.INFO)
        }
    }

    // --- Accessor Helpers for CharacterGenerator ---

    /**
     * Retrieves all properties (key/value pairs) from a specified bundle.
     * Used primarily by CharacterGenerator for retrieving name lists.
     */
    fun getAllPropsFromBundle(bundleName: String): Properties {
        ensureBundlesLoaded()
        return propertyBundles[bundleName] ?: Properties().also {
            Logger.write("ERROR: Could not find property bundle $bundleName", Logger.LogLevel.ERROR)
        }
    }

    /**
     * Allows CharacterGenerator to set a dynamically generated name into the runtime
     * character property bundle, maintaining the original access pattern (charProp).
     */
    fun setCharacterProp(key: String, value: String) {
        ensureBundlesLoaded()
        propertyBundles["character"]!!.setProperty(key, value)
    }

    // --- Accessor Functions (Refactored to use the dynamic map) ---

    /**
     * Generic function to retrieve a localized property string.
     * @param bundleName The base name of the property file (e.g., "ui", "apparatus").
     * @param key The property key.
     * @param obj An optional object to replace {VAR=...} placeholders.
     */
    private fun getLocalizedProp(bundleName: String, key: String, obj: Any? = null): String {
        ensureBundlesLoaded()
        val props = propertyBundles[bundleName]

        val rawValue = props?.getProperty(key)

        if (rawValue == null) {
            // Check the default bundle for a fallback if the key is missing in the specific bundle
            if (bundleName != "ui") {
                val fallbackValue = propertyBundles["ui"]?.getProperty(key)
                if (fallbackValue != null) {
                    Logger.write(
                        "Warning: Property $key not found in $bundleName, falling back to 'ui' bundle.",
                        Logger.LogLevel.INFO
                    )
                    return fallbackValue.let { if (obj != null) it.replacePlaceholders(obj) else it }
                }
            }

            Logger.write(
                "Warning: Could not find property $key in bundle $bundleName for locale $currentLocale",
                Logger.LogLevel.INFO
            )
            return "Unknown property [$key]"
        }

        return if (obj != null) {
            rawValue.replacePlaceholders(obj)
        } else {
            rawValue
        }
    }

    // Re-mapped public accessor functions to use the centralized lookup

    fun prop(key: String, obj: Any? = null): String = getLocalizedProp("ui", key, obj)
    fun appProp(key: String, obj: Any? = null): String = getLocalizedProp("apparatus", key, obj)
    fun placeProp(key: String, obj: Any? = null): String {
        // Handle special case logic from original code
        if (key.startsWith("home_")) {
            return if (key.contains("desc"))
                getLocalizedProp("place", "home-desc")
            else
                getLocalizedProp("place", "home").format(key.substringAfter("home_"))
        }
        if (key.startsWith("corridor_")) {
            return if (key.contains("desc"))
                getLocalizedProp("place", "corridor-desc")
            else
                getLocalizedProp("place", "corridor").format(key.substringAfter("corridor_"))
        }
        return getLocalizedProp("place", key, obj)
    }

    fun charProp(key: String, obj: Any? = null): String = getLocalizedProp("character", key, obj)
    fun itemProp(key: String, obj: Any? = null): String = getLocalizedProp("resources", key, obj)
    private fun scriptBundles(): List<Properties> {
        ensureBundlesLoaded()
        return listOfNotNull(
            propertyBundles["DefaultCharacters"],
            propertyBundles["DefaultCharacter"]
        )
    }

    private fun getStyleBundle(character: String?): Properties? {
        if (character.isNullOrBlank()) return null
        ensureBundlesLoaded()
        val cacheKey = "${currentLocale.language}:$character"
        if (!speechStyleBundles.containsKey(cacheKey)) {
            speechStyleBundles[cacheKey] = loadSpeechStyleProperties(character, currentLocale)
        }
        return speechStyleBundles[cacheKey]
    }

    fun scriptOrNull(key: String, speaker: String? = null, obj: Any? = null): String? {
        val styleValue = getStyleBundle(speaker)?.getProperty(key)
        val rawValue = styleValue ?: scriptBundles().firstNotNullOfOrNull { it.getProperty(key) }
        return rawValue?.let { if (obj != null) it.replacePlaceholders(obj) else it }
    }

    fun scriptForCharacter(character: String?, key: String, obj: Any? = null): String {
        return scriptOrNull(key, character, obj) ?: "Unknown property [$key]"
    }

    fun script(key: String, obj: Any? = null): String = scriptForCharacter(null, key, obj)

    //Script is different from other props in that some speech lines are optional, so we want a way to check if they exist without throwing an error.
    //If the script line doesn't exist, the dialogue system can decide to skip it.
    fun hasScript(key: String, speaker: String? = null): Boolean {
        return scriptOrNull(key, speaker) != null
    }

    fun questProp(key: String, obj: Any? = null): String = getLocalizedProp("quests", key, obj)
    fun charName(charId: String): String = charProp(charId)

    // --- Constant/Time Utilities (No functional change) ---

    fun const(constName: String): Double {
        return constJson[constName]?.jsonPrimitive?.double
            ?: .0.also { throw Exception("Could not find constant $constName") }
    }

    fun constInt(constName: String): Int {
        return constJson[constName]?.jsonPrimitive?.int
            ?: 0.also { throw Exception("Could not find constant $constName") }
    }

    val DT = (86400 / const("lengthOfDay")).toInt()
    val DTH = (24 / const("lengthOfDay"))
    val IDTH = (const("lengthOfDay") / 24.0).toInt()
    const val S_PER_HR = 3600

    fun toTotalMinutes(time: Int): Int = (time / (const("lengthOfDay") / 1440.0)).toInt()
    fun toMinutes(time: Int): Int = (time % constInt("lengthOfDay") / (const("lengthOfDay") / 1440.0)).toInt()
    fun toTotalHours(time: Int): Int = (time / (const("lengthOfDay") / 24.0)).toInt()
    fun toHours(time: Int): Int = (time % constInt("lengthOfDay") / (const("lengthOfDay") / 24.0)).toInt()
    fun toDays(time: Int): Int = (time / const("lengthOfDay")).toInt()

    val mutualityScale = const("mutualityMax") - const("mutualityMin")

    private fun String.replacePlaceholders(source: Any): String {
        val regex = "\\{VAR=([A-Za-z0-9_]+)}".toRegex()
        val kClass = source::class
        val propsByName = kClass.members.associateBy { it.name }

        return regex.replace(this) { matchResult ->
            val varName = matchResult.groupValues[1]
            val prop = propsByName[varName]
            prop?.call(source)?.toString() ?: matchResult.value // leave as-is if not found
        }
    }
}
