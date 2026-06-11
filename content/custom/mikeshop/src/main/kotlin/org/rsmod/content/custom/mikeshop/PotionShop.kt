package org.rsmod.content.custom.mikeshop

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object PotionShopInvs : InvReferences() {
    val potion_shop = find("potion_shop")
}

internal object PotionShopObjs : ObjReferences() {
    // Afgewerkte potions:
    val attack = find("3dose1attack")
    val strength = find("3dose1strength")
    val restore = find("3dosestatrestore")
    val prayer = find("3doseprayerrestore")
    val super_combat = find("4dose2combat")
    // Top-food:
    val lobster = find("lobster")
    val swordfish = find("swordfish")
    val shark = find("shark")
    // Herblore-secondaries:
    val eye_of_newt = find("eye_of_newt")
    val limpwurt_root = find("limpwurt_root")
    val red_spiders_eggs = find("red_spiders_eggs")
}

internal object PotionShopInvBuilder : InvBuilder() {
    init {
        build("potion_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true

            // --- Potions ---
            stock += stock(PotionShopObjs.attack, count = 100, restockCycles = 50)
            stock += stock(PotionShopObjs.strength, count = 100, restockCycles = 50)
            stock += stock(PotionShopObjs.restore, count = 100, restockCycles = 50)
            stock += stock(PotionShopObjs.prayer, count = 100, restockCycles = 50)
            stock += stock(PotionShopObjs.super_combat, count = 50, restockCycles = 100)

            // --- Food ---
            stock += stock(PotionShopObjs.lobster, count = 200, restockCycles = 25)
            stock += stock(PotionShopObjs.swordfish, count = 150, restockCycles = 25)
            stock += stock(PotionShopObjs.shark, count = 100, restockCycles = 50)

            // --- Secondaries (voor eigen brouwen) ---
            stock += stock(PotionShopObjs.eye_of_newt, count = 500, restockCycles = 25)
            stock += stock(PotionShopObjs.limpwurt_root, count = 200, restockCycles = 50)
            stock += stock(PotionShopObjs.red_spiders_eggs, count = 200, restockCycles = 50)
        }
    }
}

/** ::potionshop -> opent de potion- & food-winkel (combat-supplies). */
class PotionShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("potionshop") {
            desc = "Open the potion & food shop"
            cheat {
                shops.open(
                    player = player,
                    title = "Potion & Food Shop",
                    shopInv = PotionShopInvs.potion_shop,
                    buyPercentage = 100.0,
                    sellPercentage = 60.0,
                    changePercentage = 1.0,
                )
                player.mes("Welcome to the Potion & Food Shop!")
            }
        }
    }
}
