package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** ::heal -> herstelt HP en prayer points volledig (handig tussen boss-fights). */
class ExtraCommands @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("heal") {
            desc = "Fully restore HP and prayer points"
            cheat {
                protectedAccess.launch(player) {
                    statHeal(stats.hitpoints, constant = 999, percent = 0)
                    statHeal(stats.prayer, constant = 999, percent = 0)
                    mes("You feel completely restored.")
                }
            }
        }
    }
}
