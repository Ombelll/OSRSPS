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

internal object GeneralStoreInvs : InvReferences() {
    val general_store = find("general_store")
}

internal object GeneralStoreObjs : ObjReferences() {
    // Basics te koop:
    val bread = find("bread")
    val tinderbox = find("tinderbox")
    val hammer = find("hammer")
    val knife = find("knife")
    val needle = find("needle")
    val chisel = find("chisel")
    val bronze_pickaxe = find("bronze_pickaxe")
    val net = find("net")
    val feather = find("feather")
    // Junk die de winkel van je OPKOOPT (count 0):
    val bones = find("bones")
    val big_bones = find("big_bones")
    val cow_hide = find("cow_hide")
    val raw_beef = find("raw_beef")
    val raw_chicken = find("raw_chicken")
    val copper_ore = find("copper_ore")
    val iron_ore = find("iron_ore")
    val coal = find("coal")
    val logs = find("logs")
    val oak_logs = find("oak_logs")
}

/** Bouwt de algemene winkel 'general_store': basics + opkoop van junk-loot. */
internal object GeneralStoreInvBuilder : InvBuilder() {
    init {
        build("general_store") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true

            // --- Te koop ---
            stock += stock(GeneralStoreObjs.bread, count = 30, restockCycles = 50)
            stock += stock(GeneralStoreObjs.tinderbox, count = 10, restockCycles = 100)
            stock += stock(GeneralStoreObjs.hammer, count = 10, restockCycles = 100)
            stock += stock(GeneralStoreObjs.knife, count = 10, restockCycles = 100)
            stock += stock(GeneralStoreObjs.needle, count = 10, restockCycles = 100)
            stock += stock(GeneralStoreObjs.chisel, count = 10, restockCycles = 100)
            stock += stock(GeneralStoreObjs.bronze_pickaxe, count = 10, restockCycles = 100)
            stock += stock(GeneralStoreObjs.net, count = 10, restockCycles = 100)
            stock += stock(GeneralStoreObjs.feather, count = 1000, restockCycles = 25)

            // --- Opkoop (count 0 = winkel betaalt je ervoor) ---
            stock += stock(GeneralStoreObjs.bones, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.big_bones, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.cow_hide, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.raw_beef, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.raw_chicken, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.copper_ore, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.iron_ore, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.coal, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.logs, count = 0, restockCycles = 50)
            stock += stock(GeneralStoreObjs.oak_logs, count = 0, restockCycles = 50)
        }
    }
}

/** ::store -> opent de algemene winkel (basics + verkoop je junk-loot voor coins). */
class GeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("store") {
            desc = "Open the general store (basics + sell junk loot)"
            cheat {
                shops.open(
                    player = player,
                    title = "General Store",
                    shopInv = GeneralStoreInvs.general_store,
                    buyPercentage = 100.0,
                    sellPercentage = 55.0,
                    changePercentage = 2.0,
                )
                player.mes("Welcome to the General Store!")
            }
        }
    }
}
