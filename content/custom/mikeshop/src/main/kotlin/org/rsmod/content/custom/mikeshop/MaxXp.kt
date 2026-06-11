package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.script.onCommand
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * ::maxxp -> zet ELKE skill (alle 23) op max XP. Door de XP-rate + 200M-cap komt elke skill op
 * level 99 met maximale ervaring. Handig om snel een max-account te testen.
 */
class MaxXp @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("maxxp") {
            desc = "Set every skill to max XP (level 99)"
            cheat {
                val maxXp = PlayerStatMap.toFineXP(200_000_000.0).toDouble()
                val all =
                    listOf(
                        stats.attack,
                        stats.defence,
                        stats.strength,
                        stats.hitpoints,
                        stats.ranged,
                        stats.prayer,
                        stats.magic,
                        stats.cooking,
                        stats.woodcutting,
                        stats.fletching,
                        stats.fishing,
                        stats.firemaking,
                        stats.crafting,
                        stats.smithing,
                        stats.mining,
                        stats.herblore,
                        stats.agility,
                        stats.thieving,
                        stats.slayer,
                        stats.farming,
                        stats.runecrafting,
                        stats.hunter,
                        stats.construction,
                    )
                for (stat in all) {
                    player.statAdvance(stat, maxXp)
                }
                player.mes("Maxed: all 23 skills set to max XP (level 99)!")
            }
        }
    }
}
