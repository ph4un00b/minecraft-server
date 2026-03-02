package com.colosseum.arena

import com.colosseum.arena.domain.NPCAttackType
import com.colosseum.arena.domain.NPCDecisions
import com.colosseum.arena.domain.SpawnPosition
import com.colosseum.arena.operations.PlayerSpawner
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.npc.NPCRegistry
import net.citizensnpcs.api.trait.trait.Equipment
import net.citizensnpcs.api.trait.trait.Inventory
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.mcmonkey.sentinel.SentinelTrait
import java.util.concurrent.ConcurrentHashMap

class NPCManager(
    private val plugin: JavaPlugin,
    private val playerSpawner: PlayerSpawner,
) : Listener {
    companion object {
        private const val NPC_RADIUS = 6
        private const val DEFAULT_HEALTH = 20.0
        private const val DEFAULT_DAMAGE = 5.0
        private const val MAX_NPCS = 4
        private const val NPC_NAME_PREFIX = "ArenaGladiator_"
        private const val YELLOW = "\u001B[33m"
        private const val RESET = "\u001B[0m"
        private const val RED = "\u001B[31m"
    }

    private val trackedNPCs = ConcurrentHashMap<Int, SpawnPosition>()
    private var npcHealth = DEFAULT_HEALTH
    private var npcDamage = DEFAULT_DAMAGE
    private var npcCount = 1
    private var npcEnabled = true
    private var npcAttackType = NPCAttackType.FIREBALL
    private var currentBatchSize = 1
    private var lastSpawnWorld: World? = null
    private var lastSpawnBaseY: Int = 0

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun spawnArenaNPCs(world: World, baseY: Int) {
        plugin.logger.info(
            "$YELLOW[ArenaPlugin] spawnArenaNPCs called " +
                "with attackType=$npcAttackType$RESET",
        )
        if (!npcEnabled) {
            plugin.logger.info(
                "$YELLOW[ArenaPlugin] NPC spawning disabled$RESET",
            )
            return
        }

        val citizensPlugin = Bukkit.getPluginManager().getPlugin("Citizens")
        if (citizensPlugin == null || !citizensPlugin.isEnabled) {
            plugin.logger.severe(
                "[ArenaPlugin] Citizens plugin not found or not enabled!",
            )
            throw IllegalStateException(
                "Citizens plugin is required but not available",
            )
        }

        val sentinelPlugin = Bukkit.getPluginManager().getPlugin("Sentinel")
        if (sentinelPlugin == null || !sentinelPlugin.isEnabled) {
            plugin.logger.severe(
                "[ArenaPlugin] Sentinel plugin not found or not enabled!",
            )
            throw IllegalStateException(
                "Sentinel plugin is required but not available",
            )
        }

        plugin.logger.info(
            "$YELLOW[ArenaPlugin] Spawning $npcCount Sentinel NPCs " +
                "at baseY=$baseY$RESET",
        )

        lastSpawnWorld = world
        lastSpawnBaseY = baseY
        currentBatchSize = npcCount

        clearAllNPCs()

        val registry = CitizensAPI.getNPCRegistry()

        SpawnPosition.getAll().forEachIndexed { index, position ->
            if (index < npcCount) {
                val location = calculateNPCLocation(world, position, baseY)
                plugin.logger.info(
                    "$YELLOW[ArenaPlugin] Spawning Sentinel NPC #$index " +
                        "at $position -> $location",
                )

                try {
                    val npc = createSentinelNPC(registry, location, index)
                    if (npc != null) {
                        trackedNPCs[npc.id] = position
                        plugin.logger.info(
                            "$YELLOW[ArenaPlugin] Sentinel NPC spawned " +
                                "successfully: ID=${npc.id}$RESET",
                        )
                    } else {
                        plugin.logger.warning(
                            "$YELLOW[ArenaPlugin] Failed to spawn NPC " +
                                "at $position$RESET",
                        )
                    }
                } catch (e: Exception) {
                    plugin.logger.severe(
                        "$RED[ArenaPlugin] Error spawning NPC: " +
                            "${e.message}$RESET",
                    )
                    e.printStackTrace()
                }
            }
        }

        plugin.logger.info(
            "$YELLOW[ArenaPlugin] NPC spawning complete. " +
                "Tracked NPCs: ${trackedNPCs.size}$RESET",
        )
    }

    private fun calculateNPCLocation(
        world: World,
        position: SpawnPosition,
        baseY: Int,
    ): Location {
        val angleRad = Math.toRadians(position.angleDegrees)
        val x = (Math.cos(angleRad) * NPC_RADIUS).toInt()
        val z = (Math.sin(angleRad) * NPC_RADIUS).toInt()
        return Location(world, x + 0.5, (baseY + 1).toDouble(), z + 0.5)
    }

    private fun createSentinelNPC(
        registry: NPCRegistry,
        location: Location,
        index: Int,
    ): NPC? {
        val npcName = "$NPC_NAME_PREFIX$index"

        val npc = registry.createNPC(EntityType.PLAYER, npcName)
        if (npc == null) {
            plugin.logger.severe(
                "$RED[ArenaPlugin] Failed to create NPC in registry$RESET",
            )
            return null
        }

        npc.spawn(location)

        if (!npc.isSpawned) {
            plugin.logger.severe(
                "$RED[ArenaPlugin] NPC failed to spawn at location$RESET",
            )
            registry.deregister(npc)
            return null
        }

        val sentinel = npc.getOrAddTrait(SentinelTrait::class.java)
        if (sentinel == null) {
            plugin.logger.severe(
                "$RED[ArenaPlugin] Failed to add Sentinel trait to NPC$RESET",
            )
            npc.destroy()
            return null
        }

        configureSentinelNPC(sentinel)

        // Equip NPC with weapon based on attack type
        equipNPCWithWeapon(npc)

        return npc
    }

    private fun equipNPCWithWeapon(npc: NPC) {
        try {
            plugin.logger.info(
                "$YELLOW[ArenaPlugin] Equipping NPC ${npc.id} " +
                    "with attack type: $npcAttackType$RESET",
            )
            when (npcAttackType) {
                NPCAttackType.SPECTRAL_ARROW -> {
                    // Give bow in main hand
                    val equipment = npc.getOrAddTrait(Equipment::class.java)
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.BOW),
                    )
                    plugin.logger.info(
                        "$YELLOW[ArenaPlugin] Equipped NPC ${npc.id} " +
                            "with bow$RESET",
                    )

                    // Give spectral arrows in inventory
                    val inventory = npc.getOrAddTrait(Inventory::class.java)
                    val arrows = ItemStack(Material.SPECTRAL_ARROW, 64)
                    inventory.contents[0] = arrows
                    plugin.logger.info(
                        "$YELLOW[ArenaPlugin] Equipped NPC ${npc.id} " +
                            "with spectral arrows$RESET",
                    )
                }
                NPCAttackType.FIREBALL -> {
                    // Give blaze rod in main hand (shoots fireballs!)
                    val equipment = npc.getOrAddTrait(Equipment::class.java)
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.BLAZE_ROD),
                    )
                    plugin.logger.info(
                        "$YELLOW[ArenaPlugin] Equipped NPC ${npc.id} " +
                            "with blaze rod (fireballs)$RESET",
                    )
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning(
                "$YELLOW[ArenaPlugin] Failed to equip NPC ${npc.id}: " +
                    "${e.message}$RESET",
            )
        }
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
            plugin.logger.info(
                "$YELLOW[ArenaPlugin] NPC ${npc.id} died$RESET",
            )
            trackedNPCs.remove(npc.id)

            if (trackedNPCs.isEmpty()) {
                val nextBatchSize = NPCDecisions.nextBatchSize(currentBatchSize)
                currentBatchSize = nextBatchSize
                npcCount = currentBatchSize
                plugin.logger.info(
                    "$YELLOW[ArenaPlugin] All NPCs dead. " +
                        "Spawning next batch: $currentBatchSize NPCs$RESET",
                )
                val world = entity.world
                spawnArenaNPCs(world, lastSpawnBaseY)
            }
        }
    }

    fun clearAllNPCs() {
        val registry = CitizensAPI.getNPCRegistry()

        trackedNPCs.keys.forEach { npcId ->
            val npc = registry.getById(npcId)
            if (npc != null) {
                npc.destroy()
                plugin.logger.info(
                    "$YELLOW[ArenaPlugin] Removed NPC: $npcId$RESET",
                )
            }
        }

        trackedNPCs.clear()
        plugin.logger.info("$YELLOW[ArenaPlugin] All NPCs cleared$RESET")
    }

    fun getNPCStatus(): String {
        val status = if (npcEnabled) "Enabled" else "Disabled"
        return "NPCs: $status, Count: $npcCount, Health: $npcHealth, " +
            "Damage: $npcDamage, Attack: $npcAttackType"
    }

    fun toggleNPCs() {
        npcEnabled = !npcEnabled
        val status = if (npcEnabled) "enabled" else "disabled"
        plugin.logger.info(
            "$YELLOW[ArenaPlugin] NPCs $status$RESET",
        )
    }

    fun setNPCHealth(health: Double) {
        npcHealth = health.coerceAtLeast(1.0)
        plugin.logger.info(
            "$YELLOW[ArenaPlugin] NPC health set to $npcHealth$RESET",
        )
    }

    fun setNPCDamage(damage: Double) {
        npcDamage = damage.coerceAtLeast(1.0)
        plugin.logger.info(
            "$YELLOW[ArenaPlugin] NPC damage set to $npcDamage$RESET",
        )
    }

    fun setNPCCount(count: Int) {
        npcCount = count.coerceIn(0, MAX_NPCS)
        plugin.logger.info(
            "$YELLOW[ArenaPlugin] NPC count set to $npcCount$RESET",
        )
    }

    fun getNPCCount(): Int = npcCount

    fun getNPCHealth(): Double = npcHealth

    fun getNPCDamage(): Double = npcDamage

    fun isNPCEnabled(): Boolean = npcEnabled

    fun getNPCAttackType(): NPCAttackType = npcAttackType

    fun setNPCAttackType(attackType: NPCAttackType) {
        val oldType = npcAttackType
        npcAttackType = attackType
        plugin.logger.info(
            "$YELLOW[ArenaPlugin] NPC attack type CHANGED from $oldType " +
                "to $npcAttackType$RESET",
        )
    }
}
