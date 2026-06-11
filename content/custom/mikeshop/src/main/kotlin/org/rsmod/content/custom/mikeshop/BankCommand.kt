package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.interfaces.bank.openBank
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** ::bank -> opent je bankkluis (handig om loot/gear op te slaan tijdens het testen). */
class BankCommand @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("bank") {
            desc = "Open your bank"
            cheat {
                player.openBank(eventBus)
                player.mes("You open your bank.")
            }
        }
    }
}
