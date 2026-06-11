package org.rsmod.content.skills.extra

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.stat.StatType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AgilityLocs : LocReferences() {
    val logbalance = find("agilityarena_logbalance1")
    val ropebalance = find("agilityarena_ropebalance")
    val ledgebalance = find("agilityarena_ledgebalance")
}

/**
 * EXTRA SKILLS.
 *
 * - Echte Agility-mechaniek: balanceer over een obstakel -> Agility-XP.
 * - Universele trainer `::train <skill> [xp]` voor de skills die (nog) geen volledig
 *   systeem hebben (Herblore, Runecrafting, Farming, Hunter, Slayer, Construction, ...).
 *   Zo is elke skill trainbaar; volledige systemen kunnen later per stuk gebouwd worden.
 */
class Extra @Inject constructor() : PluginScript() {
    private val skillByName: Map<String, StatType> =
        mapOf(
            "attack" to stats.attack,
            "strength" to stats.strength,
            "defence" to stats.defence,
            "hitpoints" to stats.hitpoints,
            "ranged" to stats.ranged,
            "prayer" to stats.prayer,
            "magic" to stats.magic,
            "cooking" to stats.cooking,
            "woodcutting" to stats.woodcutting,
            "fletching" to stats.fletching,
            "fishing" to stats.fishing,
            "firemaking" to stats.firemaking,
            "crafting" to stats.crafting,
            "smithing" to stats.smithing,
            "mining" to stats.mining,
            "herblore" to stats.herblore,
            "agility" to stats.agility,
            "thieving" to stats.thieving,
            "slayer" to stats.slayer,
            "farming" to stats.farming,
            "runecrafting" to stats.runecrafting,
            "hunter" to stats.hunter,
            "construction" to stats.construction,
        )

    override fun ScriptContext.startup() {
        agilityObstacle(AgilityLocs.logbalance, 7.5, "You carefully balance across the log.")
        agilityObstacle(AgilityLocs.ropebalance, 7.5, "You walk across the tightrope.")
        agilityObstacle(AgilityLocs.ledgebalance, 8.0, "You shimmy along the ledge.")

        onCommand("train") {
            desc = "Train a skill: ::train <skill> [xp]"
            cheat {
                val name = args.getOrNull(0)?.lowercase()
                val stat = name?.let { skillByName[it] }
                if (stat == null) {
                    player.mes("Unknown skill. Use e.g. ::train herblore 50000")
                    player.mes("Skills: ${skillByName.keys.joinToString(", ")}")
                    return@cheat
                }
                val xp = args.getOrNull(1)?.toDoubleOrNull() ?: 10000.0
                player.statAdvance(stat, PlayerStatMap.toFineXP(xp).toDouble())
                player.mes("You gain ${xp.toInt()} $name experience.")
            }
        }
    }

    private fun ScriptContext.agilityObstacle(loc: LocType, xp: Double, msg: String) {
        onOpLoc1(loc) {
            statAdvance(stats.agility, PlayerStatMap.toFineXP(xp).toDouble())
            mes(msg)
        }
    }
}
