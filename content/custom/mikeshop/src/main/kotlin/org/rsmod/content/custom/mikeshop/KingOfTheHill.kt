package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * King of the Hill (PvP-wereld): claim de heuvel in de Edgeville-wild door er als enige in te
 * staan. Zolang je 'm alleen bezet (en er minstens een tegenstander online is), krijg je elke
 * [KOTH_INTERVAL] ticks [KOTH_REWARD] PK-punten. Staat er een tegenstander in de zone, dan is 'ie
 * 'contested' - sla 'm eruit om te scoren. ::koth teleporteert naar de heuvel.
 */
class KingOfTheHill
@Inject
constructor(
    private val players: PlayerList,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"
    private var Player.pkPoints by intVarp(varps.mike_pk_points)
    private var currentKing: String? = null
    private var holdIntervals = 0

    override fun ScriptContext.startup() {
        if (pvpWorld) {
            onPlayerLogin { player.softTimer(timers.koth_tick, KOTH_INTERVAL) }
        }
        onPlayerSoftTimer(timers.koth_tick) { player.kothTick() }

        onCommand("koth") {
            desc = "Teleporteer naar de King of the Hill-zone in de Wilderness"
            cheat {
                if (!pvpWorld) {
                    player.mes("King of the Hill is alleen op World 2.")
                    return@cheat
                }
                protectedAccess.launch(player) {
                    telejump(KOTH_CENTER)
                    mes("King of the Hill: blijf als enige op de heuvel staan voor PK-punten!")
                }
            }
        }
    }

    private fun Player.kothTick() {
        if (!pvpWorld || !inKothZone()) {
            if (currentKing == displayName) {
                currentKing = null
                holdIntervals = 0
            }
            return
        }
        // Anti-AFK-farm: zonder tegenstander online geen punten.
        if (players.count() < 2) {
            return
        }
        val contesters = players.count { it !== this && it.inKothZone() }
        if (contesters > 0) {
            mes("The hill is CONTESTED - kill them off to claim it!")
            if (currentKing == displayName) {
                currentKing = null
            }
            return
        }
        if (currentKing != displayName) {
            currentKing = displayName
            holdIntervals = 0
            for (online in players) {
                online.mes("[KOTH] ${displayName} has claimed the hill! Take it from them.")
            }
        }
        holdIntervals++
        pkPoints += KOTH_REWARD
        mes("You hold the hill! +$KOTH_REWARD PK points.")
        if (holdIntervals % 10 == 0) {
            val minutes = holdIntervals / 10
            for (online in players) {
                online.mes("[KOTH] ${displayName} has held the hill for $minutes minute(s)! Dethrone them!")
            }
        }
    }

    private fun Player.inKothZone(): Boolean {
        val c = coords
        return c.level == 0 && c.x in KOTH_X && c.z in KOTH_Z
    }

    private companion object {
        private const val KOTH_INTERVAL = 10 // elke 10 ticks (~6s)
        private const val KOTH_REWARD = 1 // +1 PK-punt per interval (~10/min bij bezit)
        private val KOTH_X = 3095..3101
        private val KOTH_Z = 3540..3546
        private val KOTH_CENTER = CoordGrid(3098, 3543)
    }
}
