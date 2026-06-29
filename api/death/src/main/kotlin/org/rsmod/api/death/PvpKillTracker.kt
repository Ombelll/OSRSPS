package org.rsmod.api.death

import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Houdt killstreaks bij voor de PvP-wereld. Persistente kills/punten staan op player-varps;
 * streaks, beste-streak en anti-farm-state blijven in-memory en resetten bij server-herstart.
 *
 * Wordt gevuld door [PlayerDeath] bij een player-vs-player kill.
 */
@Singleton
public class PvpKillTracker {
    public data class KillResult(
        val kills: Int,
        val points: Int,
        val streak: Int,
        val bestStreak: Int,
        val gained: Int,
        val farmed: Boolean,
        val bountyClaimed: Boolean,
    )

    public data class LeaderEntry(val name: String, val kills: Int, val bestStreak: Int)

    private val streaks = ConcurrentHashMap<String, Int>()
    private val bestStreaks = ConcurrentHashMap<String, Int>()
    private val farm = ConcurrentHashMap<String, FarmEntry>()

    // Leaderboard-state (in-memory, vanaf server-start): kill-totalen + display-naam per key.
    private val totalKills = ConcurrentHashMap<String, Int>()
    private val displayNames = ConcurrentHashMap<String, String>()

    // Head-to-head: "killerKey>victimKey" -> hoe vaak killer dit slachtoffer versloeg.
    private val h2h = ConcurrentHashMap<String, Int>()

    // Bounty-target: killer-key (lowercase) -> target display-naam. Killen van je target geeft bonus.
    private val bountyTargets = ConcurrentHashMap<String, String>()

    public fun setBounty(player: String, target: String) {
        bountyTargets[player.lowercase()] = target
    }

    public fun bountyTarget(player: String): String? = bountyTargets[player.lowercase()]

    public fun clearBounty(player: String) {
        bountyTargets.remove(player.lowercase())
    }

    private class FarmEntry(var count: Int, var lastMs: Long)

    /**
     * Registreert een kill. Punten = basis (4 + streak-1, cap 10) + streak-milestone-bonus
     * (elke 5e streak). Anti-farm: dezelfde tegenstander snel herhaald killen geeft diminishing
     * returns; de teller dooft na 2 minuten zonder kill op dat doelwit.
     */
    public fun recordKill(
        killer: String,
        victim: String,
        currentKills: Int,
        currentPoints: Int,
    ): KillResult {
        val key = killer.lowercase()
        val newStreak = (streaks[key] ?: 0) + 1
        streaks[key] = newStreak
        val best = max(bestStreaks[key] ?: 0, newStreak)
        bestStreaks[key] = best

        // Leaderboard + head-to-head bijwerken.
        val vkey = victim.lowercase()
        totalKills[key] = currentKills + 1
        displayNames[key] = killer
        displayNames[vkey] = victim
        h2h["$key>$vkey"] = (h2h["$key>$vkey"] ?: 0) + 1

        val farmKey = "$key>${victim.lowercase()}"
        val now = System.currentTimeMillis()
        val entry = farm.getOrPut(farmKey) { FarmEntry(0, now) }
        if (now - entry.lastMs > FARM_DECAY_MS) {
            entry.count = 0
        }
        entry.count += 1
        entry.lastMs = now

        val base = (4 + (newStreak - 1)).coerceAtMost(10)
        val milestoneBonus = if (newStreak % 5 == 0) newStreak else 0
        val farmed = entry.count > 1
        val baseGained =
            when {
                entry.count == 1 -> base + milestoneBonus
                entry.count <= 3 -> max(1, base / 2)
                else -> 1
            }

        // Bounty: was dit slachtoffer het aangewezen target van de killer? -> bonus + claim wissen.
        val target = bountyTargets[key]
        val bountyClaimed = target != null && target.equals(victim, ignoreCase = true)
        if (bountyClaimed) {
            bountyTargets.remove(key)
        }
        val gained = baseGained + if (bountyClaimed) BOUNTY_BONUS else 0

        return KillResult(
            kills = currentKills + 1,
            points = currentPoints + gained,
            streak = newStreak,
            bestStreak = best,
            gained = gained,
            farmed = farmed,
            bountyClaimed = bountyClaimed,
        )
    }

    /** Slachtoffer verliest z'n streak (punten blijven staan). Geeft de verloren streak terug. */
    public fun endStreak(victim: String): Int {
        val key = victim.lowercase()
        val lost = streaks[key] ?: 0
        streaks[key] = 0
        return lost
    }

    public fun streak(name: String): Int = streaks[name.lowercase()] ?: 0

    public fun bestStreak(name: String): Int = bestStreaks[name.lowercase()] ?: 0

    /**
     * Zet de persistente stats van een speler in de leaderboard (bij login). Zo telt ook iemand
     * mee die deze sessie nog niet gekilld heeft, met z'n volledige lifetime kill-count.
     */
    public fun seed(name: String, kills: Int, bestStreak: Int) {
        val key = name.lowercase()
        displayNames[key] = name
        totalKills[key] = max(totalKills[key] ?: 0, kills)
        bestStreaks[key] = max(bestStreaks[key] ?: 0, bestStreak)
    }

    /** Top-killers sinds server-start, gesorteerd op kills (aflopend). */
    public fun leaderboard(limit: Int = 10): List<LeaderEntry> =
        totalKills.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { LeaderEntry(displayNames[it.key] ?: it.key, it.value, bestStreaks[it.key] ?: 0) }

    /** Onderlinge score: (hoe vaak a -> b versloeg, hoe vaak b -> a versloeg). */
    public fun headToHead(a: String, b: String): Pair<Int, Int> {
        val ak = a.lowercase()
        val bk = b.lowercase()
        return (h2h["$ak>$bk"] ?: 0) to (h2h["$bk>$ak"] ?: 0)
    }

    public companion object {
        private const val FARM_DECAY_MS = 120_000L
        private const val BOUNTY_BONUS = 10

        /** PK-rang/titel afgeleid van het totaal aantal kills. */
        public fun pkTitle(kills: Int): String =
            when {
                kills >= 300 -> "Legend"
                kills >= 150 -> "Warlord"
                kills >= 75 -> "Maniac"
                kills >= 25 -> "Brutal"
                kills >= 5 -> "Killer"
                else -> "Novice"
            }
    }
}
