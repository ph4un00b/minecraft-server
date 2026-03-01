package com.colosseum.arena

import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.npc.NPCRegistry
import net.citizensnpcs.api.trait.Trait
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.plugin.java.JavaPlugin
import org.mcmonkey.sentinel.SentinelIntegration
import org.mcmonkey.sentinel.SentinelPlugin
import org.mcmonkey.sentinel.SentinelTrait
import java.util.concurrent.ConcurrentHashMap
import com.colosseum.arena.domain.SpawnPosition
import com.colosseum.arena.operations.PlayerSpawner

class NPCManager(
    private val plugin: JavaPlugin,
    private val playerSpawner: PlayerSpawner
) : Listener {
    
    companion object {
        private const val NPC_RADIUS = 6
        private const val DEFAULT_HEALTH = 20.0
        private const val DEFAULT_DAMAGE = 5.0
        private const val MAX_NPCS = 4
        private const val NPC_NAME_PREFIX = "ArenaGladiator_"
    }
    
    private val trackedNPCs = ConcurrentHashMap<Int, SpawnPosition>()
    private var npcHealth = DEFAULT_HEALTH
    private var npcDamage = DEFAULT_DAMAGE
    private var npcCount = 1
    private var npcEnabled = true
    
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }
    
    fun spawnArenaNPCs(world: World, baseY: Int) {
        if (!npcEnabled) {
            plugin.logger.info("[ArenaPlugin] NPC spawning disabled")
            return
        }
        
        val citizensPlugin = Bukkit.getPluginManager().getPlugin("Citizens")
        if (citizensPlugin == null || !citizensPlugin.isEnabled) {
            plugin.logger.severe("[ArenaPlugin] Citizens plugin not found or not enabled!")
            throw IllegalStateException("Citizens plugin is required but not available")
        }
        
        val sentinelPlugin = Bukkit.getPluginManager().getPlugin("Sentinel")
        if (sentinelPlugin == null || !sentinelPlugin.isEnabled) {
            plugin.logger.severe("[ArenaPlugin] Sentinel plugin not found or not enabled!")
            throw IllegalStateException("Sentinel plugin is required but not available")
        }
        
        plugin.logger.info("[ArenaPlugin] Spawning $npcCount Sentinel NPCs at baseY=$baseY")
        
        clearAllNPCs()
        
        val registry = CitizensAPI.getNPCRegistry()
        
        SpawnPosition.getAll().forEachIndexed { index, position ->
            if (index < npcCount) {
                val location = calculateNPCLocation(world, position, baseY)
                plugin.logger.info("[ArenaPlugin] Spawning Sentinel NPC #$index at $position -> $location")
                
                try {
                    val npc = createSentinelNPC(registry, location, index)
                    if (npc != null) {
                        trackedNPCs[npc.id] = position
                        plugin.logger.info("[ArenaPlugin] Sentinel NPC spawned successfully: ID=${npc.id}")
                    } else {
                        plugin.logger.warning("[ArenaPlugin] Failed to spawn NPC at $position")
                    }
                } catch (e: Exception) {
                    plugin.logger.severe("[ArenaPlugin] Error spawning NPC: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        
        plugin.logger.info("[ArenaPlugin] NPC spawning complete. Tracked NPCs: ${trackedNPCs.size}")
    }
    
    private fun calculateNPCLocation(world: World, position: SpawnPosition, baseY: Int): Location {
        val angleRad = Math.toRadians(position.angleDegrees)
        val x = (Math.cos(angleRad) * NPC_RADIUS).toInt()
        val z = (Math.sin(angleRad) * NPC_RADIUS).toInt()
        return Location(world, x + 0.5, (baseY + 1).toDouble(), z + 0.5)
    }
    
    private fun createSentinelNPC(registry: NPCRegistry, location: Location, index: Int): NPC? {
        val npcName = "$NPC_NAME_PREFIX$index"
        
        val npc = registry.createNPC(EntityType.PLAYER, npcName)
        if (npc == null) {
            plugin.logger.severe("[ArenaPlugin] Failed to create NPC in registry")
            return null
        }
        
        npc.spawn(location)
        
        if (!npc.isSpawned) {
            plugin.logger.severe("[ArenaPlugin] NPC failed to spawn at location")
            registry.deregister(npc)
            return null
        }
        
        val sentinel = npc.getOrAddTrait(SentinelTrait::class.java)
        if (sentinel == null) {
            plugin.logger.severe("[ArenaPlugin] Failed to add Sentinel trait to NPC")
            npc.destroy()
            return null
        }
        
        configureSentinelNPC(sentinel)
        
        return npc
    }
    
    private fun configureSentinelNPC(sentinel: SentinelTrait) {
        sentinel.health = npcHealth
        sentinel.damage = npcDamage
        
        // Target all players - use the proper API method
        sentinel.addTarget("Event:Player")
        
        // Set to melee combat
        sentinel.attackRate = 10
        sentinel.range = 16.0
        
        // Store spawn point but don't respawn automatically
        sentinel.spawnPoint = sentinel.npc?.storedLocation
        sentinel.respawnTime = -1
    }
    
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val npc = CitizensAPI.getNPCRegistry().getNPC(entity) ?: return
        
        if (trackedNPCs.containsKey(npc.id)) {
            plugin.logger.info("[ArenaPlugin] NPC ${npc.id} died, will stay dead until rebuild")
            trackedNPCs.remove(npc.id)
        }
    }
    
    fun clearAllNPCs() {
        val registry = CitizensAPI.getNPCRegistry()
        
        trackedNPCs.keys.forEach { npcId ->
            val npc = registry.getById(npcId)
            if (npc != null) {
                npc.destroy()
                plugin.logger.info("[ArenaPlugin] Removed NPC: $npcId")
            }
        }
        
        trackedNPCs.clear()
        plugin.logger.info("[ArenaPlugin] All NPCs cleared")
    }
    
    fun getNPCStatus(): String {
        return "NPCs: ${if (npcEnabled) "Enabled" else "Disabled"}, Count: $npcCount, Health: $npcHealth, Damage: $npcDamage"
    }
    
    fun toggleNPCs() {
        npcEnabled = !npcEnabled
        plugin.logger.info("[ArenaPlugin] NPCs ${if (npcEnabled) "enabled" else "disabled"}")
    }
    
    fun setNPCHealth(health: Double) {
        npcHealth = health.coerceAtLeast(1.0)
        plugin.logger.info("[ArenaPlugin] NPC health set to $npcHealth")
    }
    
    fun setNPCDamage(damage: Double) {
        npcDamage = damage.coerceAtLeast(1.0)
        plugin.logger.info("[ArenaPlugin] NPC damage set to $npcDamage")
    }
    
    fun setNPCCount(count: Int) {
        npcCount = count.coerceIn(0, MAX_NPCS)
        plugin.logger.info("[ArenaPlugin] NPC count set to $npcCount")
    }
    
    fun getNPCCount(): Int = npcCount
    
    fun getNPCHealth(): Double = npcHealth
    
    fun getNPCDamage(): Double = npcDamage
    
    fun isNPCEnabled(): Boolean = npcEnabled
}
