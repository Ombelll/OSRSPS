package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.PvpKillTracker
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * PK-punten (PvP-wereld): elke player-kill geeft punten (meer bij hogere killstreak; zie
 * PvpKillTracker + PlayerDeath). Hier check je je stats en geef je punten uit.
 *
 *  - ::pkpoints : toont je kills / punten / huidige streak
 *  - ::pkspend  : puntenwinkel (coins, potion-packs, dragon claws)
 *
 * Kills en punten zijn persistent; killstreak is in-memory per server-sessie.
 */
class PkPoints
@Inject
constructor(
    private val tracker: PvpKillTracker,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private var Player.pkPoints by intVarp(varps.mike_pk_points)
    private var Player.pkKills by intVarp(varps.mike_pk_kills)

    override fun ScriptContext.startup() {
        onCommand("pkpoints") {
            desc = "Show your PK kills, points and killstreak"
            cheat {
                val name = player.displayName
                player.mes("--- PK stats for $name ---")
                player.mes("Kills: ${player.pkKills}")
                player.mes("Killstreak: ${tracker.streak(name)}")
                player.mes("PK points: ${player.pkPoints} (spend with ::pkspend)")
            }
        }

        onCommand("pkspend") {
            desc = "Spend PK points on rewards"
            cheat { protectedAccess.launch(player) { spendMenu() } }
        }
    }

    private suspend fun ProtectedAccess.spendMenu() {
        val balance = player.pkPoints
        val pick =
            choice5(
                "250,000 coins (10 pts)",
                1,
                "1,000,000 coins (35 pts)",
                2,
                "5x super combat potion (15 pts)",
                3,
                "Dragon claws (50 pts)",
                4,
                "Nothing",
                0,
                title = "PK Point Shop - you have $balance points",
            )
        val cost =
            when (pick) {
                1 -> 10
                2 -> 35
                3 -> 15
                4 -> 50
                else -> return
            }
        if (player.pkPoints < cost) {
            mes("Not enough PK points (need $cost, you have $balance). Get killing!")
            return
        }
        player.pkPoints -= cost
        when (pick) {
            1 -> player.invAdd(player.inv, objs.coins, 250_000, strict = false)
            2 -> player.invAdd(player.inv, objs.coins, 1_000_000, strict = false)
            3 -> player.invAdd(player.inv, PkObjs.super_combat, 5, strict = false)
            4 -> player.invAdd(player.inv, PkObjs.dragon_claws, 1, strict = false)
        }
        mes("Reward claimed! Remaining PK points: ${player.pkPoints}.")
    }
}
