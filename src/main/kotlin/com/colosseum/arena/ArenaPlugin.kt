package com.colosseum.arena

import com.colosseum.arena.commands.ArenaCommand
import com.colosseum.arena.commands.BuildCommands
import com.colosseum.arena.commands.CommandDisplay
import com.colosseum.arena.commands.CommandLogger
import com.colosseum.arena.commands.CommandSuggestion
import com.colosseum.arena.commands.InfoCommands
import com.colosseum.arena.commands.NPCCommands
import com.colosseum.arena.commands.PlayerCommands
import com.colosseum.arena.manager.ArenaManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.Properties

/**
 * Version information data class
 */
data class VersionInfo(
    val version: String,
    val buildTime: String,
    val gitHash: String,
    val pluginName: String
) {
    companion object {
        fun load(plugin: JavaPlugin): VersionInfo {
            val props = Properties()
            val stream = plugin.getResource("version.properties")
            
            return if (stream != null) {
                props.load(stream)
                VersionInfo(
                    version = props.getProperty("version", "unknown"),
                    buildTime = props.getProperty("build.time", "unknown"),
                    gitHash = props.getProperty("git.hash", "unknown"),
                    pluginName = props.getProperty("plugin.name", "ColosseumArena")
                )
            } else {
                // Fallback if version.properties not found
                VersionInfo("unknown", "unknown", "unknown", "ColosseumArena")
            }
        }
    }
}

class ArenaPlugin : JavaPlugin(), Listener {

    // Arena manager - the facade (initialized in onEnable)
    private var manager: ArenaManager? = null

    // Version info loaded from version.properties
    private val versionInfo by lazy { VersionInfo.load(this) }

    private val prefix = ArenaCommand.PREFIX

    // Command logger for audit trail
    private lateinit var commandLogger: CommandLogger

