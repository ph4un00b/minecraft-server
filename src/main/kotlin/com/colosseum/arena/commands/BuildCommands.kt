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
    private val world: World
) {
    fun execute(cmd: ArenaCommand, args: Array<out String>, sender: CommandSender) {
        when (cmd) {
            ArenaCommand.SIMPLE -> handleSimple(sender)
            ArenaCommand.DETAILED -> handleDetailed(sender)
            ArenaCommand.REBUILD -> handleRebuild(sender)
            ArenaCommand.SET_Y -> handleSetY(sender, args)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    private fun handleSimple(sender: CommandSender) {
        sender.sendMessage("${ArenaCommand.PREFIX}Building simple arena with spawn markers...")
        arenaManager.rebuild(world, ArenaType.SIMPLE)
        sender.sendMessage("${ArenaCommand.PREFIX}Simple arena built! Spawns at E/S/W/N inner edge")
    }

    private fun handleDetailed(sender: CommandSender) {
        sender.sendMessage("${ArenaCommand.PREFIX}Building detailed gothic arena with spawn markers...")
        arenaManager.rebuild(world, ArenaType.DETAILED)
        sender.sendMessage("${ArenaCommand.PREFIX}Detailed arena built! Spawns at E/S/W/N inner edge")
    }

    private fun handleRebuild(sender: CommandSender) {
        val currentType = arenaManager.getCurrentType()
        sender.sendMessage("${ArenaCommand.PREFIX}Rebuilding arena (type: ${currentType.name.lowercase()})...")
        arenaManager.rebuild(world, currentType)
        sender.sendMessage("${ArenaCommand.PREFIX}Arena rebuilt! Spawn markers restored.")
    }

    private fun handleSetY(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(ArenaCommand.SET_Y)}")
            sender.sendMessage("${ArenaCommand.PREFIX}Current Y level: ${arenaManager.getCurrentBaseY()}")
            return
        }
        val newY = args[1].toIntOrNull()
        if (newY == null || newY < 0 || newY > 255) {
            sender.sendMessage("${ArenaCommand.PREFIX}Error: Y level must be between 0 and 255")
            return
        }
        val oldY = arenaManager.getCurrentBaseY()
        sender.sendMessage("${ArenaCommand.PREFIX}Changing arena base Y from $oldY to $newY...")
        arenaManager.changeYLevel(world, newY, arenaManager.getCurrentType())
        sender.sendMessage("${ArenaCommand.PREFIX}Arena rebuilt at Y=$newY! Spawn markers updated.")
    }
}
