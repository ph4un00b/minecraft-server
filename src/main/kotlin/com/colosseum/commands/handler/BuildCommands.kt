package com.colosseum.commands.handler

import com.colosseum.arena.ArenaManager
import com.colosseum.arena.domain.ArenaType
import com.colosseum.commands.infrastructure.ArenaCommand
import com.colosseum.commands.infrastructure.CommandLogger
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

/**
 * Handles build-related commands: simple, detailed, rebuild, sety
 * Supports both sync (fast) and async (lag-free) building modes
 */
class BuildCommands(
    private val arenaManager: ArenaManager,
    private val world: World,
    private val commandLogger: CommandLogger,
    private val plugin: JavaPlugin,
) {
    fun execute(
        cmd: ArenaCommand,
        args: Array<out String>,
        sender: CommandSender,
        forceSync: Boolean = false,
    ) {
        when (cmd) {
            ArenaCommand.SIMPLE -> {
                if (forceSync) {
                    handleSimpleSync(sender, args)
                } else {
                    handleSimpleAsync(sender, args)
                }
            }
            ArenaCommand.DETAILED -> {
                if (forceSync) {
                    handleDetailedSync(sender, args)
                } else {
                    handleDetailedAsync(sender, args)
                }
            }
            ArenaCommand.REBUILD -> {
                if (forceSync) {
                    handleRebuildSync(sender, args)
                } else {
                    handleRebuildAsync(sender, args)
                }
            }
            ArenaCommand.SET_Y -> handleSetY(sender, args)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    /**
     * Check if a build is currently in progress
     */
    fun isBuilding(): Boolean = arenaManager.isBuilding()

    private fun handleSimpleSync(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        sender.sendMessage("${ArenaCommand.PREFIX}Building simple arena...")
        try {
            arenaManager.rebuild(world, ArenaType.SIMPLE)
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Simple arena built! " +
                    "Spawns at E/S/W/N inner edge",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SIMPLE,
                args,
                true,
                mapOf("type" to "simple", "mode" to "sync"),
            )
        } catch (e: Exception) {
            commandLogger.logCommand(
                sender,
                ArenaCommand.SIMPLE,
                args,
                false,
                mapOf("error" to e.message.orEmpty(), "mode" to "sync"),
            )
            throw e
        }
    }

    private fun handleSimpleAsync(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        if (arenaManager.isBuilding()) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Another build is already in progress. " +
                    "Please wait.",
            )
            return
        }

        sender.sendMessage(
            "${ArenaCommand.PREFIX}Starting async simple arena build...",
        )
        commandLogger.logCommand(
            sender,
            ArenaCommand.SIMPLE,
            args,
            true,
            mapOf("type" to "simple", "mode" to "async", "status" to "started"),
        )

        try {
            arenaManager.rebuildAsync(
                world = world,
                type = ArenaType.SIMPLE,
                onProgress = { placed, total, percentage ->
                    if (percentage % 25 == 0) {
                        sender.sendMessage(
                            "${ArenaCommand.PREFIX}Building... " +
                                "$percentage% ($placed/$total blocks)",
                        )
                    }
                },
                onComplete = {
                    sender.sendMessage(
                        "${ArenaCommand.PREFIX}Simple arena built! " +
                            "Spawns at E/S/W/N inner edge",
                    )
                    commandLogger.logCommand(
                        sender,
                        ArenaCommand.SIMPLE,
                        args,
                        true,
                        mapOf(
                            "type" to "simple",
                            "mode" to "async",
                            "status" to "completed",
                        ),
                    )
                },
            )
        } catch (e: Exception) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Error during build: ${e.message}",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SIMPLE,
                args,
                false,
                mapOf("error" to e.message.orEmpty(), "mode" to "async"),
            )
        }
    }

    private fun handleDetailedSync(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        sender.sendMessage(
            "${ArenaCommand.PREFIX}Building detailed gothic arena...",
        )
        try {
            arenaManager.rebuild(world, ArenaType.DETAILED)
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Detailed arena built! " +
                    "Spawns at E/S/W/N inner edge",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.DETAILED,
                args,
                true,
                mapOf("type" to "detailed", "mode" to "sync"),
            )
        } catch (e: Exception) {
            commandLogger.logCommand(
                sender,
                ArenaCommand.DETAILED,
                args,
                false,
                mapOf("error" to e.message.orEmpty(), "mode" to "sync"),
            )
            throw e
        }
    }

    private fun handleDetailedAsync(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        if (arenaManager.isBuilding()) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Another build is already in progress. " +
                    "Please wait.",
            )
            return
        }

        sender.sendMessage(
            "${ArenaCommand.PREFIX}Starting async detailed arena build...",
        )
        commandLogger.logCommand(
            sender,
            ArenaCommand.DETAILED,
            args,
            true,
            mapOf(
                "type" to "detailed",
                "mode" to "async",
                "status" to "started",
            ),
        )

        try {
            arenaManager.rebuildAsync(
                world = world,
                type = ArenaType.DETAILED,
                onProgress = { placed, total, percentage ->
                    if (percentage % 10 == 0) {
                        sender.sendMessage(
                            "${ArenaCommand.PREFIX}Building... " +
                                "$percentage% ($placed/$total blocks)",
                        )
                    }
                },
                onComplete = {
                    sender.sendMessage(
                        "${ArenaCommand.PREFIX}Detailed arena built! " +
                            "Spawns at E/S/W/N inner edge",
                    )
                    commandLogger.logCommand(
                        sender,
                        ArenaCommand.DETAILED,
                        args,
                        true,
                        mapOf(
                            "type" to "detailed",
                            "mode" to "async",
                            "status" to "completed",
                        ),
                    )
                },
            )
        } catch (e: Exception) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Error during build: ${e.message}",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.DETAILED,
                args,
                false,
                mapOf("error" to e.message.orEmpty(), "mode" to "async"),
            )
        }
    }

    private fun handleRebuildSync(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        val currentType = arenaManager.getCurrentType()
        val typeName = currentType.name.lowercase()
        sender.sendMessage(
            "${ArenaCommand.PREFIX}Rebuilding arena (type: $typeName)...",
        )
        try {
            arenaManager.rebuild(world, currentType)
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Arena rebuilt! Spawn markers restored.",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.REBUILD,
                args,
                true,
                mapOf("type" to currentType.name.lowercase(), "mode" to "sync"),
            )
        } catch (e: Exception) {
            commandLogger.logCommand(
                sender,
                ArenaCommand.REBUILD,
                args,
                false,
                mapOf("error" to e.message.orEmpty(), "mode" to "sync"),
            )
            throw e
        }
    }

    private fun handleRebuildAsync(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        if (arenaManager.isBuilding()) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Another build is already in progress. " +
                    "Please wait.",
            )
            return
        }

        val currentType = arenaManager.getCurrentType()
        val typeName = currentType.name.lowercase()
        sender.sendMessage(
            "${ArenaCommand.PREFIX}Starting async arena rebuild " +
                "(type: $typeName)...",
        )
        commandLogger.logCommand(
            sender,
            ArenaCommand.REBUILD,
            args,
            true,
            mapOf(
                "type" to currentType.name.lowercase(),
                "mode" to "async",
                "status" to "started",
            ),
        )

        try {
            arenaManager.rebuildAsync(
                world = world,
                type = currentType,
                onProgress = { placed, total, percentage ->
                    if (percentage % 10 == 0) {
                        sender.sendMessage(
                            "${ArenaCommand.PREFIX}Rebuilding... " +
                                "$percentage% ($placed/$total blocks)",
                        )
                    }
                },
                onComplete = {
                    sender.sendMessage(
                        "${ArenaCommand.PREFIX}Arena rebuilt! " +
                            "Spawn markers restored.",
                    )
                    commandLogger.logCommand(
                        sender,
                        ArenaCommand.REBUILD,
                        args,
                        true,
                        mapOf(
                            "type" to currentType.name.lowercase(),
                            "mode" to "async",
                            "status" to "completed",
                        ),
                    )
                },
            )
        } catch (e: Exception) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Error during rebuild: ${e.message}",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.REBUILD,
                args,
                false,
                mapOf("error" to e.message.orEmpty(), "mode" to "async"),
            )
        }
    }

    private fun handleSetY(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            val usage = ArenaCommand.generateCommandUsage(ArenaCommand.SET_Y)
            sender.sendMessage(
                "${ArenaCommand.PREFIX}$usage",
            )
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Current Y level: " +
                    "${arenaManager.getCurrentBaseY()}",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_Y,
                args,
                false,
                mapOf("reason" to "missing_y_argument"),
            )
            return
        }
        val newY = args[1].toIntOrNull()
        if (newY == null || newY < 0 || newY > 255) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Error: Y level must be " +
                    "between 0 and 255",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_Y,
                args,
                false,
                mapOf("reason" to "invalid_y_value", "input" to args[1]),
            )
            return
        }
        val oldY = arenaManager.getCurrentBaseY()
        sender.sendMessage(
            "${ArenaCommand.PREFIX}Changing arena base Y " +
                "from $oldY to $newY...",
        )
        try {
            arenaManager.changeYLevel(
                world,
                newY,
                arenaManager.getCurrentType(),
            )
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Arena rebuilt at Y=$newY! " +
                    "Spawn markers updated.",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_Y,
                args,
                true,
                mapOf("old_y" to oldY.toString(), "new_y" to newY.toString()),
            )
        } catch (e: Exception) {
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_Y,
                args,
                false,
                mapOf(
                    "error" to e.message.orEmpty(),
                    "old_y" to oldY.toString(),
                    "attempted_y" to newY.toString(),
                ),
            )
            throw e
        }
    }
}
