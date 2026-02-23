package com.colosseum.arena.combat

import org.bukkit.Bukkit
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks persistent arrows in the arena
 * Enforces limit: 5 arrows per online player
 * Removes oldest arrows when limit exceeded
 */
class ArrowTracker(private val plugin: JavaPlugin) : Listener {
    
    companion object {
        private const val ARROWS_PER_PLAYER = 5
        private const val METADATA_KEY = "arena_arrow"
        private const val METADATA_SHOOTER = "arena_shooter"
    }
    
    // Track arrows with their timestamps (for age-based removal)
    private val trackedArrows = ConcurrentHashMap<Arrow, Long>()
    
    init {
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin)
        
        // Start cleanup task to remove invalid arrows
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            cleanupInvalidArrows()
        }, 20L, 20L) // Check every second
    }
    
    /**
     * Called when a projectile is launched
     */
    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val projectile = event.entity
        
        // Only track arrows shot by players
        if (projectile is Arrow && event.entity.shooter is Player) {
            val shooter = event.entity.shooter as Player
            
            // Mark arrow with metadata
            projectile.setMetadata(METADATA_KEY, FixedMetadataValue(plugin, true))
            projectile.setMetadata(METADATA_SHOOTER, FixedMetadataValue(plugin, shooter.uniqueId.toString()))
        }
    }
    
    /**
     * Called when arrow hits something (ground/wall)
     */
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        
        // Only track arrows marked by our system
        if (projectile is Arrow && projectile.hasMetadata(METADATA_KEY)) {
            // Add to tracking with timestamp
            trackedArrows[projectile] = System.currentTimeMillis()
            
            // Note: Arrow entities in ground can't have pickupDelay set
            // They persist until picked up naturally or removed by limit
            // We rely on the fact that we're removing old arrows when limit is hit
            
            // Enforce limit
            enforceArrowLimit()
        }
    }
    
    /**
     * Called when player picks up an arrow
     */
    @EventHandler
    fun onArrowPickup(event: PlayerPickupArrowEvent) {
        val arrow = event.arrow
        
        // Remove from tracking if it's our arrow
        if (trackedArrows.containsKey(arrow)) {
            trackedArrows.remove(arrow)
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
     * Removes oldest arrows first
     */
    private fun enforceArrowLimit() {
        val onlinePlayers = Bukkit.getOnlinePlayers().size
        val maxAllowed = onlinePlayers * ARROWS_PER_PLAYER
        
        // Get current arrow count
        val currentCount = trackedArrows.size
        
        if (currentCount > maxAllowed) {
            // Sort by timestamp (oldest first)
            val sortedArrows = trackedArrows.entries.sortedBy { it.value }
            
            // Remove excess arrows (oldest first)
            val toRemove = currentCount - maxAllowed
            for (i in 0 until toRemove) {
                if (i < sortedArrows.size) {
                    val arrow = sortedArrows[i].key
                    removeArrow(arrow)
                }
            }
            
            plugin.logger.info("[ArrowTracker] Removed $toRemove oldest arrows. Current: ${trackedArrows.size}/$maxAllowed")
        }
    }
    
    /**
     * Remove a specific arrow from world and tracking
     */
    private fun removeArrow(arrow: Arrow) {
        trackedArrows.remove(arrow)
        if (!arrow.isDead) {
            arrow.remove()
        }
    }
    
    /**
     * Clean up invalid arrows (dead, picked up, or despawned)
     */
    private fun cleanupInvalidArrows() {
        val invalidArrows = trackedArrows.keys.filter { arrow ->
            arrow.isDead || !arrow.isValid || arrow.isOnGround.not()
        }
        
        invalidArrows.forEach { trackedArrows.remove(it) }
        
        if (invalidArrows.isNotEmpty()) {
            plugin.logger.fine("[ArrowTracker] Cleaned up ${invalidArrows.size} invalid arrows")
        }
    }
    
    /**
     * Get current arrow count
     */
    fun getArrowCount(): Int = trackedArrows.size
    
    /**
     * Get maximum allowed arrows based on online players
     */
    fun getMaxAllowed(): Int = Bukkit.getOnlinePlayers().size * ARROWS_PER_PLAYER
    
    /**
     * Clear all tracked arrows (for arena rebuild)
     */
    fun clearAllArrows() {
        trackedArrows.keys.forEach { arrow ->
            if (!arrow.isDead) {
                arrow.remove()
            }
        }
        trackedArrows.clear()
        plugin.logger.info("[ArrowTracker] Cleared all arrows")
    }
}
