package com.colosseum.arena.commands

import com.colosseum.arena.NPCManager
import com.colosseum.arena.domain.NPCAttackType
import org.bukkit.command.CommandSender

/**
 * Handles NPC-related commands: npcs, togglenpcs, setnpchealth, setnpcdamage, setnpccount, setnpcattack
 */
class NPCCommands(
    private val npcManager: NPCManager?,
    private val commandLogger: CommandLogger,
) {
    fun execute(
        cmd: ArenaCommand,
        args: Array<out String>,
        sender: CommandSender,
    ) {
        when (cmd) {
            ArenaCommand.NPCS -> handleNPCs(sender, args)
            ArenaCommand.TOGGLE_NPCS -> handleToggleNPCs(sender, args)
            ArenaCommand.SET_NPC_HEALTH -> handleSetNPCHealth(sender, args)
            ArenaCommand.SET_NPC_DAMAGE -> handleSetNPCDamage(sender, args)
            ArenaCommand.SET_NPC_COUNT -> handleSetNPCCount(sender, args)
            ArenaCommand.SET_NPC_ATTACK -> handleSetNPCAttack(sender, args)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    private fun handleNPCs(sender: CommandSender, args: Array<out String>) {
        sender.sendMessage("${ArenaCommand.PREFIX}NPC System:")
        sender.sendMessage("  ${npcManager?.getNPCStatus()}")
        val npcCommands =
            listOf(
                ArenaCommand.TOGGLE_NPCS,
                ArenaCommand.SET_NPC_HEALTH,
                ArenaCommand.SET_NPC_DAMAGE,
                ArenaCommand.SET_NPC_COUNT,
                ArenaCommand.SET_NPC_ATTACK,
            )
        val usageStr =
            npcCommands.joinToString(", ") { cmd ->
                val params =
                    if (cmd.usageParams.isNotEmpty()) {
                        " ${cmd.usageParams}"
                    } else {
                        ""
                    }
                "/arena ${cmd.primaryName}$params"
            }
        sender.sendMessage("  Use: $usageStr")
        commandLogger.logCommand(
            sender,
            ArenaCommand.NPCS,
            args,
            true,
            mapOf("status" to (npcManager?.getNPCStatus() ?: "unavailable")),
        )
    }

    private fun handleToggleNPCs(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        npcManager?.toggleNPCs()
        val newStatus =
            if (npcManager?.isNPCEnabled() == true) {
                "enabled"
            } else {
                "disabled"
            }
        sender.sendMessage("${ArenaCommand.PREFIX}NPCs $newStatus")
        commandLogger.logCommand(
            sender,
            ArenaCommand.TOGGLE_NPCS,
            args,
            true,
            mapOf("new_status" to newStatus),
        )
    }

    private fun handleSetNPCHealth(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        if (args.size < 2) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(
                    ArenaCommand.SET_NPC_HEALTH,
                )}",
            )
            val currentHealth = npcManager?.getNPCHealth()
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Current NPC health: $currentHealth",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_NPC_HEALTH,
                args,
                false,
                mapOf("reason" to "missing_health_argument"),
            )
            return
        }
        val newHealth = args[1].toDoubleOrNull()
        if (newHealth == null || newHealth <= 0) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Error: Health must be a positive number",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_NPC_HEALTH,
                args,
                false,
                mapOf("reason" to "invalid_health", "input" to args[1]),
            )
            return
        }
        npcManager?.setNPCHealth(newHealth)
        sender.sendMessage("${ArenaCommand.PREFIX}NPC health set to $newHealth")
        commandLogger.logCommand(
            sender,
            ArenaCommand.SET_NPC_HEALTH,
            args,
            true,
            mapOf("health" to newHealth.toString()),
        )
    }

    private fun handleSetNPCDamage(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        if (args.size < 2) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(
                    ArenaCommand.SET_NPC_DAMAGE,
                )}",
            )
            val currentDamage = npcManager?.getNPCDamage()
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Current NPC damage: $currentDamage",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_NPC_DAMAGE,
                args,
                false,
                mapOf("reason" to "missing_damage_argument"),
            )
            return
        }
        val newDamage = args[1].toDoubleOrNull()
        if (newDamage == null || newDamage <= 0) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Error: Damage must be a positive number",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_NPC_DAMAGE,
                args,
                false,
                mapOf("reason" to "invalid_damage", "input" to args[1]),
            )
            return
        }
        npcManager?.setNPCDamage(newDamage)
        sender.sendMessage("${ArenaCommand.PREFIX}NPC damage set to $newDamage")
        commandLogger.logCommand(
            sender,
            ArenaCommand.SET_NPC_DAMAGE,
            args,
            true,
            mapOf("damage" to newDamage.toString()),
        )
    }

    private fun handleSetNPCCount(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        if (args.size < 2) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(
                    ArenaCommand.SET_NPC_COUNT,
                )}",
            )
            val currentCount = npcManager?.getNPCCount()
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Current NPC count: $currentCount",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_NPC_COUNT,
                args,
                false,
                mapOf("reason" to "missing_count_argument"),
            )
            return
        }
        val newCount = args[1].toIntOrNull()
        if (newCount == null || newCount < 0 || newCount > 4) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Error: Count must be between 0 and 4",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_NPC_COUNT,
                args,
                false,
                mapOf("reason" to "invalid_count", "input" to args[1]),
            )
            return
        }
        npcManager?.setNPCCount(newCount)
        sender.sendMessage("${ArenaCommand.PREFIX}NPC count set to $newCount")
        commandLogger.logCommand(
            sender,
            ArenaCommand.SET_NPC_COUNT,
            args,
            true,
            mapOf("count" to newCount.toString()),
        )
    }

    private fun handleSetNPCAttack(
        sender: CommandSender,
        args: Array<out String>,
    ) {
        if (args.size < 2) {
            sender.sendMessage(
                "${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(
                    ArenaCommand.SET_NPC_ATTACK,
                )}",
            )
            val currentAttack = npcManager?.getNPCAttackType()
            sender.sendMessage(
                "${ArenaCommand.PREFIX}Current NPC attack type: $currentAttack",
            )
            commandLogger.logCommand(
                sender,
                ArenaCommand.SET_NPC_ATTACK,
                args,
                false,
                mapOf("reason" to "missing_attack_argument"),
            )
            return
        }
        val attackType =
            when (args[1].lowercase()) {
                "sword" -> NPCAttackType.SWORD
                "axe" -> NPCAttackType.AXE
                "trident" -> NPCAttackType.TRIDENT
                "bow" -> NPCAttackType.BOW
                "crossbow" -> NPCAttackType.CROSSBOW
                "shield" -> NPCAttackType.SHIELD_SWORD
                "polearm" -> NPCAttackType.POLEARM
                else -> {
                    sender.sendMessage(
                        "${ArenaCommand.PREFIX}Error: Attack type must be " +
                            "'sword', 'axe', 'trident', 'bow', " +
                            "'crossbow', 'shield', or 'polearm'",
                    )
                    commandLogger.logCommand(
                        sender,
                        ArenaCommand.SET_NPC_ATTACK,
                        args,
                        false,
                        mapOf(
                            "reason" to "invalid_attack_type",
                            "input" to args[1],
                        ),
                    )
                    return
                }
            }
        npcManager?.setNPCAttackType(attackType)
        sender.sendMessage(
            "${ArenaCommand.PREFIX}NPC attack type set to $attackType " +
                "(rebuild arena to apply)",
        )
        commandLogger.logCommand(
            sender,
            ArenaCommand.SET_NPC_ATTACK,
            args,
            true,
            mapOf("type" to attackType.name.lowercase()),
        )
    }
}
