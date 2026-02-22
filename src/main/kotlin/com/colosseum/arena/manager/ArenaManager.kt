package com.colosseum.arena.manager

import org.bukkit.World
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import com.colosseum.arena.domain.ArenaType
import com.colosseum.arena.domain.ArenaConfig
import com.colosseum.arena.builders.SimpleArena
import com.colosseum.arena.builders.DetailedArena
import com.colosseum.arena.operations.ArenaClearer
import com.colosseum.arena.operations.YLevelChanger
import com.colosseum.core.storage.PropertiesStorage

/**
 * Arena Manager - Facade for all arena operations
 * Contains ONLY PDC logic and delegation
 * All actual building logic is delegated to builders and operations
 */
class ArenaManager(
    private val simpleArena: SimpleArena,
    private val detailedArena: DetailedArena,
    private val clearer: ArenaClearer,
    private val yLevelChanger: YLevelChanger,
    private val storage: PropertiesStorage,
    plugin: JavaPlugin
) {
    private val arenaBuiltKey = NamespacedKey(plugin, "arena_built")
    private val arenaTypeKey = NamespacedKey(plugin, "arena_type")

    /**
     * Check if arena exists via PDC, build if not
     * This is the ONLY logic in manager: PDC check
     */
    fun checkAndBuild(world: World): Boolean {
        val pdc = world.persistentDataContainer

        if (pdc.has(arenaBuiltKey, PersistentDataType.INTEGER)) {
            return false // Already built, skip
        }

        // Build new arena
        val type = ArenaType.valueOf(storage.arenaType.uppercase())
        build(world, type)

        // Mark as built in PDC
        pdc.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
        pdc.set(arenaTypeKey, PersistentDataType.STRING, type.name.lowercase())

        return true
    }

    /**
     * Rebuild arena at current location
     * Logic: clear → build → update PDC
     */
    fun rebuild(world: World, type: ArenaType) {
        // Clear area
        clearer.clear(world)

        // Build new
        build(world, type)

        // Update PDC
        val pdc = world.persistentDataContainer
        pdc.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
        pdc.set(arenaTypeKey, PersistentDataType.STRING, type.name.lowercase())
    }

    /**
     * Change Y level and rebuild
     * Delegates sequence to YLevelChanger (Option A)
     */
    fun changeYLevel(world: World, newY: Int, type: ArenaType) {
        yLevelChanger.change(world, newY) {
            // Callback: rebuild after Y is saved and area is cleared
            rebuild(world, type)
        }
    }

    /**
     * Get current arena type from storage
     */
    fun getCurrentType(): ArenaType {
        return ArenaType.valueOf(storage.arenaType.uppercase())
    }

    /**
     * Get current arena base Y from storage
     */
    fun getCurrentBaseY(): Int {
        return storage.arenaBaseY
    }

    /**
     * Update spawn location to arena center
     */
    fun updateSpawn(world: World) {
        val spawnLocation = Location(world, 0.5, (storage.arenaBaseY + 1).toDouble(), 0.5)
        world.spawnLocation = spawnLocation
    }

    /**
     * Reset arena built flag (for rebuilds)
     */
    fun resetBuiltFlag(world: World) {
        world.persistentDataContainer.remove(arenaBuiltKey)
    }

    /**
     * Private: delegate build to appropriate builder
     */
    private fun build(world: World, type: ArenaType) {
        val config = ArenaConfig(storage.arenaBaseY, type)
        when (type) {
            ArenaType.SIMPLE -> simpleArena.build(world, config)
            ArenaType.DETAILED -> detailedArena.build(world, config)
        }
    }
}
