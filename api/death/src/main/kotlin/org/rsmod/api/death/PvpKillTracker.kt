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
    )

    private val streaks = ConcurrentHashMap<String, Int>()
    private val bestStreaks = ConcurrentHashMap<String, Int>()
    private val farm = ConcurrentHashMap<String, FarmEntry>()

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
        val gained =
            when {
                entry.count == 1 -> base + milestoneBonus
                entry.count <= 3 -> max(1, base / 2)
                else -> 1
            }
        return KillResult(
            kills = currentKills + 1,
            points = currentPoints + gained,
            streak = newStreak,
            bestStreak = best,
            gained = gained,
            farmed = farmed,
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

    private companion object {
        private const val FARM_DECAY_MS = 120_000L
    }
}
