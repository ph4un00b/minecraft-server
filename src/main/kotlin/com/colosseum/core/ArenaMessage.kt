package com.colosseum.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import java.time.Duration

/**
 * Sealed class for arena-wide messages
 * Supports broadcasting to all players in a world
 */
sealed class ArenaMessage {
    /**
     * Display this message to a specific player
     */
    abstract fun display(player: Player)

    /**
     * Broadcast this message to all players in the specified world
     */
    fun broadcast(world: World) {
        world.players.forEach { display(it) }
    }

    /**
     * Broadcast this message to all online players
     */
    fun broadcastToAll() {
        Bukkit.getOnlinePlayers().forEach { display(it) }
    }

    /**
     * Displayed when a batch of NPCs is cleared
     * Shows title screen with batch number and next batch size
     */
    data class BatchCleared(
        val batchNumber: Int,
        val nextBatchSize: Int,
    ) : ArenaMessage() {
        override fun display(player: Player) {
            player.showTitle(
                Title.title(
                    Component.text("BATCH #$batchNumber CLEARED!"),
                    Component.text(
                        "Prepare for $nextBatchSize enemies...",
                    ),
                    Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(3500),
                        Duration.ofMillis(500),
                    ),
                ),
            )
        }
    }

    /**
     * Displayed when target block is hit
     * Shows action bar message to all players
     */
    data class TargetActivated(
        val message: String = "Target hit! NPCs are now HOSTILE!",
    ) : ArenaMessage() {
        override fun display(player: Player) {
            player.sendActionBar(Component.text(message))
        }
    }
}
