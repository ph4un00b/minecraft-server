package com.colosseum.arena.manager

import com.colosseum.arena.NPCManager
import com.colosseum.arena.TargetBlockListener
import com.colosseum.arena.builders.DetailedArena
import com.colosseum.arena.builders.QueuedBlockPlacer
import com.colosseum.arena.builders.SimpleArena
import com.colosseum.arena.combat.ArrowTracker
import com.colosseum.arena.combat.CombatKit
import com.colosseum.arena.combat.KitConfig
import com.colosseum.arena.config.TargetBlockConfig
import com.colosseum.arena.domain.ArenaConfig
import com.colosseum.arena.domain.ArenaType
import com.colosseum.arena.operations.ArenaClearer
import com.colosseum.arena.operations.PlayerSpawner
import com.colosseum.arena.operations.YLevelChanger
import com.colosseum.core.storage.PropertiesStorage
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

/**
 * Arena Manager - Facade for all arena operations
 * Delegates spawn logic to PlayerSpawner
 * Supports both sync and async arena building
 */
class ArenaManager(private val plugin: JavaPlugin) {
    companion object {
        // Number of blocks to place per tick
        const val BLOCKS_PER_TICK = 100

        // Show progress every second (20 ticks)
        const val PROGRESS_INTERVAL = 20L
    }

    private val storage by lazy {
        PropertiesStorage { msg -> plugin.logger.info(msg) }
    }
    private val simpleArena by lazy { SimpleArena() }
    private val detailedArena by lazy { DetailedArena() }
    private val clearer by lazy { ArenaClearer() }
    private val playerSpawner by lazy { PlayerSpawner() }
    private val combatKit by lazy { CombatKit(KitConfig()) }
    val arrowTracker by lazy { ArrowTracker(plugin) }
    val npcManager by lazy { NPCManager(plugin, playerSpawner) }
    val targetBlockConfig = TargetBlockConfig()
    val targetBlockListener by lazy {
        TargetBlockListener(plugin, npcManager, targetBlockConfig).also {
            npcManager.setTargetBlockListener(it)
        }
    }
    private val yLevelChanger by lazy {
        YLevelChanger(storage, clearer, npcManager)
    }

    private val arenaBuiltKey = NamespacedKey(plugin, "arena_built")
    private val arenaTypeKey = NamespacedKey(plugin, "arena_type")

    /**
     * Check if an async build is currently in progress
     */
    @Volatile
    private var isBuilding = false

    fun isBuilding(): Boolean = isBuilding

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

        // Set up target block listener
        targetBlockListener.setWorldInfo(world, storage.arenaBaseY)

        // Spawn NPCs after arena build
        npcManager.spawnArenaNPCs(world, storage.arenaBaseY)

        // Mark as built in PDC
        pdc.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
        pdc.set(arenaTypeKey, PersistentDataType.STRING, type.name.lowercase())

        return true
    }

    /**
     * Rebuild arena synchronously (immediate, may cause lag)
     */
    fun rebuild(world: World, type: ArenaType) {
        // Clear area blocks
        clearer.clear(world)

        // Clear all persistent arrows
        arrowTracker.clearAllArrows()

        // Clear NPCs
        npcManager.clearAllNPCs()

        // Build new
        build(world, type)

        // Build spawn markers (delegated to PlayerSpawner)
        buildSpawnMarkers(world)

        // Set up target block listener
        targetBlockListener.setWorldInfo(world, storage.arenaBaseY)

        // Spawn NPCs after arena build
        npcManager.spawnArenaNPCs(world, storage.arenaBaseY)

        // Reset spawn rotation for fresh start
        resetSpawnRotation()

        // Update PDC
        val pdc = world.persistentDataContainer
        pdc.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
        pdc.set(arenaTypeKey, PersistentDataType.STRING, type.name.lowercase())
    }

    /**
     * Rebuild arena asynchronously (lag-free, gradual placement)
     * @param world The world to build in
     * @param type The arena type
     * @param onProgress Called with (placedCount, totalCount, percentage)
     * @param onComplete Called when build is finished
     */
    fun rebuildAsync(
        world: World,
        type: ArenaType,
        onProgress: (Int, Int, Int) -> Unit = { _, _, _ -> },
        onComplete: () -> Unit = {},
    ) {
        if (isBuilding) {
            throw IllegalStateException(
                "Another async build is already in progress",
            )
        }

        isBuilding = true

        // Clear area blocks
        clearer.clear(world)

        // Clear all persistent arrows
        arrowTracker.clearAllArrows()

        // Clear NPCs
        npcManager.clearAllNPCs()

        // Queue all blocks for placement
        val placer = QueuedBlockPlacer()
        val config = ArenaConfig(storage.arenaBaseY, type, targetBlockConfig)

        when (type) {
            ArenaType.SIMPLE -> simpleArena.build(world, config, placer)
            ArenaType.DETAILED -> detailedArena.build(world, config, placer)
        }

        val blocks = placer.getBlocks()
        val totalBlocks = blocks.size
        var placedBlocks = 0

        // Start async placement
        object : BukkitRunnable() {
            private var lastProgressUpdate = 0

            override fun run() {
                if (placedBlocks >= totalBlocks) {
                    // Build complete
                    isBuilding = false

                    // Build spawn markers and spawn NPCs
                    buildSpawnMarkers(world)
                    targetBlockListener.setWorldInfo(world, storage.arenaBaseY)
                    npcManager.spawnArenaNPCs(world, storage.arenaBaseY)
                    resetSpawnRotation()

                    // Update PDC
                    val pdc = world.persistentDataContainer
                    pdc.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
                    pdc.set(
                        arenaTypeKey,
                        PersistentDataType.STRING,
                        type.name.lowercase(),
                    )

                    onComplete()
                    cancel()
                    return
                }

                // Place next batch
                val endIndex =
                    minOf(placedBlocks + BLOCKS_PER_TICK, totalBlocks)
                for (i in placedBlocks until endIndex) {
                    val block = blocks[i]
                    val world = block.world
                    val x = block.x
                    val y = block.y
                    val z = block.z
                    world.getBlockAt(x, y, z).type = block.material
                }

                placedBlocks = endIndex
                val percentage = (placedBlocks * 100 / totalBlocks)

                // Update progress every second or at milestones
                val progressUpdateThreshold = BLOCKS_PER_TICK * 20
                val progressDelta = placedBlocks - lastProgressUpdate
                val shouldUpdateProgress =
                    progressDelta >= progressUpdateThreshold ||
                        percentage % 10 == 0
                if (shouldUpdateProgress) {
                    onProgress(placedBlocks, totalBlocks, percentage)
                    lastProgressUpdate = placedBlocks
                }
            }
        }.runTaskTimer(plugin, 0L, 1L) // Run every tick
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
        val spawnLocation =
            Location(world, 0.5, (storage.arenaBaseY + 1).toDouble(), 0.5)
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
     * Private: delegate build to appropriate builder (sync)
     */
    private fun build(world: World, type: ArenaType) {
        val config = ArenaConfig(storage.arenaBaseY, type, targetBlockConfig)
        when (type) {
            ArenaType.SIMPLE -> simpleArena.build(world, config)
            ArenaType.DETAILED -> detailedArena.build(world, config)
        }
    }
}
