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
        "maleNames", "femaleNames", "nameModifier", // ADDED for name generation
        "quests"
    )

    // --- JSON Loading (No change, as JSON is typically non-localized) ---
    private fun loadJson(path: String): JsonObject {
        val content = Gdx.files?.internal(path)?.readString()
            ?: File("../assets/$path").readText()
        return Json.parseToJsonElement(content).jsonObject
    }

    val mapJson = loadJson("json/map.json")
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
        val defaultPath = Gdx.files.internal("texts/$baseName.properties")

        // Localized path: texts/ui_fr.properties (simple language matching)
        val localeTag = if (locale.language.isEmpty()) "" else "_${locale.language}"
        val localizedPath = Gdx.files.internal("texts/$baseName$localeTag.properties")

        val defaultContent = defaultPath.reader("UTF-8")
        val localizedContent = localizedPath.reader("UTF-8")

        val mergedProps = Properties()

        // 1. Load default properties (the base) with UTF-8 encoding
        mergedProps.load(defaultContent)

        // 2. Overwrite/merge with localized properties if they exist and are not the default path
        if (localizedPath != defaultPath) {
            val localizedProps =
                Properties().apply { load(localizedContent) }
            localizedProps.forEach { (k, v) -> mergedProps.setProperty(k.toString(), v.toString()) }
            Logger.write("Loaded localized properties for $baseName: $localizedPath", Logger.LogLevel.INFO)
        }

        Logger.write("Loaded default properties for $baseName: $defaultPath", Logger.LogLevel.INFO)
        return mergedProps
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
        return getLocalizedProp("place", key, obj)
    }

    fun charProp(key: String, obj: Any? = null): String = getLocalizedProp("character", key, obj)
    fun itemProp(key: String, obj: Any? = null): String = getLocalizedProp("resources", key, obj)
    fun script(key: String, obj: Any? = null): String = getLocalizedProp("DefaultCharacter", key, obj)
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
