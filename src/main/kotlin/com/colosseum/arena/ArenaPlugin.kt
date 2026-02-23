package com.colosseum.arena

import com.colosseum.arena.domain.ArenaType
import com.colosseum.arena.manager.ArenaManager
import com.colosseum.arena.builders.SimpleArena
import com.colosseum.arena.builders.DetailedArena
import com.colosseum.arena.operations.ArenaClearer
import com.colosseum.arena.operations.YLevelChanger
import com.colosseum.arena.combat.CombatKit
import com.colosseum.arena.combat.KitConfig
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
    
    // Arena manager - the facade
    private lateinit var manager: ArenaManager

    override fun onLoad() {
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
        
        // Create manager (facade)
        manager = ArenaManager(
            simpleArena = simpleArena,
            detailedArena = detailedArena,
            clearer = clearer,
            yLevelChanger = yLevelChanger,
            combatKit = combatKit,
            storage = storage,
            plugin = this
        )
    }

    override fun onEnable() {
        logger.info("${prefix}Enabling Colosseum Arena Plugin...")

        server.pluginManager.registerEvents(this, this)

        val world = server.getWorld("world")
        if (world == null) {
            logger.severe("${prefix}Default world not found! Plugin disabled.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Use manager to check and build
        val wasBuilt = manager.checkAndBuild(world)
        if (wasBuilt) {
            logger.info("${prefix}Arena construction complete!")
        } else {
            logger.info("${prefix}Arena already built. Skipping generation.")
        }
        
        // Update spawn
        manager.updateSpawn(world)

        logger.info("${prefix}Colosseum Arena Plugin enabled successfully!")
    }

    override fun onDisable() {
        logger.info("${prefix}Colosseum Arena Plugin disabled.")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val world = server.getWorld("world") ?: return
        
        // Teleport to spawn
        player.teleport(Location(world, 0.5, (manager.getCurrentBaseY() + 1).toDouble(), 0.5))
        
        // Equip with combat kit on first join
        manager.equipPlayer(player)
        player.sendMessage("${prefix}Welcome to the arena! You received a combat kit.")
    }
    
    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val world = server.getWorld("world") ?: return
        
        // Set respawn location to arena spawn
        event.respawnLocation = Location(world, 0.5, (manager.getCurrentBaseY() + 1).toDouble(), 0.5)
        
        // Equip with fresh combat kit on respawn
        manager.equipPlayer(player)
        player.sendMessage("${prefix}Respawned! Fresh combat kit equipped.")
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
                sender.sendMessage("${prefix}Usage: /arena [ simple | detailed | rebuild | sety <y-level> | restock <player> ]")
                sender.sendMessage("${prefix}Current: base-y=${manager.getCurrentBaseY()}, type=${manager.getCurrentType().name.lowercase()}")
                return true
            }

            val world = server.getWorld("world")
            if (world == null) {
                sender.sendMessage("${prefix}Error: World not found!")
                return true
            }

            when (args[0].lowercase()) {
                "simple" -> {
                    sender.sendMessage("${prefix}Building simple arena...")
                    manager.rebuild(world, ArenaType.SIMPLE)
                    sender.sendMessage("${prefix}Simple arena built! Saved to phau.properties")
                }
                "detailed" -> {
                    sender.sendMessage("${prefix}Building detailed gothic arena...")
                    manager.rebuild(world, ArenaType.DETAILED)
                    sender.sendMessage("${prefix}Detailed gothic arena built! Saved to phau.properties")
                }
                "rebuild" -> {
                    val currentType = manager.getCurrentType()
                    sender.sendMessage("${prefix}Rebuilding arena (type: ${currentType.name.lowercase()})...")
                    manager.rebuild(world, currentType)
                    sender.sendMessage("${prefix}Arena rebuilt!")
                }
                "sety" -> {
                    if (args.size < 2) {
                        sender.sendMessage("${prefix}Usage: /arena sety <y-level>")
                        sender.sendMessage("${prefix}Current Y level: ${manager.getCurrentBaseY()}")
                        return true
                    }
                    val newY = args[1].toIntOrNull()
                    if (newY == null || newY < 0 || newY > 255) {
                        sender.sendMessage("${prefix}Error: Y level must be between 0 and 255")
                        return true
                    }
                    val oldY = manager.getCurrentBaseY()
                    sender.sendMessage("${prefix}Changing arena base Y from $oldY to $newY...")
                    manager.changeYLevel(world, newY, manager.getCurrentType())
                    manager.updateSpawn(world)
                    sender.sendMessage("${prefix}Arena rebuilt at Y=$newY! Spawn updated.")
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
                    val success = manager.restockPlayer(targetPlayer)
                    if (success) {
                        sender.sendMessage("${prefix}Restocked ${targetPlayer.name} with 5 arrows and repaired bow")
                        targetPlayer.sendMessage("${prefix}You have been restocked! Bow repaired, +5 arrows (max 10)")
                    } else {
                        sender.sendMessage("${prefix}Error: ${targetPlayer.name} does not have a bow")
                    }
                }
                else -> {
                    sender.sendMessage("${prefix}Unknown option. Use: simple, detailed, rebuild, sety, or restock")
                }
            }
            return true
        }
        return false
    }
}
