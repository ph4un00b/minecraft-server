package com.colosseum.arena.commands

import com.colosseum.arena.VersionInfo
import com.colosseum.arena.manager.ArenaManager
import org.bukkit.command.CommandSender

/**
 * Handles info-related commands: spawns, version, help
 */
class InfoCommands(
    private val versionInfo: VersionInfo,
    private val arenaManager: ArenaManager
) {
    fun execute(cmd: ArenaCommand, sender: CommandSender) {
        when (cmd) {
            ArenaCommand.SPAWNS -> handleSpawns(sender)
            ArenaCommand.VERSION -> handleVersion(sender)
            ArenaCommand.HELP -> handleHelp(sender)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    private fun handleSpawns(sender: CommandSender) {
        sender.sendMessage("${ArenaCommand.PREFIX}Spawn System:")
        sender.sendMessage("  4 fixed positions at inner edge (radius 12)")
        sender.sendMessage("  East: Gold block marker")
        sender.sendMessage("  South: Diamond block marker")
        sender.sendMessage("  West: Emerald block marker")
        sender.sendMessage("  North: Lapis block marker")
        sender.sendMessage("  Rotation: Clockwise (E → S → W → N)")
    }

    private fun handleVersion(sender: CommandSender) {
        sender.sendMessage("${ArenaCommand.PREFIX}Arena Plugin v${versionInfo.version}")
        sender.sendMessage("  Built: ${versionInfo.buildTime}")
        sender.sendMessage("  Git: ${versionInfo.gitHash}")
    }

    private fun handleHelp(sender: CommandSender) {
        sender.sendMessage("${ArenaCommand.PREFIX}Available commands:")
        ArenaCommand.generateHelpText().forEach { line ->
            sender.sendMessage(line)
        }
    }
}