    override fun onEnable() {
        logger.info("${prefix}Enabling Colosseum Arena Plugin...")
        
        // Check required plugins - throws exception on failure to stop server immediately
        try {
            checkRequiredPlugins()
        } catch (e: Exception) {
            logger.severe("${prefix}CRITICAL: Required plugin check failed: ${e.message}")
            logger.severe("${prefix}Server will be shut down to prevent damage.")
            // Force immediate shutdown using system exit as backup
            logger.severe("${prefix}SHUTTING DOWN SERVER NOW!")
            Thread {
                Thread.sleep(1000) // Give logs time to flush
                Bukkit.shutdown()
            }.start()
            return
        }
        
        // Initialize components in onEnable (safer than onLoad)
        initializeComponents()

        server.pluginManager.registerEvents(this, this)

        val world = server.getWorld("world")
        if (world == null) {
            logger.severe("${prefix}Default world not found! Plugin disabled.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Use manager to check and build
        manager?.let { mgr ->
            val wasBuilt = mgr.checkAndBuild(world)
            if (wasBuilt) {
                logger.info("${prefix}Arena construction complete with spawn markers!")
            } else {
                logger.info("${prefix}Arena already built. Skipping generation.")
            }
        } ?: run {
            logger.severe("${prefix}Manager not initialized! Plugin disabled.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Log version information from version.properties
        logger.info("${prefix}Version: ${versionInfo.version}")
        logger.info("${prefix}Built: ${versionInfo.buildTime}")
        logger.info("${prefix}Git: ${versionInfo.gitHash}")
        logger.info("${prefix}Colosseum Arena Plugin enabled successfully!")
        manager?.let {
            logger.info("${prefix}Arrow system: Max ${it.arrowTracker.getMaxAllowed()} arrows (${it.arrowTracker.getArrowCount()} per player)")
        }
        
        // Display all available commands in brilliant purple
        val commandDisplay = CommandDisplay(logger)
        commandDisplay.displayAllCommands()
    }
    
    /**
     * Check that required plugins (Citizens and Sentinel) are installed
     * This method throws exceptions to stop the server immediately if dependencies are missing
     */
    private fun checkRequiredPlugins(): Boolean {
        logger.info("${prefix}Checking required dependencies...")
        
        // Check Citizens first - it's a dependency for Sentinel
        val citizensPlugin = server.pluginManager.getPlugin("Citizens")
        if (citizensPlugin == null) {
            logger.severe("${prefix}=================================")
            logger.severe("${prefix}CRITICAL ERROR: Citizens plugin is not installed!")
            logger.severe("${prefix}Please download Citizens from: https://wiki.citizensnpcs.co/Versions")
            logger.severe("${prefix}This plugin requires Citizens to function.")
            logger.severe("${prefix}=================================")
            throw IllegalStateException("Citizens plugin is required but not installed")
        }
        
        if (!citizensPlugin.isEnabled) {
            logger.severe("${prefix}=================================")
            logger.severe("${prefix}CRITICAL ERROR: Citizens plugin failed to load!")
            logger.severe("${prefix}Check Citizens configuration and logs above.")
            logger.severe("${prefix}=================================")
            throw IllegalStateException("Citizens plugin failed to load")
        }
        
        // Verify Citizens API is actually accessible
        try {
            val citizensAPI = Class.forName("net.citizensnpcs.api.CitizensAPI")
            logger.info("${prefix}Citizens API verified: ${citizensAPI.name}")
        } catch (e: ClassNotFoundException) {
            logger.severe("${prefix}=================================")
            logger.severe("${prefix}CRITICAL ERROR: Citizens API not accessible!")
            logger.severe("${prefix}Citizens plugin may be corrupted or incompatible.")
            logger.severe("${prefix}=================================")
            throw IllegalStateException("Citizens API not accessible", e)
        }
        
        @Suppress("DEPRECATION")
        val citizensVersion = citizensPlugin.description.version
        logger.info("${prefix}Citizens v$citizensVersion found and enabled")
        
        // Check Sentinel
        val sentinelPlugin = server.pluginManager.getPlugin("Sentinel")
        if (sentinelPlugin == null) {
            logger.severe("${prefix}=================================")
            logger.severe("${prefix}CRITICAL ERROR: Sentinel plugin is not installed!")
            logger.severe("${prefix}Please download Sentinel from: https://wiki.citizensnpcs.co/Sentinel")
            logger.severe("${prefix}This plugin requires Sentinel to function.")
            logger.severe("${prefix}=================================")
            throw IllegalStateException("Sentinel plugin is required but not installed")
        }
        
        if (!sentinelPlugin.isEnabled) {
            logger.severe("${prefix}=================================")
            logger.severe("${prefix}CRITICAL ERROR: Sentinel plugin failed to load!")
            logger.severe("${prefix}This usually means Citizens failed to load first.")
            logger.severe("${prefix}Check logs above for Citizens errors.")
            logger.severe("${prefix}=================================")
            throw IllegalStateException("Sentinel plugin failed to load - check if Citizens loaded successfully")
        }
        
        // Verify Sentinel API is accessible
        try {
            val sentinelTrait = Class.forName("org.mcmonkey.sentinel.SentinelTrait")
            logger.info("${prefix}Sentinel API verified: ${sentinelTrait.name}")
        } catch (e: ClassNotFoundException) {
            logger.severe("${prefix}=================================")
            logger.severe("${prefix}CRITICAL ERROR: Sentinel API not accessible!")
            logger.severe("${prefix}Sentinel plugin may be corrupted or incompatible.")
            logger.severe("${prefix}=================================")
            throw IllegalStateException("Sentinel API not accessible", e)
        }
        
        @Suppress("DEPRECATION")
        val sentinelVersion = sentinelPlugin.description.version
        logger.info("${prefix}Sentinel v$sentinelVersion found and enabled")
        logger.info("${prefix}All required dependencies verified!")
        
        return true
    }
    
    /**
     * Initialize all components
     */
    private fun initializeComponents() {
        try {
            manager = ArenaManager(this)
            commandLogger = CommandLogger(dataFolder)
            logger.info("${prefix}Components initialized successfully")
            logger.info("${prefix}Command logging enabled: ${commandLogger.getLogFile().absolutePath}")
        } catch (e: Exception) {
            logger.severe("${prefix}Failed to initialize: ${e.message}")
            throw e
        }
    }

    override fun onDisable() {
        logger.info("${prefix}Colosseum Arena Plugin disabled.")
        // Clear NPCs on disable
        manager?.npcManager?.clearAllNPCs()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val world = server.getWorld("world") ?: return
        val mgr = manager ?: return
        
        // Get next spawn point (rotates: E, S, W, N)
        val spawnLoc = mgr.getNextSpawnPoint(world)
        val spawnName = mgr.getSpawnLocationName(spawnLoc.blockX, spawnLoc.blockZ)
        
        // Teleport to assigned spawn
        player.teleport(spawnLoc)
        
        // Log player position
        logger.info("${prefix}Player ${player.name} joined -> Spawn: $spawnName at (${spawnLoc.x}, ${spawnLoc.y}, ${spawnLoc.z})")
        
        // Equip with combat kit
        mgr.equipPlayer(player)
        
        player.sendMessage("${prefix}Welcome to the arena! You received a combat kit.")
        player.sendMessage("${prefix}Spawned at: $spawnName. Arrows are limited - pick them up!")
    }
    
    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val world = server.getWorld("world") ?: return
        val mgr = manager ?: return
        
        // Get next spawn point for respawn
        val spawnLoc = mgr.getNextSpawnPoint(world)
        val spawnName = mgr.getSpawnLocationName(spawnLoc.blockX, spawnLoc.blockZ)
        
        // Set respawn location
        event.respawnLocation = spawnLoc
        
        // Equip with fresh combat kit
        mgr.equipPlayer(player)
        
        player.sendMessage("${prefix}Respawned at: $spawnName! Fresh combat kit equipped.")
        player.sendMessage("${prefix}Pick up arrows from the ground to restock!")
    }

override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name.equals("arena", ignoreCase = true)) {
            if (!sender.hasPermission("colosseum.arena.admin")) {
                sender.sendMessage("${prefix}You don't have permission to use this command.")
                return true
            }

            if (args.isEmpty()) {
                val currentMgr = manager
                val currentTracker = manager?.arrowTracker
                sender.sendMessage("${prefix}Usage: /arena [ ${ArenaCommand.generateUsageString()} ]")
                sender.sendMessage("${prefix}Version: ${versionInfo.version}")
                if (currentMgr != null) {
                    sender.sendMessage("${prefix}Current: base-y=${currentMgr.getCurrentBaseY()}, type=${currentMgr.getCurrentType().name.lowercase()}")
                }
                if (currentTracker != null) {
                    sender.sendMessage("${prefix}Arrows: ${currentTracker.getArrowCount()}/${currentTracker.getMaxAllowed()} (5 per player)")
                }
                sender.sendMessage("${prefix}Spawn rotation: East → South → West → North (clockwise)")
                return true
            }

            val world = server.getWorld("world")
            if (world == null) {
                sender.sendMessage("${prefix}Error: World not found!")
                return true
            }

            val currentMgr = manager ?: run {
                sender.sendMessage("${prefix}Error: Plugin not fully initialized")
                return true
            }

            val cmd = ArenaCommand.fromString(args[0])
            if (cmd == null) {
                val suggestion = CommandSuggestion.suggestSimilar(args[0])
                if (suggestion != null) {
                    sender.sendMessage("${prefix}Unknown command '${args[0]}'. Did you mean '${suggestion}'?")
                } else {
                    sender.sendMessage("${prefix}${ArenaCommand.generateUnknownOptionMessage()}")
                }
                return true
            }

            // Check if a build is in progress for build commands
            val isBuildCommand = cmd in listOf(ArenaCommand.SIMPLE, ArenaCommand.DETAILED, ArenaCommand.REBUILD)
            
            // Initialize command category handlers with dependency injection
            val buildCommands = BuildCommands(currentMgr, world, commandLogger, this)
            val playerCommands = PlayerCommands(currentMgr, commandLogger)
            val npcCommands = NPCCommands(currentMgr.npcManager, commandLogger)
            val infoCommands = InfoCommands(versionInfo, currentMgr, commandLogger)

            // Handle cancel command first
            if (cmd == ArenaCommand.CANCEL) {
                if (buildCommands.isBuilding()) {
                    sender.sendMessage("${prefix}Cannot cancel - build is already in progress and cannot be interrupted")
                    commandLogger.logCommand(sender, cmd, args, false, mapOf("reason" to "build_in_progress_no_cancel"))
                } else {
                    sender.sendMessage("${prefix}No operation to cancel")
                    commandLogger.logCommand(sender, cmd, args, false, mapOf("reason" to "no_operation"))
                }
                return true
            }

            // Check if this is a destructive command
            val isDestructive = cmd in listOf(ArenaCommand.SIMPLE, ArenaCommand.DETAILED, ArenaCommand.REBUILD, ArenaCommand.SET_Y)

            if (isDestructive) {
                // Check for force flag
                val hasForceFlag = args.size > 1 && args[1].equals("f", ignoreCase = true)

                // Execute with appropriate mode (sync with 'f' flag, async by default)
                val newArgs = if (hasForceFlag) {
                    args.filterIndexed { index, _ -> index != 1 }.toTypedArray()
                } else {
                    args
                }
                
                buildCommands.execute(cmd, newArgs, sender, hasForceFlag)
                return true
            }

            // Direct dispatch to appropriate category handler
            when (cmd) {
                ArenaCommand.RESTOCK,
                ArenaCommand.ARROWS -> playerCommands.execute(cmd, args, sender)

                ArenaCommand.NPCS,
                ArenaCommand.TOGGLE_NPCS,
                ArenaCommand.SET_NPC_HEALTH,
                ArenaCommand.SET_NPC_DAMAGE,
                ArenaCommand.SET_NPC_COUNT,
                ArenaCommand.SET_NPC_ATTACK -> npcCommands.execute(cmd, args, sender)

                ArenaCommand.SPAWNS,
                ArenaCommand.VERSION,
                ArenaCommand.HELP -> infoCommands.execute(cmd, args, sender)

                else -> {
                    sender.sendMessage("${prefix}Unknown command")
                    commandLogger.logCommand(sender, cmd, args, false, mapOf("reason" to "unknown_command"))
                }
            }
            return true
        }
        return false
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String>? {
        if (command.name != "arena") return null

        return when (args.size) {
            1 -> ArenaCommand.entries
                .map { it.primaryName }
                .filter { it.startsWith(args[0].lowercase()) }
            2 -> when (ArenaCommand.fromString(args[0])) {
                ArenaCommand.RESTOCK -> server.onlinePlayers
                    .map { it.name }
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                ArenaCommand.SET_NPC_ATTACK -> listOf("arrow", "fireball")
                    .filter { it.startsWith(args[1].lowercase()) }
                ArenaCommand.SET_NPC_COUNT -> listOf("0", "1", "2", "3", "4")
                    .filter { it.startsWith(args[1]) }
                ArenaCommand.SET_Y -> listOf(manager?.getCurrentBaseY()?.toString() ?: "64")
                ArenaCommand.SIMPLE, ArenaCommand.DETAILED, ArenaCommand.REBUILD -> listOf("f")
                    .filter { it.startsWith(args[1].lowercase()) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
