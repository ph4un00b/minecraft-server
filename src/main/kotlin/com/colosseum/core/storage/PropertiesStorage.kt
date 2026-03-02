package com.colosseum.core.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * Manages arena configuration persistence in phau.properties
 * Single source of truth for arena settings
 */
class PropertiesStorage(private val logger: (String) -> Unit) {
    companion object {
        private const val FILENAME = "phau.properties"
        private const val DEFAULT_BASE_Y = 64
        private const val DEFAULT_TYPE = "detailed"
    }

    private val propsFile = File(FILENAME)
    private val properties = Properties()

    /**
     * Current arena base Y level
     */
    var arenaBaseY: Int = DEFAULT_BASE_Y
        private set

    /**
     * Current arena type (simple or detailed)
     */
    var arenaType: String = DEFAULT_TYPE
        private set

    init {
        load()
    }

    /**
     * Load all properties from file
     */
    fun load(): Boolean {
        return try {
            if (propsFile.exists()) {
                FileInputStream(propsFile).use { properties.load(it) }

                // Parse values
                arenaBaseY = properties.getProperty("arena-base-y", DEFAULT_BASE_Y.toString())
                    .toIntOrNull() ?: DEFAULT_BASE_Y
                arenaType = properties.getProperty("arena-type", DEFAULT_TYPE)
                    ?.lowercase() ?: DEFAULT_TYPE

                logger("Loaded arena-base-y=$arenaBaseY, arena-type=$arenaType from $FILENAME")
                true
            } else {
                logger("$FILENAME not found, using defaults: base-y=$DEFAULT_BASE_Y, type=$DEFAULT_TYPE")
                arenaBaseY = DEFAULT_BASE_Y
                arenaType = DEFAULT_TYPE
                false
            }
        } catch (e: Exception) {
            logger("Failed to load $FILENAME: ${e.message}")
            arenaBaseY = DEFAULT_BASE_Y
            arenaType = DEFAULT_TYPE
            false
        }
    }

    /**
     * Save all properties to file
     */
    fun save(): Boolean {
        return try {
            FileOutputStream(propsFile).use {
                properties.store(it, "Arena Configuration")
            }
            logger("Saved configuration to $FILENAME")
            true
        } catch (e: Exception) {
            logger("Failed to save $FILENAME: ${e.message}")
            false
        }
    }

    /**
     * Update arena base Y level
     */
    fun setArenaBaseY(newY: Int): Boolean {
        arenaBaseY = newY
        properties.setProperty("arena-base-y", newY.toString())
        return save()
    }

    /**
     * Update arena type
     */
    fun setArenaType(newType: String): Boolean {
        val normalizedType = newType.lowercase()
        if (normalizedType !in listOf("simple", "detailed")) {
            logger("Invalid arena type: $newType")
            return false
        }
        arenaType = normalizedType
        properties.setProperty("arena-type", normalizedType)
        return save()
    }

    /**
     * Get configuration summary for display
     */
    fun getConfigSummary(): String {
        return "base-y=$arenaBaseY, type=$arenaType"
    }
}
