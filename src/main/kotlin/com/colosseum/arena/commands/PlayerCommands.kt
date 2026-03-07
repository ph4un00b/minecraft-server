package com.colosseum.arena.commands

import com.colosseum.arena.ArenaManager
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Handles player-related commands: restock, arrows
 */
class PlayerCommands(
    private val arenaManager: ArenaManager,
    private val commandLogger: CommandLogger,
) {
    fun execute(
        cmd: ArenaCommand,
        args: Array<out String>,
        sender: CommandSender,
    ) {
        when (cmd) {
            ArenaCommand.RESTOCK -> handleRestock(sender, args)
            ArenaCommand.ARROWS -> handleArrows(sender, args)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    private fun handleRestock(sender: CommandSender, args: Array<out String>) {
        val targetPlayer: Player =
            if (args.size >= 2) {
                Bukkit.getPlayer(args[1]) ?: run {
                    val playerName = args[1]
                    sender.sendMessage(
                        "${ArenaCommand.PREFIX}Error: Player '$playerName' " +
                            "not found or not online",
                    )
                    commandLogger.logCommand(
                        sender,
                        ArenaCommand.RESTOCK,
                        args,
                        false,
                        mapOf(
                            "reason" to "player_not_found",
                            "target" to args[1],
                        ),
                    )
                    return
                }
            } else {
                if (sender is Player) {
                    sender
                } else {
                    val usage = ArenaCommand.generateCommandUsage(
                        ArenaCommand.RESTOCK,
                    )
                    sender.sendMessage(
                        "${ArenaCommand.PREFIX}$usage",
                    )
                    commandLogger.logCommand(
                        sender,
                        ArenaCommand.RESTOCK,
                        args,
                        false,
                        mapOf("reason" to "console_without_target"),
                    )
                    return
                }
            }

        val success = arenaManager.restockPlayer(targetPlayer)
        if (success) {
            val name = targetPlayer.name
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Restocked $name with 5 arrows " +
                    "and repaired bow",
            )
            targetPlayer.sendMessage(
                "${ArenaCommand.PREFIX}You have been restocked! " +
                    "Bow repaired, +5 arrows (max 10)",
            )
            val details =
                mutableMapOf("target" to targetPlayer.name, "success" to "true")
            if (sender != targetPlayer) {
                details["executed_by"] = sender.name
            }
            commandLogger.logCommand(
                sender,
                ArenaCommand.RESTOCK,
                args,
                true,
                details,
            )
        } else {
            val name = targetPlayer.name
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Error: $name does not have a bow",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.RESTOCK,
                args,
                false,
                mapOf("target" to targetPlayer.name, "reason" to "no_bow"),
            )
        }
    }

    private fun handleArrows(sender: CommandSender, args: Array<out String>) {
        val currentTracker = arenaManager.arrowTracker
        sender.sendMessage("${ArenaCommand.PREFIX}Arrow Status:")
        sender.sendMessage(
            "  Tracked arrows: ${currentTracker.getArrowCount()}",
        )
        sender.sendMessage(
            "  Max allowed: ${currentTracker.getMaxAllowed()} (5 per player)",
        )
        sender.sendMessage(
            "  Online players: ${Bukkit.getOnlinePlayers().size}",
        )
        sender.sendMessage("  Arrows persist until picked up")
        commandLogger.logCommand(
            sender,
            ArenaCommand.ARROWS,
            args,
            true,
            mapOf(
                "arrow_count" to currentTracker.getArrowCount().toString(),
                "max_allowed" to currentTracker.getMaxAllowed().toString(),
                "online_players" to Bukkit.getOnlinePlayers().size.toString(),
            ),
        )
    }
}
