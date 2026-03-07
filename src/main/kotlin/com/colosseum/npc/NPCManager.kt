package com.colosseum.npc

import com.colosseum.arena.domain.SpawnPosition
import com.colosseum.combat.spawn.PlayerSpawner
import com.colosseum.npc.config.BatchConfig
import com.colosseum.target.TargetBlockListener
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
import org.bukkit.entity.Player
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
    private val batchConfig: BatchConfig = BatchConfig(),
) : Listener {
    companion object {
        private const val NPC_RADIUS = 6
        private const val DEFAULT_HEALTH = 1.0
        private const val DEFAULT_DAMAGE = 5.0
        private const val NPC_NAME_PREFIX = "ArenaGladiator_"
        private const val YELLOW = "\u001B[33m"
        private const val RESET = "\u001B[0m"
        private const val RED = "\u001B[31m"
    }

    private val trackedNPCs =
        ConcurrentHashMap<Int, Pair<SpawnPosition, NPCAttackType>>()
    private var npcHealth = DEFAULT_HEALTH
    private var npcDamage = DEFAULT_DAMAGE
    private var npcEnabled = true
    private var npcAttackType = NPCAttackType.SWORD
    private var currentBatchSize = batchConfig.startingBatchSize
    private var lastSpawnWorld: World? = null
    private var lastSpawnBaseY: Int = 0
    private var targetBlockListener: TargetBlockListener? = null

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun setTargetBlockListener(listener: TargetBlockListener) {
        targetBlockListener = listener
    }

    fun spawnArenaNPCs(world: World, baseY: Int, targetPlayer: Player) {
        val logMsg = "$YELLOW[ArenaPlugin] Spawning with random types$RESET"
        plugin.logger.info(logMsg)
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
            "$YELLOW[ArenaPlugin] Spawning $currentBatchSize Sentinel NPCs " +
                "at baseY=$baseY$RESET",
        )

        lastSpawnWorld = world
        lastSpawnBaseY = baseY

        val attackTypes =
            NPCDecisions.selectRandomTypesForBatch(currentBatchSize)

        clearAllNPCs()

        val registry = CitizensAPI.getNPCRegistry()

        SpawnPosition.getAll().forEachIndexed { index, position ->
            if (index < currentBatchSize) {
                val attackType = attackTypes[index]
                val location = calculateNPCLocation(world, position, baseY)
                val probPct = (attackType.probability * 100).toInt()
                plugin.logger.info(
                    "$YELLOW[ArenaPlugin] Spawning Sentinel NPC #$index " +
                        "($attackType $probPct%) at $position -> $location",
                )

                try {
                    val npc = createSentinelNPC(
                        registry,
                        location,
                        index,
                        attackType,
                        targetPlayer,
                    )
                    if (npc != null) {
                        trackedNPCs[npc.id] = position to attackType
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
        attackType: NPCAttackType,
        targetPlayer: Player,
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

        configureSentinelNPC(sentinel, targetPlayer)

        equipNPCWithWeapon(npc, attackType)

        return npc
    }

    private fun equipNPCWithWeapon(npc: NPC, attackType: NPCAttackType) {
        try {
            plugin.logger.info(
                "$YELLOW[ArenaPlugin] Equipping NPC ${npc.id} " +
                    "with attack type: $attackType$RESET",
            )
            val equipment = npc.getOrAddTrait(Equipment::class.java)
            when (attackType) {
                NPCAttackType.SWORD -> {
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.IRON_SWORD),
                    )
                }
                NPCAttackType.AXE -> {
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.IRON_AXE),
                    )
                }
                NPCAttackType.TRIDENT -> {
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.TRIDENT),
                    )
                }
                NPCAttackType.BOW -> {
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.BOW),
                    )
                    val inventory = npc.getOrAddTrait(Inventory::class.java)
                    val arrows = ItemStack(Material.SPECTRAL_ARROW, 64)
                    inventory.contents[0] = arrows
                }
                NPCAttackType.CROSSBOW -> {
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.CROSSBOW),
                    )
                }
                NPCAttackType.SHIELD_SWORD -> {
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.IRON_SWORD),
                    )
                    equipment.set(
                        Equipment.EquipmentSlot.OFF_HAND,
                        ItemStack(Material.SHIELD),
                    )
                }
                NPCAttackType.POLEARM -> {
                    equipment.set(
                        Equipment.EquipmentSlot.HAND,
                        ItemStack(Material.TRIDENT),
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

    private fun configureSentinelNPC(
        sentinel: SentinelTrait,
        targetPlayer: Player,
    ) {
        sentinel.health = npcHealth
        sentinel.damage = npcDamage

        // Add player as target if configured to spawn hostile
        if (batchConfig.spawnHostile) {
            sentinel.addTarget("PLAYER:${targetPlayer.name}")
        }

        // Set to melee combat
        sentinel.attackRate = 10
        sentinel.range = 16.0

        // Store spawn point but don't respawn automatically
        sentinel.spawnPoint = sentinel.npc?.storedLocation
        sentinel.respawnTime = -1
    }

    fun activateHostility(activatingPlayer: Player) {
        val registry = CitizensAPI.getNPCRegistry()

        trackedNPCs.keys.forEach { npcId ->
            val npc = registry.getById(npcId)
            if (npc != null) {
                val sentinel = npc.getOrAddTrait(SentinelTrait::class.java)
                // Add player as specific target using PLAYER: prefix
                sentinel.addTarget("PLAYER:${activatingPlayer.name}")
                plugin.logger.info(
                    "$YELLOW[ArenaPlugin] NPC $npcId is now targeting " +
                        "${activatingPlayer.name}$RESET",
                )
            }
        }

        val count = trackedNPCs.size
        plugin.logger.info(
            "$YELLOW[ArenaPlugin] All NPCs activated! Count: $count$RESET",
        )
    }

    fun isHostile(): Boolean = batchConfig.spawnHostile

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
                val nextBatchSize = batchConfig.nextBatchSize(currentBatchSize)
                currentBatchSize = nextBatchSize
                plugin.logger.info(
                    "$YELLOW[ArenaPlugin] All NPCs dead. " +
                        "Next batch will be: $currentBatchSize NPCs$RESET",
                )
                // Recreate target block after delay so player can spawn next batch
                targetBlockListener?.recreateTargetAfterDelay()
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
        return "NPCs: $status, Count: $currentBatchSize, " +
            "Health: $npcHealth, Damage: $npcDamage, " +
            "Attack: $npcAttackType"
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
        currentBatchSize = count.coerceIn(0, batchConfig.maxBatchSize)
        plugin.logger.info(
            "$YELLOW[ArenaPlugin] NPC count set to $currentBatchSize$RESET",
        )
    }

    fun getNPCCount(): Int = currentBatchSize

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
