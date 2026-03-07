package com.colosseum.arena

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
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
) : Listener {
    companion object {
        private const val TARGET_X = 0
        private const val TARGET_Z = 0
        private const val YELLOW = "\u001B[33m"
        private const val RESET = "\u001B[0m"
        private const val GREEN = "\u001B[32m"
    }

    private var targetActivated = false
    private var lastSpawnWorld: World? = null
    private var lastSpawnBaseY: Int = 0

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun setWorldInfo(world: World, baseY: Int) {
        lastSpawnWorld = world
        lastSpawnBaseY = baseY
        targetActivated = false
    }

    fun reset() {
        targetActivated = false
        val msg = "[ArenaPlugin] Target block reset - NPCs are now passive"
        plugin.logger.info("$YELLOW$msg$RESET")
    }

    fun isActivated(): Boolean = targetActivated

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val projectile = event.entity
        val hitBlock = event.hitBlock ?: return

        // Only handle arrows hitting target blocks
        if (projectile !is Arrow) return
        if (hitBlock.type != Material.TARGET) return

        // Check if it's our arena target
        val world = lastSpawnWorld ?: return
        if (hitBlock.world.name != world.name) return
        if (hitBlock.x != TARGET_X || hitBlock.z != TARGET_Z) return

        // Prevent multiple activations
        if (targetActivated) {
            return
        }

        val shooter = projectile.shooter
        if (shooter !is Player) return

        // Activate the target
        targetActivated = true

        plugin.logger.info(
            "$GREEN[ArenaPlugin] Target hit by ${shooter.name}! " +
                "NPCs are now HOSTILE!$RESET",
        )

        // Play sound and effects
        playActivationEffects(hitBlock.location)

        // Make NPCs hostile
        npcManager.activateHostility(shooter)

        // Remove the arrow
        projectile.remove()
    }

    private fun playActivationEffects(location: Location) {
        val world = location.world ?: return

        // Play dragon growl sound
        world.playSound(
            location,
            Sound.ENTITY_ENDER_DRAGON_GROWL,
            1.0f,
            1.0f,
        )

        // Spawn particles
        object : BukkitRunnable() {
            private var ticks = 0
            override fun run() {
                if (ticks >= 20) {
                    cancel()
                    return
                }

                world.spawnParticle(
                    org.bukkit.Particle.FLAME,
                    location.clone().add(0.5, 0.5, 0.5),
                    10,
                    0.5,
                    0.5,
                    0.5,
                    0.1,
                )

                ticks++
            }
        }.runTaskTimer(plugin, 0L, 2L)
    }
}
