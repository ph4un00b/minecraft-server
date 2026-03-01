package com.colosseum.arena

import com.colosseum.arena.domain.ArenaType
import com.colosseum.arena.domain.SpawnPosition
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

    private val prefix = "\u001B[32m[ArenaPlugin]\u001B[0m "
    
    // Arena manager - the facade (initialized in onEnable)
    private var manager: ArenaManager? = null
    
    // Version info loaded from version.properties
    private val versionInfo by lazy { VersionInfo.load(this) }

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
            logger.info("${prefix}Components initialized successfully")
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
                sender.sendMessage("${prefix}Usage: /arena [ simple | detailed | rebuild | sety <y-level> | restock <player> | arrows | spawns | version | npcs | toggleNPCs | setNPCHealth <health> | setNPCDamage <damage> | setNPCCount <count> ]")
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

            if (args.isEmpty()) {
                val currentMgr = manager
                val currentTracker = manager?.arrowTracker
                sender.sendMessage("${prefix}Usage: /arena [ simple | detailed | rebuild | sety <y-level> | restock <player> | arrows | spawns | version ]")
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

            when (args[0].lowercase()) {
                "simple" -> {
                    sender.sendMessage("${prefix}Building simple arena with spawn markers...")
                    currentMgr.rebuild(world, ArenaType.SIMPLE)
                    sender.sendMessage("${prefix}Simple arena built! Spawns at E/S/W/N inner edge")
                }
                "detailed" -> {
                    sender.sendMessage("${prefix}Building detailed gothic arena with spawn markers...")
                    currentMgr.rebuild(world, ArenaType.DETAILED)
                    sender.sendMessage("${prefix}Detailed arena built! Spawns at E/S/W/N inner edge")
                }
                "rebuild" -> {
                    val currentType = currentMgr.getCurrentType()
                    sender.sendMessage("${prefix}Rebuilding arena (type: ${currentType.name.lowercase()})...")
                    currentMgr.rebuild(world, currentType)
                    sender.sendMessage("${prefix}Arena rebuilt! Spawn markers restored.")
                }
                "sety" -> {
                    if (args.size < 2) {
                        sender.sendMessage("${prefix}Usage: /arena sety <y-level>")
                        sender.sendMessage("${prefix}Current Y level: ${currentMgr.getCurrentBaseY()}")
                        return true
                    }
                    val newY = args[1].toIntOrNull()
                    if (newY == null || newY < 0 || newY > 255) {
                        sender.sendMessage("${prefix}Error: Y level must be between 0 and 255")
                        return true
                    }
                    val oldY = currentMgr.getCurrentBaseY()
                    sender.sendMessage("${prefix}Changing arena base Y from $oldY to $newY...")
                    currentMgr.changeYLevel(world, newY, currentMgr.getCurrentType())
                    sender.sendMessage("${prefix}Arena rebuilt at Y=$newY! Spawn markers updated.")
                }
                "restock" -> {
                    // Get target player
                    val targetPlayer: Player = if (args.size >= 2) {
                        // Get player by name
                        server.getPlayer(args[1]) ?: run {
                            sender.sendMessage("${prefix}Error: Player '${args[1]}' not found or not online")
                            return true
                        }
                    } else {
                        // Use sender if they're a player
                        if (sender is Player) {
                            sender
                        } else {
                            sender.sendMessage("${prefix}Usage: /arena restock <player>")
                            return true
                        }
                    }
                    
                    // Restock the player
                    val success = currentMgr.restockPlayer(targetPlayer)
                    if (success) {
                        sender.sendMessage("${prefix}Restocked ${targetPlayer.name} with 5 arrows and repaired bow")
                        targetPlayer.sendMessage("${prefix}You have been restocked! Bow repaired, +5 arrows (max 10)")
                    } else {
                        sender.sendMessage("${prefix}Error: ${targetPlayer.name} does not have a bow")
                    }
                }
                "arrows" -> {
                    // Show arrow status
                    val currentTracker = manager?.arrowTracker
                    if (currentTracker != null) {
                        sender.sendMessage("${prefix}Arrow Status:")
                        sender.sendMessage("  Tracked arrows: ${currentTracker.getArrowCount()}")
                        sender.sendMessage("  Max allowed: ${currentTracker.getMaxAllowed()} (5 per player)")
                        sender.sendMessage("  Online players: ${Bukkit.getOnlinePlayers().size}")
                        sender.sendMessage("  Arrows persist until picked up")
                    } else {
                        sender.sendMessage("${prefix}Arrow tracker not initialized")
                    }
                }
                "spawns" -> {
                    // Show spawn info
                    sender.sendMessage("${prefix}Spawn System:")
                    sender.sendMessage("  4 fixed positions at inner edge (radius 12)")
                    sender.sendMessage("  East: Gold block marker")
                    sender.sendMessage("  South: Diamond block marker")
                    sender.sendMessage("  West: Emerald block marker")
                    sender.sendMessage("  North: Lapis block marker")
                    sender.sendMessage("  Rotation: Clockwise (E → S → W → N)")
                }
                "npcs" -> {
                    sender.sendMessage("${prefix}NPC System:")
                    sender.sendMessage("  ${manager?.npcManager?.getNPCStatus()}")
                    sender.sendMessage("  Use: /arena toggleNPCs, /arena setNPCHealth <health>, /arena setNPCDamage <damage>, /arena setNPCCount <count>")
                }
                "toggleNPCs" -> {
                    manager?.npcManager?.toggleNPCs()
                    sender.sendMessage("${prefix}NPCs ${if (manager?.npcManager?.isNPCEnabled() == true) "enabled" else "disabled"}")
                }
                "setNPCHealth" -> {
                    if (args.size < 2) {
                        sender.sendMessage("${prefix}Usage: /arena setNPCHealth <health>")
                        sender.sendMessage("${prefix}Current NPC health: ${manager?.npcManager?.getNPCHealth()}")
                        return true
                    }
                    val newHealth = args[1].toDoubleOrNull()
                    if (newHealth == null || newHealth <= 0) {
                        sender.sendMessage("${prefix}Error: Health must be a positive number")
                        return true
                    }
                    manager?.npcManager?.setNPCHealth(newHealth)
                    sender.sendMessage("${prefix}NPC health set to $newHealth")
                }
                "setNPCDamage" -> {
                    if (args.size < 2) {
                        sender.sendMessage("${prefix}Usage: /arena setNPCDamage <damage>")
                        sender.sendMessage("${prefix}Current NPC damage: ${manager?.npcManager?.getNPCDamage()}")
                        return true
                    }
                    val newDamage = args[1].toDoubleOrNull()
                    if (newDamage == null || newDamage <= 0) {
                        sender.sendMessage("${prefix}Error: Damage must be a positive number")
                        return true
                    }
                    manager?.npcManager?.setNPCDamage(newDamage)
                    sender.sendMessage("${prefix}NPC damage set to $newDamage")
                }
                "setNPCCount" -> {
                    if (args.size < 2) {
                        sender.sendMessage("${prefix}Usage: /arena setNPCCount <count>")
                        sender.sendMessage("${prefix}Current NPC count: ${manager?.npcManager?.getNPCCount()}")
                        return true
                    }
                    val newCount = args[1].toIntOrNull()
                    if (newCount == null || newCount < 0 || newCount > 4) {
                        sender.sendMessage("${prefix}Error: Count must be between 0 and 4")
                        return true
                    }
                    manager?.npcManager?.setNPCCount(newCount)
                    sender.sendMessage("${prefix}NPC count set to $newCount")
                }
                else -> {
                    sender.sendMessage("${prefix}Unknown option. Use: simple, detailed, rebuild, sety, restock, arrows, spawns, version, npcs, toggleNPCs, setNPCHealth, setNPCDamage, or setNPCCount")
                }
            }
            return true
        }
        return false
    }
}
