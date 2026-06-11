package org.rsmod.api.death

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.jingles
import org.rsmod.api.config.refs.midis
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.deathResetTimers
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.realm.Realm
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.type.stat.StatTypeList

@Singleton
public class PlayerDeath
@Inject
constructor(
    private val statTypes: StatTypeList,
    private val realm: Realm,
    private val players: PlayerList,
    private val pvpKills: PvpKillTracker,
) {
    private var Player.specialAttackType by intVarp(varps.sa_attack)
    private var Player.pkPoints by intVarp(varps.mike_pk_points)
    private var Player.pkKills by intVarp(varps.mike_pk_kills)

    // PK-punten/killstreaks gelden alleen op de PvP-wereld (RSMOD_WORLD=2).
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"

    public suspend fun death(access: ProtectedAccess) {
        access.deathSequence()
    }

    private suspend fun ProtectedAccess.deathSequence() {
        // Respawn op de realm-respawnCoord (bv. Edgeville op de PvP-wereld) i.p.v. hardcoded.
        val respawn = realm.config.respawnCoord
        val randomRespawn = mapFindSquareLineOfWalk(respawn, minRadius = 0, maxRadius = 2)
        if (pvpWorld) {
            awardPvpKill()
        }
        stopAction()
        delay(2)
        anim(seqs.human_death)
        delay(4)
        combatClearQueue()
        clearQueue(queues.death)
        midiSong(midis.stop_music)
        midiJingle(jingles.death_jingle_2)
        mes("Oh dear, you are dead!")
        telejump(randomRespawn ?: respawn)
        resetAnim()
        // TODO: Drop death invs, etc.
        resetPlayerState(statTypes)
        restoreToplevelTabs(
            components.toplevel_target_pvp_icons,
            components.toplevel_target_side1,
            components.toplevel_target_side2,
            components.toplevel_target_side4,
            components.toplevel_target_side5,
            components.toplevel_target_side6,
            components.toplevel_target_side9,
            components.toplevel_target_side8,
            components.toplevel_target_side7,
            components.toplevel_target_side10,
            components.toplevel_target_side11,
            components.toplevel_target_side12,
            components.toplevel_target_side13,
        )
    }

    /** Kent PK-punten toe aan de killer en beeindigt de streak van het slachtoffer. */
    private fun ProtectedAccess.awardPvpKill() {
        val victim = player
        val killer = findHero() ?: return
        if (killer === victim) {
            return
        }
        val victimStreakLost = pvpKills.endStreak(victim.displayName)
        val result = pvpKills.recordKill(killer.displayName, killer.pkKills, killer.pkPoints)
        killer.pkKills = result.kills
        killer.pkPoints = result.points
        killer.mes(
            "You killed ${victim.displayName}! Streak: ${result.streak}. " +
                "PK points: ${result.points} (::pkspend to spend them)."
        )
        victim.mes(
            "You were killed by ${killer.displayName}." +
                if (victimStreakLost > 1) " Your streak of $victimStreakLost is over!" else ""
        )
        // Broadcast bij streak-mijlpalen (5, 10, 15, ...).
        if (result.streak >= 5 && result.streak % 5 == 0) {
            for (online in players) {
                online.mes(
                    "[PK] ${killer.displayName} is on a ${result.streak} killstreak! " +
                        "Take them down for bonus glory!"
                )
            }
        }
    }

    private fun ProtectedAccess.resetPlayerState(stats: StatTypeList) {
        player.disablePrayers()
        player.deathResetTimers()

        player.specialAttackType = 0
        player.skullIcon = null

        rebuildAppearance()

        camReset()
        statRestoreAll(stats.values)
        minimapReset()
    }
}
