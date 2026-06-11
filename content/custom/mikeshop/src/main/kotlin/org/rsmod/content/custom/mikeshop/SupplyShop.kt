package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.shops.Shops
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * ::supplyshop -> opent de voorraadwinkel met runes, kruid-secondaries, vials en vis-tools,
 * zodat Magic, Runecrafting, Herblore en Fishing makkelijk te trainen zijn.
 */
class SupplyShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("supplyshop") {
            desc = "Open the supply shop (runes, herbs, fishing gear)"
            cheat {
                shops.open(
                    player = player,
                    title = "Supply Shop",
                    shopInv = SupplyShopInvs.supply_shop,
                    buyPercentage = 100.0,
                    sellPercentage = 60.0,
                    changePercentage = 1.0,
                )
                player.mes("Welcome to the Supply Shop!")
            }
        }
    }
}
