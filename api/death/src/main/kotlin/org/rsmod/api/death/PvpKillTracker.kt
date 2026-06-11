package org.rsmod.api.death

import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * Houdt killstreaks bij voor de PvP-wereld. Persistente kills/punten staan op player-varps;
 * alleen streaks blijven in-memory en resetten bij server-herstart.
 *
 * Wordt gevuld door [PlayerDeath] bij een player-vs-player kill.
 */
@Singleton
public class PvpKillTracker {
    public data class KillResult(val kills: Int, val points: Int, val streak: Int)

    private val streaks = ConcurrentHashMap<String, Int>()

    /** Registreert een kill: +punten (meer bij hogere streak) en verhoogt de streak. */
    public fun recordKill(killer: String, currentKills: Int, currentPoints: Int): KillResult {
        val key = killer.lowercase()
        val newStreak = (streaks[key] ?: 0) + 1
        streaks[key] = newStreak
        // Basis 4 punten + streak-bonus (cap 10 per kill).
        val gained = (4 + (newStreak - 1)).coerceAtMost(10)
        val newPoints = currentPoints + gained
        val newKills = currentKills + 1
        return KillResult(kills = newKills, points = newPoints, streak = newStreak)
    }

    /** Slachtoffer verliest z'n streak (punten blijven staan). Geeft de verloren streak terug. */
    public fun endStreak(victim: String): Int {
        val key = victim.lowercase()
        val lost = streaks[key] ?: 0
        streaks[key] = 0
        return lost
    }

    public fun streak(name: String): Int = streaks[name.lowercase()] ?: 0
}
