package org.rsmod.content.custom.mikeshop

import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

typealias mike_invs = MikeShopInvs

/** Verwijzing naar onze winkel-voorraad. */
object MikeShopInvs : InvReferences() {
    val mike_shop = find("mike_shop")
}

/** Alle items die de winkel verkoopt of opkoopt (bestaande cache-items). */
object MikeShopObjs : ObjReferences() {
    // Verkoopbare loot (count 0 = winkel koopt het van je):
    val cow_hide = find("cow_hide")
    val raw_beef = find("raw_beef")
    val raw_chicken = find("raw_chicken")
    val feather = find("feather")
    val bones = find("bones")
    val big_bones = find("big_bones")
    val limpwurt_root = find("limpwurt_root")
    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val iron_ore = find("iron_ore")
    val coal = find("coal")
    val silver_ore = find("silver_ore")
    val gold_ore = find("gold_ore")
    val mithril_ore = find("mithril_ore")
    val adamantite_ore = find("adamantite_ore")
    val runite_ore = find("runite_ore")

    // Tools:
    val bronze_pickaxe = find("bronze_pickaxe")
    val adamant_pickaxe = find("adamant_pickaxe")
    val rune_pickaxe = find("rune_pickaxe")
    val hammer = find("hammer")
    val tinderbox = find("tinderbox")

    // Wapens (gear-tiers):
    val bronze_sword = find("bronze_sword")
    val iron_sword = find("iron_sword")
    val bronze_scimitar = find("bronze_scimitar")
    val iron_scimitar = find("iron_scimitar")
    val steel_scimitar = find("steel_scimitar")
    val mithril_scimitar = find("mithril_scimitar")
    val adamant_scimitar = find("adamant_scimitar")
    val rune_scimitar = find("rune_scimitar")

    // Armour:
    val leather_armour = find("leather_armour")
    val wooden_shield = find("wooden_shield")
    val bronze_platebody = find("bronze_platebody")
    val iron_platebody = find("iron_platebody")
    val steel_platebody = find("steel_platebody")
    val rune_platebody = find("rune_platebody")
    val iron_full_helm = find("iron_full_helm")
    val rune_full_helm = find("rune_full_helm")
    val bronze_kiteshield = find("bronze_kiteshield")
    val rune_kiteshield = find("rune_kiteshield")

    // Food:
    val bread = find("bread")
    val cooked_meat = find("cooked_meat")
    val cooked_chicken = find("cooked_chicken")
    val trout = find("trout")
    val salmon = find("salmon")
    val lobster = find("lobster")
    val shark = find("shark")
}

/** Bouwt de winkelvoorraad 'mike_shop' (autoSize past zich aan het aantal items aan). */
internal object MikeShopInvBuilder : InvBuilder() {
    init {
        build("mike_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true

            // --- Verkoopbaar (count 0): jouw mining/combat-loot wordt geld ---
            stock += stock(MikeShopObjs.cow_hide, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.raw_beef, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.raw_chicken, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.feather, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.bones, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.big_bones, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.limpwurt_root, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.copper_ore, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.tin_ore, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.iron_ore, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.coal, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.silver_ore, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.gold_ore, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.mithril_ore, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.adamantite_ore, count = 0, restockCycles = 50)
            stock += stock(MikeShopObjs.runite_ore, count = 0, restockCycles = 50)

            // --- Tools ---
            stock += stock(MikeShopObjs.bronze_pickaxe, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.adamant_pickaxe, count = 3, restockCycles = 200)
            stock += stock(MikeShopObjs.rune_pickaxe, count = 1, restockCycles = 400)
            stock += stock(MikeShopObjs.hammer, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.tinderbox, count = 10, restockCycles = 100)

            // --- Wapens ---
            stock += stock(MikeShopObjs.bronze_sword, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.iron_sword, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.bronze_scimitar, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.iron_scimitar, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.steel_scimitar, count = 5, restockCycles = 200)
            stock += stock(MikeShopObjs.mithril_scimitar, count = 5, restockCycles = 200)
            stock += stock(MikeShopObjs.adamant_scimitar, count = 3, restockCycles = 300)
            stock += stock(MikeShopObjs.rune_scimitar, count = 1, restockCycles = 500)

            // --- Armour ---
            stock += stock(MikeShopObjs.leather_armour, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.wooden_shield, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.bronze_platebody, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.iron_platebody, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.steel_platebody, count = 5, restockCycles = 200)
            stock += stock(MikeShopObjs.rune_platebody, count = 1, restockCycles = 500)
            stock += stock(MikeShopObjs.iron_full_helm, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.rune_full_helm, count = 1, restockCycles = 500)
            stock += stock(MikeShopObjs.bronze_kiteshield, count = 10, restockCycles = 100)
            stock += stock(MikeShopObjs.rune_kiteshield, count = 1, restockCycles = 500)

            // --- Food ---
            stock += stock(MikeShopObjs.bread, count = 20, restockCycles = 50)
            stock += stock(MikeShopObjs.cooked_meat, count = 20, restockCycles = 50)
            stock += stock(MikeShopObjs.cooked_chicken, count = 20, restockCycles = 50)
            stock += stock(MikeShopObjs.trout, count = 20, restockCycles = 50)
            stock += stock(MikeShopObjs.salmon, count = 20, restockCycles = 50)
            stock += stock(MikeShopObjs.lobster, count = 10, restockCycles = 100)
        }
    }
}
