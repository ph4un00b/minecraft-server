package com.colosseum.arena.commands

import com.colosseum.arena.domain.ArenaType
import com.colosseum.arena.manager.ArenaManager
import org.bukkit.World
import org.bukkit.command.CommandSender

/**
 * Handles build-related commands: simple, detailed, rebuild, sety
 */
class BuildCommands(
    private val arenaManager: ArenaManager,
    private val world: World,
    private val commandLogger: CommandLogger
) {
    fun execute(cmd: ArenaCommand, args: Array<out String>, sender: CommandSender) {
        when (cmd) {
            ArenaCommand.SIMPLE -> handleSimple(sender, args)
            ArenaCommand.DETAILED -> handleDetailed(sender, args)
            ArenaCommand.REBUILD -> handleRebuild(sender, args)
            ArenaCommand.SET_Y -> handleSetY(sender, args)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    private fun handleSimple(sender: CommandSender, args: Array<out String>) {
        sender.sendMessage("${ArenaCommand.PREFIX}Building simple arena with spawn markers...")
        try {
            arenaManager.rebuild(world, ArenaType.SIMPLE)
            sender.sendMessage("${ArenaCommand.PREFIX}Simple arena built! Spawns at E/S/W/N inner edge")
            commandLogger.logCommand(sender, ArenaCommand.SIMPLE, args, true, mapOf("type" to "simple"))
        } catch (e: Exception) {
            commandLogger.logCommand(sender, ArenaCommand.SIMPLE, args, false, mapOf("error" to e.message.orEmpty()))
            throw e
        }
    }

    private fun handleDetailed(sender: CommandSender, args: Array<out String>) {
        sender.sendMessage("${ArenaCommand.PREFIX}Building detailed gothic arena with spawn markers...")
        try {
            arenaManager.rebuild(world, ArenaType.DETAILED)
            sender.sendMessage("${ArenaCommand.PREFIX}Detailed arena built! Spawns at E/S/W/N inner edge")
            commandLogger.logCommand(sender, ArenaCommand.DETAILED, args, true, mapOf("type" to "detailed"))
        } catch (e: Exception) {
            commandLogger.logCommand(sender, ArenaCommand.DETAILED, args, false, mapOf("error" to e.message.orEmpty()))
            throw e
        }
    }

    private fun handleRebuild(sender: CommandSender, args: Array<out String>) {
        val currentType = arenaManager.getCurrentType()
        sender.sendMessage("${ArenaCommand.PREFIX}Rebuilding arena (type: ${currentType.name.lowercase()})...")
        try {
            arenaManager.rebuild(world, currentType)
            sender.sendMessage("${ArenaCommand.PREFIX}Arena rebuilt! Spawn markers restored.")
            commandLogger.logCommand(sender, ArenaCommand.REBUILD, args, true, mapOf("type" to currentType.name.lowercase()))
        } catch (e: Exception) {
            commandLogger.logCommand(sender, ArenaCommand.REBUILD, args, false, mapOf("error" to e.message.orEmpty()))
            throw e
        }
    }

    private fun handleSetY(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(ArenaCommand.SET_Y)}")
            sender.sendMessage("${ArenaCommand.PREFIX}Current Y level: ${arenaManager.getCurrentBaseY()}")
            commandLogger.logCommand(sender, ArenaCommand.SET_Y, args, false, mapOf("reason" to "missing_y_argument"))
            return
        }
        val newY = args[1].toIntOrNull()
        if (newY == null || newY < 0 || newY > 255) {
            sender.sendMessage("${ArenaCommand.PREFIX}Error: Y level must be between 0 and 255")
            commandLogger.logCommand(sender, ArenaCommand.SET_Y, args, false, mapOf("reason" to "invalid_y_value", "input" to args[1]))
            return
        }
        val oldY = arenaManager.getCurrentBaseY()
        sender.sendMessage("${ArenaCommand.PREFIX}Changing arena base Y from $oldY to $newY...")
        try {
            arenaManager.changeYLevel(world, newY, arenaManager.getCurrentType())
            sender.sendMessage("${ArenaCommand.PREFIX}Arena rebuilt at Y=$newY! Spawn markers updated.")
            commandLogger.logCommand(sender, ArenaCommand.SET_Y, args, true, mapOf("old_y" to oldY.toString(), "new_y" to newY.toString()))
        } catch (e: Exception) {
            commandLogger.logCommand(sender, ArenaCommand.SET_Y, args, false, mapOf("error" to e.message.orEmpty(), "old_y" to oldY.toString(), "attempted_y" to newY.toString()))
            throw e
        }
    }
}
