package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Gear-preset: sla je huidige uitrusting + inventory op (::saveset) en pak 'm in één keer terug
 * (::loadset). Snel re-gearen na een death zonder alles los uit de shops te halen. In-memory per
 * server-sessie, gekoppeld aan je display-naam (blijft na relog tot de server herstart).
 */
class GearPreset
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    private class Preset(
        val worn: List<Pair<Int, InvObj>>,
        val inv: List<Pair<Int, InvObj>>,
    )

    private val presets = HashMap<String, Preset>()

    override fun ScriptContext.startup() {
        onCommand("saveset") {
            desc = "Sla je huidige uitrusting + inventory op als preset (::loadset om te laden)"
            cheat {
                val worn = ArrayList<Pair<Int, InvObj>>()
                for (slot in player.worn.indices) {
                    player.worn[slot]?.let { worn.add(slot to it) }
                }
                val inv = ArrayList<Pair<Int, InvObj>>()
                for (slot in player.inv.indices) {
                    player.inv[slot]?.let { inv.add(slot to it) }
                }
                presets[player.displayName.lowercase()] = Preset(worn, inv)
                player.mes(
                    "Preset opgeslagen (${worn.size} gedragen + ${inv.size} inventory). Laad met ::loadset."
                )
            }
        }

        onCommand("loadset") {
            desc = "Herstel je opgeslagen uitrusting + inventory"
            cheat {
                val preset = presets[player.displayName.lowercase()]
                if (preset == null) {
                    player.mes("Geen preset opgeslagen. Sla er een op met ::saveset.")
                    return@cheat
                }
                protectedAccess.launch(player) {
                    for (slot in player.worn.indices) {
                        player.worn[slot] = null
                    }
                    for (slot in player.inv.indices) {
                        player.inv[slot] = null
                    }
                    for ((slot, obj) in preset.worn) {
                        player.worn[slot] = obj
                    }
                    for ((slot, obj) in preset.inv) {
                        player.inv[slot] = obj
                    }
                    rebuildAppearance()
                    player.mes("Preset geladen.")
                }
            }
        }
    }
}
