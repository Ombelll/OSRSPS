package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Kleine dev-helpers. ::coords print je huidige tegel in beide formaten zodat we dingen
 * (winkels, spawns) op een exacte, gekozen plek kunnen neerzetten i.p.v. te gokken.
 */
class DevCommands @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("coords") {
            desc = "Show your current tile coordinates"
            cheat {
                val c = player.coords
                player.mes("Coords: x=${c.x}, z=${c.z}, level=${c.level}")
                player.mes(
                    "Grid: ${c.level}_${c.x / 64}_${c.z / 64}_${c.x % 64}_${c.z % 64} " +
                        "(level_mapX_mapZ_localX_localZ)"
                )
            }
        }
    }
}
