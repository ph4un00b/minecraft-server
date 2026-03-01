package com.colosseum.arena.commands

import com.colosseum.arena.manager.ArenaManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Handles player-related commands: restock, arrows
 */
class PlayerCommands(
    private val arenaManager: ArenaManager
) {
    fun execute(cmd: ArenaCommand, args: Array<out String>, sender: CommandSender) {
        when (cmd) {
            ArenaCommand.RESTOCK -> handleRestock(sender, args)
            ArenaCommand.ARROWS -> handleArrows(sender)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    private fun handleRestock(sender: CommandSender, args: Array<out String>) {
        val targetPlayer: Player = if (args.size >= 2) {
            Bukkit.getPlayer(args[1]) ?: run {
                sender.sendMessage("${ArenaCommand.PREFIX}Error: Player '${args[1]}' not found or not online")
                return
            }
        } else {
            if (sender is Player) {
                sender
            } else {
                sender.sendMessage("${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(ArenaCommand.RESTOCK)}")
                return
            }
        }

        val success = arenaManager.restockPlayer(targetPlayer)
        if (success) {
            sender.sendMessage("${ArenaCommand.PREFIX}Restocked ${targetPlayer.name} with 5 arrows and repaired bow")
            targetPlayer.sendMessage("${ArenaCommand.PREFIX}You have been restocked! Bow repaired, +5 arrows (max 10)")
        } else {
            sender.sendMessage("${ArenaCommand.PREFIX}Error: ${targetPlayer.name} does not have a bow")
        }
    }

    private fun handleArrows(sender: CommandSender) {
        val currentTracker = arenaManager.arrowTracker
        sender.sendMessage("${ArenaCommand.PREFIX}Arrow Status:")
        sender.sendMessage("  Tracked arrows: ${currentTracker.getArrowCount()}")
        sender.sendMessage("  Max allowed: ${currentTracker.getMaxAllowed()} (5 per player)")
        sender.sendMessage("  Online players: ${Bukkit.getOnlinePlayers().size}")
        sender.sendMessage("  Arrows persist until picked up")
    }
}
