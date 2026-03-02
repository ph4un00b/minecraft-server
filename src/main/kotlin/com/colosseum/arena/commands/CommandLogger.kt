package com.colosseum.arena.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Logs all arena commands to a persistent log file
 * Records detailed information including sender, command, location, and outcome
 */
class CommandLogger(pluginDataFolder: File) {

    private val logFile: File
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        // Create plugin data folder if it doesn't exist
        if (!pluginDataFolder.exists()) {
            pluginDataFolder.mkdirs()
        }
        logFile = File(pluginDataFolder, "commands.log")
    }

    /**
     * Log a command execution with full details
     */
    fun logCommand(
        sender: CommandSender,
        command: ArenaCommand,
        args: Array<out String>,
        success: Boolean,
        details: Map<String, String> = emptyMap()
    ) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val senderName = sender.name
        val senderType = if (sender is Player) "Player" else "Console"

        // Build location info if player
        val locationInfo = if (sender is Player) {
            val loc = sender.location
            "world=${loc.world?.name},x=${loc.blockX},y=${loc.blockY},z=${loc.blockZ}"
        } else {
            "console"
        }

        // Build details string
        val detailsStr = if (details.isNotEmpty()) {
            details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        } else {
            ""
        }

        // Build log entry
        val logEntry = buildString {
            append("[$timestamp]")
            append(" [$senderType:$senderName]")
            append(" /arena ${command.primaryName}")
            if (args.size > 1) {
                append(" ${args.drop(1).joinToString(" ")}")
            }
            append(" | Location: $locationInfo")
            append(" | Success: $success")
            if (detailsStr.isNotEmpty()) {
                append(" | $detailsStr")
            }
        }

        // Write to file (append mode)
        FileWriter(logFile, true).use { writer ->
            writer.write(logEntry)
            writer.write(System.lineSeparator())
        }
    }

    /**
     * Get the log file for reading/debugging
     */
    fun getLogFile(): File = logFile
}
