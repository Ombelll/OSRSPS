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
import org.rsmod.api.script.onPlayerLogin
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
        // Seed de leaderboard met je lifetime-stats bij login, zodat ::pktop ook spelers toont
        // die deze sessie nog niet gekilld hebben (en niet alleen kills sinds server-start).
        onPlayerLogin {
            if (player.pkKills > 0 || player.pkBestStreak > 0) {
                tracker.seed(player.displayName, player.pkKills, player.pkBestStreak)
            }
        }

        onCommand("pkpoints") {
            desc = "Show your PK kills, points and killstreak"
            cheat {
                val name = player.displayName
                val kd = if (player.pkDeaths == 0) player.pkKills.toDouble() else player.pkKills.toDouble() / player.pkDeaths
                player.mes("--- PK stats for $name ---")
                player.mes("Rank: ${PvpKillTracker.pkTitle(player.pkKills)}")
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

        onCommand("pktop") {
            desc = "Toon de PK-leaderboard (top killers + onderlinge score)"
            cheat {
                val board = tracker.leaderboard(10)
                if (board.isEmpty()) {
                    player.mes("Nog geen PK-kills sinds de server-start.")
                    return@cheat
                }
                player.mes("--- PK Leaderboard ---")
                board.forEachIndexed { i, e ->
                    player.mes(
                        "${i + 1}. ${e.name} [${PvpKillTracker.pkTitle(e.kills)}] - " +
                            "${e.kills} kills (best streak ${e.bestStreak})"
                    )
                }
                if (board.size >= 2) {
                    val a = board[0]
                    val b = board[1]
                    val (aWins, bWins) = tracker.headToHead(a.name, b.name)
                    player.mes("Head-to-head: ${a.name} $aWins - $bWins ${b.name}")
                }
            }
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
            3 -> combatGearMenu()
            4 -> cosmeticsMenu()
        }
    }

    private suspend fun ProtectedAccess.coinsMenu() {
        val pick =
            choice5(
                "250,000 coins (10 pts)",
                1,
                "1,000,000 coins (35 pts)",
                2,
                "5,000,000 coins (150 pts)",
                3,
                "10,000,000 coins (280 pts)",
                4,
                "Back",
                0,
                title = "PK Point Shop - Coins",
            )
        when (pick) {
            1 -> claimReward("250,000 coins", cost = 10, obj = objs.coins, count = 250_000)
            2 -> claimReward("1,000,000 coins", cost = 35, obj = objs.coins, count = 1_000_000)
            3 -> claimReward("5,000,000 coins", cost = 150, obj = objs.coins, count = 5_000_000)
            4 -> claimReward("10,000,000 coins", cost = 280, obj = objs.coins, count = 10_000_000)
        }
    }

    private suspend fun ProtectedAccess.combatGearMenu() {
        val pick =
            choice5(
                "Dragon claws (50 pts)",
                1,
                "Voidwaker (75 pts)",
                2,
                "Armadyl godsword (60 pts)",
                3,
                "Dragon warhammer (40 pts)",
                4,
                "Back",
                0,
                title = "PK Combat Gear - you have ${player.pkPoints} points",
            )
        when (pick) {
            1 -> claimReward("Dragon claws", cost = 50, obj = PkObjs.dragon_claws)
            2 -> claimReward("Voidwaker", cost = 75, obj = PkObjs.voidwaker)
            3 -> claimReward("Armadyl godsword", cost = 60, obj = PkObjs.armadyl_godsword)
            4 -> claimReward("Dragon warhammer", cost = 40, obj = PkObjs.dragon_warhammer)
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
