package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.account.AccountManager
import org.rsmod.api.config.refs.queues
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Periodieke autosave: elke ~60s wordt elke online speler volledig opgeslagen, zodat voortgang
 * (PK-punten, gear, settings, keybinds) niet verloren gaat bij een server-herstart of crash.
 * Voorheen werd alleen bij een clean logout opgeslagen. Een volledige save voorkomt ook
 * "partial save"-problemen na een force-kill.
 *
 * Mechanisme: een re-armende soft-queue (onderbreekt de speler niet).
 */
class AutoSave @Inject constructor(private val accounts: AccountManager) : PluginScript() {
    private val intervalCycles = 100 // ~60 seconden (0.6s per game-tick)

    override fun ScriptContext.startup() {
        // TIJDELIJK UITGESCHAKELD om te testen of de autosave de shop-modal sluit.
        // (Soft-queue zou de modal niet moeten sluiten, maar we sluiten dit eerst uit.)
        if (DISABLED) return
        onPlayerLogin { player.softQueue(queues.generic_queue9, intervalCycles) }
        onPlayerSoftQueue(queues.generic_queue9) {
            accounts.save(player) {}
            player.softQueue(queues.generic_queue9, intervalCycles)
        }
    }

    private companion object {
        private const val DISABLED = true
    }
}
