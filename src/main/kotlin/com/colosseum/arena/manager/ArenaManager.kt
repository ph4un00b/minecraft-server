package com.colosseum.arena.manager

import org.bukkit.World
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import com.colosseum.arena.domain.ArenaType
import com.colosseum.arena.domain.ArenaConfig
import com.colosseum.arena.builders.SimpleArena
import com.colosseum.arena.builders.DetailedArena
import com.colosseum.arena.operations.ArenaClearer
import com.colosseum.arena.operations.YLevelChanger
import com.colosseum.arena.operations.PlayerSpawner
import com.colosseum.arena.combat.CombatKit
import com.colosseum.arena.combat.ArrowTracker
import com.colosseum.core.storage.PropertiesStorage

/**
 * Arena Manager - Facade for all arena operations
 * Delegates spawn logic to PlayerSpawner
 */
class ArenaManager(
    private val simpleArena: SimpleArena,
    private val detailedArena: DetailedArena,
    private val clearer: ArenaClearer,
    private val yLevelChanger: YLevelChanger,
    private val playerSpawner: PlayerSpawner,
    private val combatKit: CombatKit,
    private val arrowTracker: ArrowTracker,
    private val storage: PropertiesStorage,
    plugin: JavaPlugin
) {
    private val arenaBuiltKey = NamespacedKey(plugin, "arena_built")
    private val arenaTypeKey = NamespacedKey(plugin, "arena_type")

    /**
     * Get the next spawn point for a player
     * Delegates to PlayerSpawner
     */
    fun getNextSpawnPoint(world: World): Location {
        return playerSpawner.getNextSpawnPoint(world, storage.arenaBaseY)
    }
    
    /**
     * Build spawn point floor markers during arena construction
     * Delegates to PlayerSpawner
     */
    fun buildSpawnMarkers(world: World) {
        playerSpawner.buildSpawnMarkers(world, storage.arenaBaseY)
    }
    
    /**
     * Reset spawn rotation
     * Delegates to PlayerSpawner
     */
    fun resetSpawnRotation() {
        playerSpawner.resetRotation()
    }
    
    /**
     * Get spawn location name based on coordinates
     * Delegates to PlayerSpawner
     */
    fun getSpawnLocationName(x: Int, z: Int): String {
        return playerSpawner.getSpawnLocationName(x, z)
    }

    /**
     * Check if arena exists via PDC, build if not
     */
    fun checkAndBuild(world: World): Boolean {
        val pdc = world.persistentDataContainer

        if (pdc.has(arenaBuiltKey, PersistentDataType.INTEGER)) {
            return false // Already built, skip
        }

        // Build new arena
        val type = ArenaType.valueOf(storage.arenaType.uppercase())
        build(world, type)
        
        // Build spawn markers (delegated to PlayerSpawner)
        buildSpawnMarkers(world)

        // Mark as built in PDC
        pdc.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
        pdc.set(arenaTypeKey, PersistentDataType.STRING, type.name.lowercase())

        return true
    }

    /**
     * Rebuild arena at current location
     */
    fun rebuild(world: World, type: ArenaType) {
        // Clear area blocks
        clearer.clear(world)
        
        // Clear all persistent arrows
        arrowTracker.clearAllArrows()

        // Build new
        build(world, type)
        
        // Build spawn markers (delegated to PlayerSpawner)
        buildSpawnMarkers(world)
        
        // Reset spawn rotation for fresh start
        resetSpawnRotation()

        // Update PDC
        val pdc = world.persistentDataContainer
        pdc.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
        pdc.set(arenaTypeKey, PersistentDataType.STRING, type.name.lowercase())
    }

    /**
     * Change Y level and rebuild
     */
    fun changeYLevel(world: World, newY: Int, type: ArenaType) {
        yLevelChanger.change(world, newY) {
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
     * Update spawn location to center (fallback only)
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
     * Equip player with combat kit
     */
    fun equipPlayer(player: Player) {
        combatKit.equipPlayer(player)
    }

    /**
     * Restock player with arrows and repair bow
     */
    fun restockPlayer(player: Player): Boolean {
        return combatKit.restockPlayer(player)
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
