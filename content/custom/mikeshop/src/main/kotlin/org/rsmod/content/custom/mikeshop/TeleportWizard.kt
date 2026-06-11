package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * De Teleport Wizard op de GE-hub: praat met 'm -> keuzemenu -> teleporteert je naar een stad of
 * skilling-plek. Handige polish zodat reizen niet via een commando hoeft.
 */
class TeleportWizard @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HubNpcs.wizard) { teleportMenu() }
    }

    private suspend fun ProtectedAccess.teleportMenu() {
        val dest =
            choice5(
                "Grand Exchange (hub)",
                CoordGrid(0, 49, 54, 28, 30),
                "Lumbridge",
                CoordGrid(0, 50, 50, 21, 18),
                "Varrock",
                CoordGrid(0, 50, 53, 12, 30),
                "Falador",
                CoordGrid(0, 46, 52, 21, 50),
                "Mining site",
                CoordGrid(0, 50, 49, 28, 12),
                title = "Where would you like to teleport?",
            )
        telejump(dest)
        mes("The wizard teleports you away in a puff of smoke.")
    }
}
