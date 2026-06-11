package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.shops.Shops
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Registreert het commando ::mikeshop dat onze eigen winkel opent.
 *
 * onCommand gebruikt een builder: je zet 'desc' (verplicht) en de 'cheat { }'-lus
 * is de code die draait. Binnen cheat { } is 'this' een Cheat, dus 'player' is direct
 * beschikbaar (en 'args' als je argumenten zou willen lezen).
 *
 * We openen via de Shops.open-overload ZONDER npc, met expliciete percentages:
 *   - buyPercentage   : prijs die je betaalt om te kopen (100% = volle waarde)
 *   - sellPercentage  : wat de winkel je geeft als je verkoopt
 *   - changePercentage: hoe snel de prijs meebeweegt met de voorraad
 */
class MikeShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("mikeshop") {
            desc = "Open Mike's custom shop"
            cheat {
                shops.open(
                    player = player,
                    title = "Mike's Custom Shop",
                    shopInv = mike_invs.mike_shop,
                    buyPercentage = 100.0,
                    sellPercentage = 80.0,
                    changePercentage = 2.0,
                )
                player.mes("Welcome to Mike's Custom Shop!")
            }
        }
    }
}
