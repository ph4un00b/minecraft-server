package com.colosseum.arena.commands

import com.colosseum.arena.NPCManager
import com.colosseum.arena.domain.NPCAttackType
import org.bukkit.command.CommandSender

/**
 * Handles NPC-related commands: npcs, togglenpcs, setnpchealth, setnpcdamage, setnpccount, setnpcattack
 */
class NPCCommands(
    private val npcManager: NPCManager?
) {
    fun execute(cmd: ArenaCommand, args: Array<out String>, sender: CommandSender) {
        when (cmd) {
            ArenaCommand.NPCS -> handleNPCs(sender)
            ArenaCommand.TOGGLE_NPCS -> handleToggleNPCs(sender)
            ArenaCommand.SET_NPC_HEALTH -> handleSetNPCHealth(sender, args)
            ArenaCommand.SET_NPC_DAMAGE -> handleSetNPCDamage(sender, args)
            ArenaCommand.SET_NPC_COUNT -> handleSetNPCCount(sender, args)
            ArenaCommand.SET_NPC_ATTACK -> handleSetNPCAttack(sender, args)
            else -> throw IllegalArgumentException("Unexpected command: $cmd")
        }
    }

    private fun handleNPCs(sender: CommandSender) {
        sender.sendMessage("${ArenaCommand.PREFIX}NPC System:")
        sender.sendMessage("  ${npcManager?.getNPCStatus()}")
        val npcCommands = listOf(
            ArenaCommand.TOGGLE_NPCS,
            ArenaCommand.SET_NPC_HEALTH,
            ArenaCommand.SET_NPC_DAMAGE,
            ArenaCommand.SET_NPC_COUNT,
            ArenaCommand.SET_NPC_ATTACK
        )
        val usageStr = npcCommands.joinToString(", ") { 
            "/arena ${it.primaryName}${if (it.usageParams.isNotEmpty()) " ${it.usageParams}" else ""}" 
        }
        sender.sendMessage("  Use: $usageStr")
    }

    private fun handleToggleNPCs(sender: CommandSender) {
        npcManager?.toggleNPCs()
        sender.sendMessage("${ArenaCommand.PREFIX}NPCs ${if (npcManager?.isNPCEnabled() == true) "enabled" else "disabled"}")
    }

    private fun handleSetNPCHealth(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(ArenaCommand.SET_NPC_HEALTH)}")
            sender.sendMessage("${ArenaCommand.PREFIX}Current NPC health: ${npcManager?.getNPCHealth()}")
            return
        }
        val newHealth = args[1].toDoubleOrNull()
        if (newHealth == null || newHealth <= 0) {
            sender.sendMessage("${ArenaCommand.PREFIX}Error: Health must be a positive number")
            return
        }
        npcManager?.setNPCHealth(newHealth)
        sender.sendMessage("${ArenaCommand.PREFIX}NPC health set to $newHealth")
    }

    private fun handleSetNPCDamage(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(ArenaCommand.SET_NPC_DAMAGE)}")
            sender.sendMessage("${ArenaCommand.PREFIX}Current NPC damage: ${npcManager?.getNPCDamage()}")
            return
        }
        val newDamage = args[1].toDoubleOrNull()
        if (newDamage == null || newDamage <= 0) {
            sender.sendMessage("${ArenaCommand.PREFIX}Error: Damage must be a positive number")
            return
        }
        npcManager?.setNPCDamage(newDamage)
        sender.sendMessage("${ArenaCommand.PREFIX}NPC damage set to $newDamage")
    }

    private fun handleSetNPCCount(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(ArenaCommand.SET_NPC_COUNT)}")
            sender.sendMessage("${ArenaCommand.PREFIX}Current NPC count: ${npcManager?.getNPCCount()}")
            return
        }
        val newCount = args[1].toIntOrNull()
        if (newCount == null || newCount < 0 || newCount > 4) {
            sender.sendMessage("${ArenaCommand.PREFIX}Error: Count must be between 0 and 4")
            return
        }
        npcManager?.setNPCCount(newCount)
        sender.sendMessage("${ArenaCommand.PREFIX}NPC count set to $newCount")
    }

    private fun handleSetNPCAttack(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("${ArenaCommand.PREFIX}${ArenaCommand.generateCommandUsage(ArenaCommand.SET_NPC_ATTACK)}")
            sender.sendMessage("${ArenaCommand.PREFIX}Current NPC attack type: ${npcManager?.getNPCAttackType()}")
            return
        }
        val attackType = when (args[1].lowercase()) {
            "arrow" -> NPCAttackType.SPECTRAL_ARROW
            "fireball" -> NPCAttackType.FIREBALL
            else -> {
                sender.sendMessage("${ArenaCommand.PREFIX}Error: Attack type must be 'arrow' or 'fireball'")
                return
            }
        }
        npcManager?.setNPCAttackType(attackType)
        sender.sendMessage("${ArenaCommand.PREFIX}NPC attack type set to $attackType (rebuild arena to apply)")
    }
}
