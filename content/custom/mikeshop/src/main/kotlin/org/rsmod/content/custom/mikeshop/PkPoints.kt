package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import java.util.Locale
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
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * PK-punten (PvP-wereld): elke player-kill geeft punten (meer bij hogere killstreak; zie
 * PvpKillTracker + PlayerDeath). Hier check je je stats en geef je punten uit.
 *
 *  - ::pkpoints : toont je kills / deaths / KD / punten / huidige streak
 *  - ::pkspend  : puntenwinkel (coins, potion-packs, dragon claws, cosmetics)
 *
 * Kills, deaths, punten en beste streak zijn persistent; huidige killstreak is per server-sessie.
 */
class PkPoints
@Inject
constructor(
    private val tracker: PvpKillTracker,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private var Player.pkPoints by intVarp(varps.mike_pk_points)
    private var Player.pkKills by intVarp(varps.mike_pk_kills)
    private var Player.pkDeaths by intVarp(varps.mike_pk_deaths)
    private var Player.pkBestStreak by intVarp(varps.mike_pk_best_streak)

    override fun ScriptContext.startup() {
        onCommand("pkpoints") {
            desc = "Show your PK kills, points and killstreak"
            cheat {
                val name = player.displayName
                val kd = if (player.pkDeaths == 0) player.pkKills.toDouble() else player.pkKills.toDouble() / player.pkDeaths
                player.mes("--- PK stats for $name ---")
                player.mes("Kills: ${player.pkKills}")
                player.mes("Deaths: ${player.pkDeaths} (KD: ${String.format(Locale.ROOT, "%.2f", kd)})")
                player.mes(
                    "Killstreak: ${tracker.streak(name)} " +
                        "(best: ${maxOf(player.pkBestStreak, tracker.bestStreak(name))})"
                )
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
                "Coins",
                1,
                "Supplies",
                2,
                "Combat gear",
                3,
                "Cosmetics",
                4,
                "Nothing",
                0,
                title = "PK Point Shop - you have $balance points",
            )
        when (pick) {
            1 -> coinsMenu()
            2 -> claimReward("5x super combat potion", cost = 15, obj = PkObjs.super_combat, count = 5)
            3 -> claimReward("Dragon claws", cost = 50, obj = PkObjs.dragon_claws)
            4 -> cosmeticsMenu()
        }
    }

    private suspend fun ProtectedAccess.coinsMenu() {
        val pick =
            choice3(
                "250,000 coins (10 pts)",
                1,
                "1,000,000 coins (35 pts)",
                2,
                "Back",
                0,
                title = "PK Point Shop - Coins",
            )
        when (pick) {
            1 -> claimReward("250,000 coins", cost = 10, obj = objs.coins, count = 250_000)
            2 -> claimReward("1,000,000 coins", cost = 35, obj = objs.coins, count = 1_000_000)
        }
    }

    private suspend fun ProtectedAccess.cosmeticsMenu() {
        val pick =
            choice5(
                "Santa hat (75 pts)",
                1,
                "Robin hood hat (100 pts)",
                2,
                "Red halloween mask (125 pts)",
                3,
                "Black partyhat (200 pts)",
                4,
                "Back",
                0,
                title = "PK Cosmetics - you have ${player.pkPoints} points",
            )
        when (pick) {
            1 -> claimReward("Santa hat", cost = 75, obj = PkObjs.santa_hat)
            2 -> claimReward("Robin hood hat", cost = 100, obj = PkObjs.robinhoodhat)
            3 -> claimReward("Red halloween mask", cost = 125, obj = PkObjs.halloweenmask_red)
            4 -> claimReward("Black partyhat", cost = 200, obj = PkObjs.black_partyhat)
        }
    }

    private fun ProtectedAccess.claimReward(label: String, cost: Int, obj: ObjType, count: Int = 1) {
        val balance = player.pkPoints
        if (balance < cost) {
            mes("Not enough PK points for $label (need $cost, you have $balance).")
            return
        }
        val add = player.invAdd(player.inv, obj, count, strict = true)
        if (!add.success) {
            mes("Not enough inventory space for $label.")
            return
        }
        player.pkPoints = balance - cost
        mes("$label claimed! Remaining PK points: ${player.pkPoints}.")
    }
}
