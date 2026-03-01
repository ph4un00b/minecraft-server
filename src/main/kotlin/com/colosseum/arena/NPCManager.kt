package com.colosseum.arena

import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Damageable
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ConcurrentHashMap
import com.colosseum.arena.domain.SpawnPosition
import com.colosseum.arena.operations.PlayerSpawner

class NPCManager(
    private val plugin: JavaPlugin,
    private val playerSpawner: PlayerSpawner
) : Listener {
    
    companion object {
        private const val NPC_RADIUS = 6  // Inside arena, away from walls
        private const val DEFAULT_HEALTH = 1.0
        private const val MAX_NPCS = 4  // Maximum of 4 NPCs allowed
    }
    
    private val trackedNPCs = ConcurrentHashMap<Villager, SpawnPosition>()
    private var npcHealth = DEFAULT_HEALTH
    private var npcCount = 1  // Default to 1 NPC
    private var npcEnabled = true
    
    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }
    
    fun spawnArenaNPCs(world: World, baseY: Int) {
        if (!npcEnabled) {
            plugin.logger.info("[ArenaPlugin] NPC spawning disabled")
            return
        }
        
        plugin.logger.info("[ArenaPlugin] Spawning $npcCount NPCs at baseY=$baseY, radius=$NPC_RADIUS")
        
        // Clear existing NPCs first
        clearAllNPCs()
        
        // Spawn NPCs at opposite positions to players
        SpawnPosition.getAll().forEachIndexed { index, position ->
            if (index < npcCount) {
                val location = calculateNPCLocation(world, position, baseY)
                plugin.logger.info("[ArenaPlugin] Spawning NPC #$index at $position -> $location")
                val villager = spawnVillagerNPC(world, location)
                plugin.logger.info("[ArenaPlugin] NPC spawned successfully: ${villager.entityId}")
                configureNPC(villager, position)
                trackedNPCs[villager] = position
            }
        }
        
        plugin.logger.info("[ArenaPlugin] NPC spawning complete. Tracked NPCs: ${trackedNPCs.size}")
    }
    
    private fun calculateNPCLocation(world: World, position: SpawnPosition, baseY: Int): Location {
        // Spawn NPC at same angle as position but inside the arena
        val angleRad = Math.toRadians(position.angleDegrees)
        val x = (Math.cos(angleRad) * NPC_RADIUS).toInt()
        val z = (Math.sin(angleRad) * NPC_RADIUS).toInt()
        return Location(world, x + 0.5, (baseY + 1).toDouble(), z + 0.5)
    }
    
    @Suppress("DEPRECATION")
    private fun spawnVillagerNPC(world: World, location: Location): Villager {
        return world.spawn(location, Villager::class.java).also { villager ->
            villager.isPersistent = true
            villager.removeWhenFarAway = false
            villager.customName = "Arena Guardian"
            villager.isCustomNameVisible = false
        }
    }
    
    private fun configureNPC(villager: Villager, position: SpawnPosition) {
        // Set health for one-shot kills
        // Note: Villager is always Damageable (extends LivingEntity)
        val maxHealthAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic.max_health"))
        maxHealthAttr?.let { attr ->
            villager.getAttribute(attr)?.baseValue = npcHealth
        }
        villager.health = npcHealth
        
        // Prevent movement with maximum slowness
        val slownessEffect = PotionEffect(
            PotionEffectType.SLOWNESS,
            Integer.MAX_VALUE,
            255,
            true,
            false
        )
        villager.addPotionEffect(slownessEffect)
        
        // Add jump effect to prevent jumping
        val jumpEffect = PotionEffect(
            PotionEffectType.JUMP_BOOST,
            Integer.MAX_VALUE,
            128,
            true,
            false
        )
        villager.addPotionEffect(jumpEffect)
        
        // Set random profession for variety
        val professions = listOf(
            Villager.Profession.LIBRARIAN,
            Villager.Profession.FARMER,
            Villager.Profession.FISHERMAN,
            Villager.Profession.SHEPHERD,
            Villager.Profession.FLETCHER
        )
        val randomProfession = professions.random()
        villager.setProfession(randomProfession)
    }
    
    private fun getOppositeSpawn(position: SpawnPosition): SpawnPosition {
        return when (position) {
            SpawnPosition.EAST -> SpawnPosition.WEST
            SpawnPosition.SOUTH -> SpawnPosition.NORTH
            SpawnPosition.WEST -> SpawnPosition.EAST
            SpawnPosition.NORTH -> SpawnPosition.SOUTH
        }
    }
    
    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Villager && trackedNPCs.containsKey(entity)) {
            // One-shot kill handling
            if (entity.health - event.damage <= 0) {
                event.isCancelled = true
                handleNPCKill(entity)
            }
        }
    }
    
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity is Villager && trackedNPCs.containsKey(entity)) {
            event.isCancelled = true
            // Custom death handling
            handleNPCDeath(entity)
        }
    }
    
    private fun handleNPCKill(villager: Villager) {
        // Play death effects
        villager.world.playSound(villager.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f)
        villager.world.spawnParticle(Particle.EXPLOSION, villager.location, 10)
        villager.world.spawnParticle(Particle.FLAME, villager.location, 20)
        villager.world.spawnParticle(Particle.LARGE_SMOKE, villager.location, 15)
        
        // Remove NPC
        villager.remove()
    }
    
    private fun handleNPCDeath(villager: Villager) {
        // Custom death handling - play effects, drop items, etc.
        villager.world.playEffect(villager.location, Effect.SMOKE, 0)
        villager.world.playSound(villager.location, Sound.ENTITY_VILLAGER_DEATH, 1.0f, 1.0f)
        
        // Drop custom items
        val drops = listOf(
            ItemStack(Material.EMERALD, 1),
            ItemStack(Material.GOLD_INGOT, 1),
            ItemStack(Material.DIAMOND, 1)
        )
        drops.forEach { drop ->
            villager.world.dropItemNaturally(villager.location, drop)
        }
    }
    
    fun clearAllNPCs() {
        trackedNPCs.keys.forEach { npc ->
            if (!npc.isDead) {
                npc.remove()
            }
        }
        trackedNPCs.clear()
    }
    
    fun getNPCStatus(): String {
        return "NPCs: ${if (npcEnabled) "Enabled" else "Disabled"}, Count: $npcCount, Health: $npcHealth"
    }
    
    fun toggleNPCs() {
        npcEnabled = !npcEnabled
    }
    
    fun setNPCHealth(health: Double) {
        npcHealth = health.coerceAtLeast(0.1)
    }
    
    fun setNPCCount(count: Int) {
        npcCount = count.coerceIn(0, 4)
    }
    
    fun getNPCCount(): Int = npcCount
    
    fun getNPCHealth(): Double = npcHealth
    
    fun isNPCEnabled(): Boolean = npcEnabled
}