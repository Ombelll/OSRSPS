package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.death.PvpKillTracker
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Bounty-target (W2 PvP): je krijgt een aangewezen tegenstander; die killen geeft bonus PK-punten
 * (zie [PvpKillTracker.recordKill]) en een server-broadcast (zie PlayerDeath kill-feed). Bij login
 * krijg je automatisch een target als er een andere speler online is; met `::bounty` zie je 'm of
 * wijs je een nieuwe toe (als de oude offline/al geclaimd is).
 */
class BountyTarget
@Inject
constructor(private val tracker: PvpKillTracker, private val players: PlayerList) : PluginScript() {
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"

    override fun ScriptContext.startup() {
        if (!pvpWorld) {
            return
        }
        onPlayerLogin {
            if (tracker.bountyTarget(player.displayName) == null) {
                pickTarget(player)?.let { tracker.setBounty(player.displayName, it.displayName) }
            }
        }
        onCommand("bounty") {
            desc = "Toon of krijg je PK bounty-target (bonus PK-punten + broadcast bij een kill)"
            cheat {
                val current = tracker.bountyTarget(player.displayName)
                val stillOnline =
                    current != null &&
                        players.any { it.displayName.equals(current, ignoreCase = true) }
                if (current != null && stillOnline) {
                    player.mes("Je bounty-target: $current. Kill 'm voor bonus PK-punten!")
                    return@cheat
                }
                val target = pickTarget(player)
                if (target == null) {
                    player.mes("Geen andere spelers online voor een bounty.")
                } else {
                    tracker.setBounty(player.displayName, target.displayName)
                    player.mes("Nieuw bounty-target: ${target.displayName}. Kill 'm voor bonus PK-punten!")
                }
            }
        }
    }

    private fun pickTarget(self: Player): Player? =
        players.filter { it !== self }.randomOrNull()
}
