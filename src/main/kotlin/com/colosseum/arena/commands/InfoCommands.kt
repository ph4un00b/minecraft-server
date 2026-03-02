package com.colosseum.arena.commands

import com.colosseum.arena.VersionInfo
import com.colosseum.arena.manager.ArenaManager
import org.bukkit.command.CommandSender

/**
 * Handles info-related commands: spawns, version, help
 */
class InfoCommands(
    private val versionInfo: VersionInfo,
    private val arenaManager: ArenaManager,
    private val commandLogger: CommandLogger,
) {
    fun execute(
        cmd: ArenaCommand,
        args: Array<out String>,
        sender: CommandSender,
    ) {
        when (cmd) {
            ArenaCommand.SPAWNS -> handleSpawns(sender, args)
            ArenaCommand.VERSION -> handleVersion(sender, args)
            ArenaCommand.HELP -> handleHelp(sender, args)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    private fun handleSpawns(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        sender.sendMessage("${ArenaCommand.PREFIX}Spawn System:")
        sender.sendMessage("  4 fixed positions at inner edge (radius 12)")
        sender.sendMessage("  East: Gold block marker")
        sender.sendMessage("  South: Diamond block marker")
        sender.sendMessage("  West: Emerald block marker")
        sender.sendMessage("  North: Lapis block marker")
        sender.sendMessage("  Rotation: Clockwise (E → S → W → N)")
        commandLogger.logCommand(sender, ArenaCommand.SPAWNS, args, true)
    }

    private fun handleVersion(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        sender.sendMessage("${ArenaCommand.PREFIX}Arena Plugin v${versionInfo.version}")
        sender.sendMessage("  Built: ${versionInfo.buildTime}")
        sender.sendMessage("  Git: ${versionInfo.gitHash}")
        commandLogger.logCommand(
            sender,
            ArenaCommand.VERSION,
            args,
            true,
            mapOf(
                "version" to versionInfo.version,
                "build_time" to versionInfo.buildTime,
                "git_hash" to versionInfo.gitHash,
            ),
        )
    }

    private fun handleHelp(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        sender.sendMessage("${ArenaCommand.PREFIX}Available commands:")
        ArenaCommand.generateHelpText().forEach { line ->
            sender.sendMessage(line)
        }
        commandLogger.logCommand(sender, ArenaCommand.HELP, args, true)
    }
}
