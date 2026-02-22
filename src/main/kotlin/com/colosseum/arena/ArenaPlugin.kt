package com.colosseum.arena

import com.colosseum.core.storage.PropertiesStorage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.NamespacedKey
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ArenaPlugin : JavaPlugin(), Listener {

    private val arenaBuiltKey = NamespacedKey(this, "arena_built")
    private val arenaTypeKey = NamespacedKey(this, "arena_type")
    private val prefix = "\u001B[32m[ArenaPlugin]\u001B[0m "

    // Arena configuration storage
    private lateinit var storage: PropertiesStorage

    override fun onLoad() {
        // Initialize storage with logger callback
        storage = PropertiesStorage { msg -> logger.info("${prefix}$msg") }
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

        checkAndBuildArena(world)

        logger.info("${prefix}Colosseum Arena Plugin enabled successfully!")
    }

    override fun onDisable() {
        logger.info("${prefix}Colosseum Arena Plugin disabled.")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.teleport(Location(server.getWorld("world"), 0.5, (storage.arenaBaseY + 1).toDouble(), 0.5))
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
                sender.sendMessage("${prefix}Usage: /arena [ simple | detailed | rebuild | sety <y-level> ]")
                sender.sendMessage("${prefix}Current config: ${storage.getConfigSummary()}")
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
                    rebuildArena(world, "simple")
                    storage.setArenaType("simple")
                    sender.sendMessage("${prefix}Simple arena built! Saved to phau.properties")
                }
                "detailed" -> {
                    sender.sendMessage("${prefix}Building detailed gothic arena...")
                    rebuildArena(world, "detailed")
                    storage.setArenaType("detailed")
                    sender.sendMessage("${prefix}Detailed gothic arena built! Saved to phau.properties")
                }
                "rebuild" -> {
                    val currentType = world.persistentDataContainer.get(arenaTypeKey, PersistentDataType.STRING) ?: storage.arenaType
                    sender.sendMessage("${prefix}Rebuilding arena (type: $currentType)...")
                    rebuildArena(world, currentType)
                    sender.sendMessage("${prefix}Arena rebuilt!")
                }
                "sety" -> {
                    if (args.size < 2) {
                        sender.sendMessage("${prefix}Usage: /arena sety <y-level>")
                        sender.sendMessage("${prefix}Current Y level: ${storage.arenaBaseY}")
                        return true
                    }
                    val newY = args[1].toIntOrNull()
                    if (newY == null || newY < 0 || newY > 255) {
                        sender.sendMessage("${prefix}Error: Y level must be between 0 and 255")
                        return true
                    }
                    val oldY = storage.arenaBaseY
                    storage.setArenaBaseY(newY)
                    sender.sendMessage("${prefix}Changed arena base Y from $oldY to $newY")
                    sender.sendMessage("${prefix}Rebuilding arena at new Y level...")
                    val currentType = world.persistentDataContainer.get(arenaTypeKey, PersistentDataType.STRING) ?: storage.arenaType
                    rebuildArena(world, currentType)
                    // Update spawn location
                    world.spawnLocation = Location(world, 0.5, (storage.arenaBaseY + 1).toDouble(), 0.5)
                    sender.sendMessage("${prefix}Arena rebuilt at Y=$newY! Spawn updated.")
                }
                else -> {
                    sender.sendMessage("${prefix}Unknown option. Use: simple, detailed, rebuild, or sety")
                }
            }
            return true
        }
        return false
    }

    private fun checkAndBuildArena(world: World) {
        val pdc = world.persistentDataContainer

        if (pdc.has(arenaBuiltKey, PersistentDataType.INTEGER)) {
            logger.info("${prefix}Arena already built. Skipping generation.")
            val type = pdc.get(arenaTypeKey, PersistentDataType.STRING) ?: "unknown"
            logger.info("${prefix}Existing arena type: $type")
        } else {
            val arenaType = storage.arenaType
            logger.info("${prefix}Building $arenaType arena at Y=${storage.arenaBaseY}...")

            if (arenaType == "simple") {
                buildSimpleArena(world)
            } else {
                buildDetailedArena(world)
            }

            pdc.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
            pdc.set(arenaTypeKey, PersistentDataType.STRING, arenaType)
            logger.info("${prefix}Arena construction complete!")
        }

        // Set spawn location (1 block above arena base)
        val spawnLocation = Location(world, 0.5, (storage.arenaBaseY + 1).toDouble(), 0.5)
        world.spawnLocation = spawnLocation
    }

    private fun rebuildArena(world: World, type: String) {
        // Clear existing arena markers
        world.persistentDataContainer.remove(arenaBuiltKey)

        // Clear the arena area
        clearArenaArea(world)

        // Build new arena
        if (type == "simple") {
            buildSimpleArena(world)
        } else {
            buildDetailedArena(world)
        }

        world.persistentDataContainer.set(arenaBuiltKey, PersistentDataType.INTEGER, 1)
        world.persistentDataContainer.set(arenaTypeKey, PersistentDataType.STRING, type)
    }

    private fun clearArenaArea(world: World) {
        val radius = 25
        // Clear from world bottom to top
        val minY = world.minHeight
        val maxY = world.maxHeight.coerceAtMost(320)

        logger.info("${prefix}Clearing arena area (radius=$radius, Y=$minY to $maxY)...")

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                for (y in minY..maxY) {
                    val block = world.getBlockAt(x, y, z)
                    // Remove everything except bedrock
                    if (block.type != Material.BEDROCK) {
                        block.type = Material.AIR
                    }
                }
            }
        }

        logger.info("${prefix}Arena area cleared")
    }

    private fun buildSimpleArena(world: World) {
        val centerX = 0
        val centerZ = 0
        val groundY = storage.arenaBaseY
        val innerRadius = 12
        val outerRadius = 18
        val wallHeight = 6

        // Build ground
        for (x in -outerRadius..outerRadius) {
            for (z in -outerRadius..outerRadius) {
                val distance = sqrt((x * x + z * z).toDouble())
                if (distance <= outerRadius) {
                    world.getBlockAt(centerX + x, groundY, centerZ + z).type = Material.GRASS_BLOCK
                }
            }
        }

        // Build wall ring
        for (x in -outerRadius..outerRadius) {
            for (z in -outerRadius..outerRadius) {
                val distance = sqrt((x * x + z * z).toDouble())
                if (distance >= innerRadius && distance <= outerRadius) {
                    for (h in 0 until wallHeight) {
                        world.getBlockAt(centerX + x, groundY + 1 + h, centerZ + z).type = Material.STONE_BRICKS
                    }
                }
            }
        }

        // Create entrance (gate) at North
        for (x in -3..3) {
            for (y in groundY + 1..groundY + wallHeight) {
                for (z in -outerRadius - 1..-innerRadius + 1) {
                    world.getBlockAt(centerX + x, y, centerZ + z).type = Material.AIR
                }
            }
        }

        // Add gate arch
        for (x in -4..4) {
            world.getBlockAt(centerX + x, groundY + wallHeight + 1, centerZ - outerRadius).type = Material.STONE_BRICKS
        }
        for (y in groundY + 1..groundY + wallHeight + 1) {
            world.getBlockAt(centerX - 4, y, centerZ - outerRadius).type = Material.STONE_BRICKS
            world.getBlockAt(centerX + 4, y, centerZ - outerRadius).type = Material.STONE_BRICKS
        }

        logger.info("${prefix}Simple arena built: $innerRadius-$outerRadius radius, ${wallHeight}m walls")
    }

    private fun buildDetailedArena(world: World) {
        val centerX = 0
        val centerZ = 0
        val groundY = storage.arenaBaseY

        // Wall parameters (thick wall: 12.0 <= r <= 19.5)
        val innerRadius = 12.0
        val outerRadius = 19.5
        val wallHeight = 10
        val wallTop = groundY + wallHeight

        // Build ground
        buildGround(world, centerX, centerZ, groundY, outerRadius.toInt() + 2)

        // Build thick wall with gothic architecture
        buildThickWall(world, centerX, centerZ, groundY, innerRadius, outerRadius, wallHeight)

        // Build buttresses at cardinal and intercardinal directions (skip North for gate)
        buildButtresses(world, centerX, centerZ, groundY, outerRadius, wallHeight)

        // Build windows at 22.5 degree offsets
        buildWindows(world, centerX, centerZ, groundY, innerRadius, outerRadius, wallHeight)

        // Build gate at North
        buildGate(world, centerX, centerZ, groundY, outerRadius, wallHeight)

        // Add crenellations
        buildCrenellations(world, centerX, centerZ, wallTop, innerRadius, outerRadius)

        // Add floor pattern
        buildFloorPattern(world, centerX, centerZ, groundY, innerRadius.toInt())

        logger.info("${prefix}Detailed gothic arena built with buttresses, windows, and gate")
    }

    private fun buildGround(world: World, centerX: Int, centerZ: Int, groundY: Int, radius: Int) {
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val distance = sqrt((x * x + z * z).toDouble())
                if (distance <= radius) {
                    val block = world.getBlockAt(centerX + x, groundY, centerZ + z)
                    block.type = if (distance <= 12) {
                        if ((x + z) % 2 == 0) Material.SMOOTH_STONE else Material.STONE_BRICKS
                    } else {
                        Material.GRASS_BLOCK
                    }
                }
            }
        }
    }

    private fun buildThickWall(
        world: World,
        centerX: Int,
        centerZ: Int,
        groundY: Int,
        innerRadius: Double,
        outerRadius: Double,
        wallHeight: Int
    ) {
        val wallTop = groundY + wallHeight

        for (x in -outerRadius.toInt()..outerRadius.toInt()) {
            for (z in -outerRadius.toInt()..outerRadius.toInt()) {
                val distance = sqrt((x * x + z * z).toDouble())

                // Wall condition: 12.0 <= distance <= 19.5
                if (distance >= innerRadius && distance <= outerRadius) {
                    for (y in groundY + 1..wallTop) {
                        val block = world.getBlockAt(centerX + x, y, centerZ + z)

                        // Varied stone materials for texture
                        block.type = when {
                            distance > 18 -> Material.DEEPSLATE_BRICKS
                            distance > 15 -> Material.STONE_BRICKS
                            else -> Material.CRACKED_STONE_BRICKS
                        }
                    }
                }
            }
        }
    }

    private fun buildButtresses(
        world: World,
        centerX: Int,
        centerZ: Int,
        groundY: Int,
        outerRadius: Double,
        wallHeight: Int
    ) {
        // Buttresses at E, SE, S, SW, W, NW, NE (skip N for gate)
        val buttressAngles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 315.0)

        for (angleDeg in buttressAngles) {
            val angleRad = Math.toRadians(angleDeg)
            val x = (cos(angleRad) * outerRadius).toInt()
            val z = (sin(angleRad) * outerRadius).toInt()

            // Build buttress (3x3 base, tapering up)
            for (by in groundY + 1..groundY + wallHeight + 2) {
                val width = when {
                    by < groundY + 3 -> 2
                    by < groundY + 6 -> 1
                    else -> 0
                }

                for (bx in -width..width) {
                    for (bz in -width..width) {
                        val blockX = centerX + x + bx
                        val blockZ = centerZ + z + bz
                        val block = world.getBlockAt(blockX, by, blockZ)
                        block.type = Material.DEEPSLATE_BRICKS
                    }
                }
            }
        }
    }

    private fun buildWindows(
        world: World,
        centerX: Int,
        centerZ: Int,
        groundY: Int,
        innerRadius: Double,
        outerRadius: Double,
        wallHeight: Int
    ) {
        // Windows at 22.5 degree offsets from buttresses
        val windowAngles = listOf(22.5, 67.5, 112.5, 157.5, 202.5, 247.5, 292.5, 337.5)
        val windowWidth = 3

        for (angleDeg in windowAngles) {
            val angleRad = Math.toRadians(angleDeg)

            // Carve window through wall thickness
            for (r in innerRadius.toInt()..outerRadius.toInt()) {
                val x = (cos(angleRad) * r).toInt()
                val z = (sin(angleRad) * r).toInt()

                // Window height (gothic arch shape)
                for (wy in groundY + 2..groundY + 6) {
                    val isArch = wy == groundY + 6 && r > (innerRadius + outerRadius) / 2
                    if (!isArch) {
                        world.getBlockAt(centerX + x, wy, centerZ + z).type = Material.AIR
                    }
                }
            }
        }
    }

    private fun buildGate(
        world: World,
        centerX: Int,
        centerZ: Int,
        groundY: Int,
        outerRadius: Double,
        wallHeight: Int
    ) {
        // Gate at North (Z = -19, angle 270/-90 degrees)
        val gateZ = -outerRadius.toInt()
        val gateWidth = 5
        val gateHeight = 8

        // Carve opening
        for (x in -gateWidth..gateWidth) {
            for (y in groundY + 1..groundY + gateHeight) {
                for (z in gateZ..gateZ + 2) {
                    world.getBlockAt(centerX + x, y, centerZ + z).type = Material.AIR
                }
            }
        }

        // Build arch
        val archRadius = gateWidth + 1
        for (angle in 0..180) {
            val rad = Math.toRadians(angle.toDouble())
            val archX = (cos(rad) * archRadius).toInt()
            val archY = (sin(rad) * (gateHeight / 2)).toInt()

            world.getBlockAt(centerX + archX, groundY + gateHeight + archY, centerZ + gateZ).type =
                Material.CHISELED_STONE_BRICKS
        }

        // Gate towers
        for (towerX in listOf(-gateWidth - 1, gateWidth + 1)) {
            for (y in groundY + 1..groundY + wallHeight + 3) {
                for (z in gateZ..gateZ + 2) {
                    world.getBlockAt(centerX + towerX, y, centerZ + z).type = Material.DEEPSLATE_BRICKS
                }
            }
        }
    }

    private fun buildCrenellations(
        world: World,
        centerX: Int,
        centerZ: Int,
        wallTop: Int,
        innerRadius: Double,
        outerRadius: Double
    ) {
        // Add battlements every 2 blocks
        for (angle in 0 until 360 step 10) {
            val rad = Math.toRadians(angle.toDouble())

            // Outer crenellations
            val outerX = (cos(rad) * (outerRadius + 0.5)).toInt()
            val outerZ = (sin(rad) * (outerRadius + 0.5)).toInt()

            if (angle % 20 == 0) {
                world.getBlockAt(centerX + outerX, wallTop + 1, centerZ + outerZ).type = Material.STONE_BRICK_WALL
            }

            // Skip inner crenellations at gate
            val innerX = (cos(rad) * (innerRadius - 0.5)).toInt()
            val innerZ = (sin(rad) * (innerRadius - 0.5)).toInt()

            if (angle % 20 == 0 && innerZ > -18) {
                world.getBlockAt(centerX + innerX, wallTop + 1, centerZ + innerZ).type = Material.STONE_BRICK_WALL
            }
        }
    }

    private fun buildFloorPattern(world: World, centerX: Int, centerZ: Int, groundY: Int, radius: Int) {
        // Create a decorative floor pattern in the arena
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val distance = sqrt((x * x + z * z).toDouble())
                if (distance <= radius) {
                    val block = world.getBlockAt(centerX + x, groundY, centerZ + z)

                    // Concentric circles pattern
                    val patternRadius = distance.toInt()
                    block.type = when {
                        patternRadius % 4 == 0 -> Material.POLISHED_ANDESITE
                        patternRadius % 2 == 0 -> Material.SMOOTH_STONE
                        (x + z) % 3 == 0 -> Material.MOSSY_STONE_BRICKS
                        else -> Material.STONE_BRICKS
                    }
                }
            }
        }

        // Center podium
        for (x in -2..2) {
            for (z in -2..2) {
                world.getBlockAt(centerX + x, groundY + 1, centerZ + z).type = Material.CHISELED_STONE_BRICKS
            }
        }
        world.getBlockAt(centerX, groundY + 2, centerZ).type = Material.LANTERN
    }
}
