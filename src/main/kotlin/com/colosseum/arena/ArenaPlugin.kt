package com.colosseum.arena

import com.colosseum.arena.domain.ArenaType
import com.colosseum.arena.domain.SpawnPosition
import com.colosseum.arena.manager.ArenaManager
import com.colosseum.arena.builders.SimpleArena
import com.colosseum.arena.builders.DetailedArena
import com.colosseum.arena.operations.ArenaClearer
import com.colosseum.arena.operations.YLevelChanger
import com.colosseum.arena.combat.CombatKit
import com.colosseum.arena.combat.KitConfig
import com.colosseum.arena.combat.ArrowTracker
import com.colosseum.arena.operations.PlayerSpawner
import com.colosseum.core.storage.PropertiesStorage
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

class ArenaPlugin : JavaPlugin(), Listener {

    private val prefix = "\u001B[32m[ArenaPlugin]\u001B[0m "
    
    // Arena manager - the facade (initialized in onEnable)
    private var manager: ArenaManager? = null
    
    // Arrow tracker for persistent arrows
    private var arrowTracker: ArrowTracker? = null

    override fun onEnable() {
        logger.info("${prefix}Enabling Colosseum Arena Plugin...")
        
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

        // Log version information
        logger.info("${prefix}Version: ${pluginMeta.version}")
        logger.info("${prefix}Colosseum Arena Plugin enabled successfully!")
        arrowTracker?.let {
            logger.info("${prefix}Arrow system: Max ${it.getMaxAllowed()} arrows (${it.getArrowCount()} per player)")
        }
    }
    
    /**
     * Initialize all components
     */
    private fun initializeComponents() {
        try {
            // Create storage
            val storage = PropertiesStorage { msg -> logger.info("${prefix}$msg") }
            
            // Create operations
            val clearer = ArenaClearer()
            val yLevelChanger = YLevelChanger(storage, clearer)
            
            // Create builders (one instance each)
            val simpleArena = SimpleArena()
            val detailedArena = DetailedArena()
            
            // Create combat kit with config
            val kitConfig = KitConfig()
            val combatKit = CombatKit(kitConfig)
            
            // Create arrow tracker (registers its own events)
            arrowTracker = ArrowTracker(this)
            
            // Create player spawner (handles spawn points and rotation)
            val playerSpawner = PlayerSpawner()
            
            // Create manager (facade)
            manager = ArenaManager(
                simpleArena = simpleArena,
                detailedArena = detailedArena,
                clearer = clearer,
                yLevelChanger = yLevelChanger,
                playerSpawner = playerSpawner,
                combatKit = combatKit,
                arrowTracker = arrowTracker!!,
                storage = storage,
                plugin = this
            )
            
            logger.info("${prefix}Components initialized successfully")
        } catch (e: Exception) {
            logger.severe("${prefix}Failed to initialize components: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    override fun onDisable() {
        logger.info("${prefix}Colosseum Arena Plugin disabled.")
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
                val currentTracker = arrowTracker
                sender.sendMessage("${prefix}Usage: /arena [ simple | detailed | rebuild | sety <y-level> | restock <player> | arrows | spawns | version ]")
                sender.sendMessage("${prefix}Version: ${pluginMeta.version}")
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
                    val currentTracker = arrowTracker
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
                "version" -> {
                    // Show version info
                    sender.sendMessage("${prefix}Colosseum Arena Plugin")
                    sender.sendMessage("${prefix}Version: ${pluginMeta.version}")
                    sender.sendMessage("${prefix}API: Paper 1.21.4")
                    sender.sendMessage("${prefix}Java: ${System.getProperty("java.version")}")
                }
                else -> {
                    sender.sendMessage("${prefix}Unknown option. Use: simple, detailed, rebuild, sety, restock, arrows, spawns, or version")
                }
            }
            return true
        }
        return false
    }
}
