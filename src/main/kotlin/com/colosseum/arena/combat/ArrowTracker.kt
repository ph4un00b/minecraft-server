@file:Suppress("DEPRECATION")

package com.colosseum.arena.combat

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Arrow
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks persistent arrows in the arena using Paper API
 * Converts arrows to items on impact
 * Enforces limit: 5 arrows per online player
 * Items have unlimited lifetime (never despawn)
 */
class ArrowTracker(private val plugin: JavaPlugin) : Listener {

    companion object {
        private const val ARROWS_PER_PLAYER = 5
        private const val METADATA_KEY = "arena_arrow"
    }

    // Track item entities with their timestamps
    private val trackedItems = ConcurrentHashMap<Item, Long>()

    init {
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin)

        plugin.logger.info("[ArrowTracker] Initialized - Arrows convert to infinite-lifetime items")
    }

    /**
     * Called when a projectile is launched
     */
    @Suppress("DEPRECATION")
    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity

        // Only track arrows shot by players
        if (projectile is Arrow && projectile.shooter is Player) {
            // Mark arrow with metadata so we know it's an arena arrow
            projectile.setMetadata(METADATA_KEY, FixedMetadataValue(plugin, true))
        }
    }

    /**
     * Called when arrow hits something (ground/wall)
     * Converts arrow to item with unlimited lifetime
     */
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity

        // Only convert arrows marked by our system
        if (projectile is Arrow && projectile.hasMetadata(METADATA_KEY)) {
            // Convert to item
            convertArrowToItem(projectile)
        }
    }

    /**
     * Convert arrow entity to item entity with unlimited lifetime
     */
    private fun convertArrowToItem(arrow: Arrow) {
        val location = arrow.location
        val world = location.world

        // Create arrow item stack
        val itemStack = ItemStack(Material.ARROW, 1)

        // Spawn item entity
        val item = world.dropItem(location, itemStack)

        // Configure item with Paper API unlimited lifetime
        item.setUnlimitedLifetime(true)  // Native Paper API - item lives forever!
        item.setWillAge(false)           // Don't age/despawn
        item.setCanPlayerPickup(true)    // Allow player pickup
        item.setCanMobPickup(false)      // Don't let mobs pick it up
        item.pickupDelay = 0             // Can be picked up immediately

        // Let gravity work naturally (your requirement #1)
        // item.setGravity(true) // This is default

        // Track the item
        trackedItems[item] = System.currentTimeMillis()

        // Remove original arrow
        arrow.remove()

        // Enforce limit
        enforceArrowLimit()
    }

    /**
     * Called when player picks up an item
     */
    @EventHandler
    fun onItemPickup(event: EntityPickupItemEvent) {
        val item = event.item

        // Remove from tracking if it's our tracked arrow item
        if (trackedItems.containsKey(item)) {
            trackedItems.remove(item)

            // Debug logging
            if (event.entity is Player) {
                plugin.logger.fine("[ArrowTracker] ${event.entity.name} picked up an arrow")
            }
        }
    }

    /**
     * Called when player quits - recalculate limits
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Enforce limit immediately when player leaves
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            enforceArrowLimit()
        }, 1L)
    }

    /**
     * Enforce the arrow limit: 5 arrows per online player
     * Removes oldest items first
     */
    private fun enforceArrowLimit() {
        val onlinePlayers = Bukkit.getOnlinePlayers().size
        val maxAllowed = onlinePlayers * ARROWS_PER_PLAYER

        // Get current item count
        val currentCount = trackedItems.size

        if (currentCount > maxAllowed) {
            // Sort by timestamp (oldest first)
            val sortedItems = trackedItems.entries.sortedBy { it.value }

            // Remove excess items (oldest first)
            val toRemove = currentCount - maxAllowed
            for (i in 0 until toRemove) {
                if (i < sortedItems.size) {
                    val item = sortedItems[i].key
                    removeItem(item)
                }
            }

            plugin.logger.info("[ArrowTracker] Removed $toRemove oldest items. Current: ${trackedItems.size}/$maxAllowed")
        }
    }

    /**
     * Remove a specific item from world and tracking
     */
    private fun removeItem(item: Item) {
        trackedItems.remove(item)
        if (!item.isDead) {
            item.remove()
        }
    }

    /**
     * Get current item count
     */
    fun getArrowCount(): Int = trackedItems.size

    /**
     * Get maximum allowed items based on online players
     */
    fun getMaxAllowed(): Int = Bukkit.getOnlinePlayers().size * ARROWS_PER_PLAYER

    /**
     * Clear all tracked items (for arena rebuild)
     */
    fun clearAllArrows() {
        trackedItems.keys.forEach { item ->
            if (!item.isDead) {
                item.remove()
            }
        }
        trackedItems.clear()
        plugin.logger.info("[ArrowTracker] Cleared all arrow items")
    }
}
