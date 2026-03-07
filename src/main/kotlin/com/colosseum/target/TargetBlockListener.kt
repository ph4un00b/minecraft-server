package com.colosseum.target

import com.colosseum.npc.NPCManager
import com.colosseum.target.config.TargetBlockConfig
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class TargetBlockListener(
    private val plugin: JavaPlugin,
    private val npcManager: NPCManager,
    private val config: TargetBlockConfig = TargetBlockConfig(),
) : Listener {
    companion object {
        private const val YELLOW = "\u001B[33m"
        private const val RESET = "\u001B[0m"
        private const val GREEN = "\u001B[32m"
        private const val DELAY_SECONDS = 5L
    }

    private var targetActivated = false
    private var lastSpawnWorld: World? = null
    private var lastSpawnBaseY: Int = 0
    private var activatingPlayer: Player? = null

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun setWorldInfo(world: World, baseY: Int) {
        lastSpawnWorld = world
        lastSpawnBaseY = baseY
        targetActivated = false
        activatingPlayer = null
    }

    fun reset() {
        targetActivated = false
        activatingPlayer = null
        val msg = "Target block reset - ready for next batch"
        plugin.logger.info("$YELLOW[ArenaPlugin] $msg$RESET")
    }

    fun isActivated(): Boolean = targetActivated

    /**
     * Recreate the target block after a delay
     * Called when all NPCs in a batch are killed
     */
    fun recreateTargetAfterDelay() {
        val world = lastSpawnWorld ?: return
        val baseY = lastSpawnBaseY

        plugin.logger.info(
            "$YELLOW[ArenaPlugin] Recreating target block in " +
                "$DELAY_SECONDS seconds...$RESET",
        )

        object : BukkitRunnable() {
            override fun run() {
                val targetLocation = Location(
                    world,
                    config.centerX.toDouble(),
                    (baseY + config.offsetY).toDouble(),
                    config.centerZ.toDouble(),
                )
                targetLocation.block.type = config.material

                // Reset the activated flag so it can be hit again
                reset()

                plugin.logger.info(
                    "$GREEN[ArenaPlugin] Target block recreated! " +
                        "Hit it to spawn next batch.$RESET",
                )
            }
        }.runTaskLater(plugin, DELAY_SECONDS * 20L) // 20 ticks = 1 second
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        val hitBlock = event.hitBlock ?: return

        // Only handle arrows hitting configured target material
        if (projectile !is Arrow) return
        if (hitBlock.type != config.material) return

        // Check if it's our arena target at configured position
        val world = lastSpawnWorld ?: return
        if (hitBlock.world.name != world.name) return
        if (hitBlock.x != config.centerX || hitBlock.z != config.centerZ) return

        // Prevent multiple activations
        if (targetActivated) {
            return
        }

        val shooter = projectile.shooter
        if (shooter !is Player) return

        // Activate the target
        targetActivated = true
        activatingPlayer = shooter

        plugin.logger.info(
            "$GREEN[ArenaPlugin] Target hit by ${shooter.name}! " +
                "${config.activationMessage}$RESET",
        )

        // Play sound and effects
        playActivationEffects(hitBlock.location)

        // Destroy the target block
        hitBlock.type = Material.AIR

        // Spawn NPCs for this batch
        npcManager.spawnArenaNPCs(world, lastSpawnBaseY, shooter)

        // Remove the arrow
        projectile.remove()
    }

    private fun playActivationEffects(location: Location) {
        val world = location.world ?: return

        // Play configured sound
        world.playSound(
            location,
            config.soundEffect,
            config.soundVolume,
            config.soundPitch,
        )

        // Spawn particles based on config
        object : BukkitRunnable() {
            private var ticks = 0L
            override fun run() {
                if (ticks >= config.activationDuration) {
                    cancel()
                    return
                }

                world.spawnParticle(
                    config.particleType,
                    location.clone().add(0.5, 0.5, 0.5),
                    config.particleCount,
                    config.particleSpread,
                    config.particleSpread,
                    config.particleSpread,
                    config.particleSpeed,
                )

                ticks += config.tickInterval
            }
        }.runTaskTimer(plugin, 0L, config.tickInterval)
    }
}
