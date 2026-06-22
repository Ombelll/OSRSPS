package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.account.AccountManager
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Autosave UITGESCHAKELD.
 *
 * Een periodieke autosave via een re-armende soft-queue (onPlayerSoftQueue) bleek de open shop-/
 * bank-modal te sluiten: soft-queues worden in `processQueues` afgehandeld, wat de modal sluit -
 * ook als de save zelf wordt overgeslagen. De modal-gate hielp niet, dus deze aanpak is onbruikbaar.
 *
 * Niet nodig voor normale voortgang: een nette logout slaat al volledig op via de engine
 * (PlayerLogoutProcess -> AccountRegistry -> AccountManager.save). Verlies treedt alleen op bij een
 * force-kill/crash van de server terwijl je online bent; dat los je op met een nette herstart i.p.v.
 * een autosave die de UI breekt.
 */
class AutoSave @Inject constructor(@Suppress("unused") private val accounts: AccountManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        // Bewust leeg: geen autosave-queue meer (zie klassendoc).
    }
}
