package com.colosseum.target.config

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound

/**
 * Target block configuration
 * Central source of truth for target block behavior and effects
 */
data class TargetBlockConfig(
    val material: Material = Material.TARGET,
    val centerX: Int = 0,
    val centerZ: Int = 0,
    val offsetY: Int = 2,
    val particleType: Particle = Particle.FLAME,
    val particleCount: Int = 10,
    val particleSpread: Double = 0.5,
    val particleSpeed: Double = 0.1,
    val soundEffect: Sound = Sound.BLOCK_NOTE_BLOCK_PLING,
    val soundVolume: Float = 1.0f,
    val soundPitch: Float = 1.5f,
    val activationMessage: String = "Target hit! NPCs are now HOSTILE!",
    // 2 seconds (20 ticks per second)
    val activationDuration: Long = 40L,
    val tickInterval: Long = 2L,
    // Target destroy/recreate settings
    val destroyOnHit: Boolean = true,
    val recreateDelaySeconds: Long = 5L,
) {
    companion object {
        const val DEFAULT_DELAY_SECONDS = 5L
    }

    init {
        require(recreateDelaySeconds >= 0) {
            "Recreate delay must be non-negative"
        }
    }
}
