package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private val PK_READY_TILE: CoordGrid = CoordGrid(0, 48, 54, 15, 40)

class PvpTools @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    private val pvpWorld = System.getenv("RSMOD_WORLD") == "2"

    override fun ScriptContext.startup() {
        onCommand("pvpops") {
            desc = "Refresh player right-click options for PvP testing"
            cheat {
                player.sendPvpPlayerOps(pvpWorld)
                if (pvpWorld) {
                    player.mes("PvP options refreshed. Right-click players should show Attack.")
                } else {
                    player.mes("PvP options refreshed for safe world. Attack is only shown on World 2.")
                }
            }
        }

        onCommand("pkready") {
            desc = "W2 test setup: refresh PvP options and move to Edgeville PK hub"
            cheat {
                player.sendPvpPlayerOps(pvpWorld)
                if (!pvpWorld) {
                    player.mes("Use World 2 for PvP. Your W1 player options were left safe.")
                    return@cheat
                }
                protectedAccess.launch(player) {
                    telejump(PK_READY_TILE)
                    mes("PK ready: Edgeville hub, Attack option refreshed.")
                }
            }
        }
    }
}

private fun Player.sendPvpPlayerOps(pvpWorld: Boolean) {
    MiscOutput.setPlayerOp(this, slot = 2, op = if (pvpWorld) "Attack" else null, priority = pvpWorld)
    MiscOutput.setPlayerOp(this, slot = 3, op = "Follow")
    MiscOutput.setPlayerOp(this, slot = 4, op = "Trade with")
    MiscOutput.setPlayerOp(this, slot = 5, op = null)
    MiscOutput.setPlayerOp(this, slot = 8, op = "Report")
}
