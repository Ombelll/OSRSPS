package org.rsmod.content.custom.mikeshop

import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

/** Verwijzing naar de voorraadwinkel. */
object SupplyShopInvs : InvReferences() {
    val supply_shop = find("supply_shop")
}

/** Alles wat de voorraadwinkel verkoopt: ondersteunt Magic, Runecrafting, Herblore en Fishing. */
object SupplyShopObjs : ObjReferences() {
    // Runes (Magic + Runecrafting):
    val airrune = find("airrune")
    val waterrune = find("waterrune")
    val earthrune = find("earthrune")
    val firerune = find("firerune")
    val mindrune = find("mindrune")
    val bodyrune = find("bodyrune")
    val chaosrune = find("chaosrune")
    val deathrune = find("deathrune")
    val cosmicrune = find("cosmicrune")
    val naturerune = find("naturerune")
    val lawrune = find("lawrune")
    val blankrune = find("blankrune") // rune essence

    // Herblore (vials, secondaries, te reinigen kruiden):
    val vial_water = find("vial_water")
    val eye_of_newt = find("eye_of_newt")
    val limpwurt_root = find("limpwurt_root")
    val red_spiders_eggs = find("red_spiders_eggs")
    val unidentified_guam = find("unidentified_guam")
    val unidentified_tarromin = find("unidentified_tarromin")
    val unidentified_harralander = find("unidentified_harralander")

    // Fishing-tools + aas:
    val net = find("net")
    val fishing_rod = find("fishing_rod")
    val fly_fishing_rod = find("fly_fishing_rod")
    val harpoon = find("harpoon")
    val lobster_pot = find("lobster_pot")
    val fishing_bait = find("fishing_bait")
    val feather = find("feather")

    // Overige skill-tools:
    val knife = find("knife")
    val needle = find("needle")
    val tinderbox = find("tinderbox")
    val chisel = find("chisel")
    val ring_mould = find("ring_mould")
    val gold_bar = find("gold_bar")

    // Enchant-doelen (Magic):
    val sapphire_ring = find("sapphire_ring")
    val strung_sapphire_amulet = find("strung_sapphire_amulet")
}

/** Bouwt de voorraadwinkel 'supply_shop'. */
internal object SupplyShopInvBuilder : InvBuilder() {
    init {
        build("supply_shop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true

            // --- Runes ---
            stock += stock(SupplyShopObjs.airrune, count = 1000, restockCycles = 25)
            stock += stock(SupplyShopObjs.waterrune, count = 1000, restockCycles = 25)
            stock += stock(SupplyShopObjs.earthrune, count = 1000, restockCycles = 25)
            stock += stock(SupplyShopObjs.firerune, count = 1000, restockCycles = 25)
            stock += stock(SupplyShopObjs.mindrune, count = 1000, restockCycles = 25)
            stock += stock(SupplyShopObjs.bodyrune, count = 1000, restockCycles = 25)
            stock += stock(SupplyShopObjs.chaosrune, count = 500, restockCycles = 50)
            stock += stock(SupplyShopObjs.deathrune, count = 250, restockCycles = 100)
            stock += stock(SupplyShopObjs.cosmicrune, count = 500, restockCycles = 50)
            stock += stock(SupplyShopObjs.naturerune, count = 500, restockCycles = 50)
            stock += stock(SupplyShopObjs.lawrune, count = 250, restockCycles = 100)
            stock += stock(SupplyShopObjs.blankrune, count = 1000, restockCycles = 25)

            // --- Herblore ---
            stock += stock(SupplyShopObjs.vial_water, count = 500, restockCycles = 25)
            stock += stock(SupplyShopObjs.eye_of_newt, count = 200, restockCycles = 50)
            stock += stock(SupplyShopObjs.limpwurt_root, count = 100, restockCycles = 100)
            stock += stock(SupplyShopObjs.red_spiders_eggs, count = 100, restockCycles = 100)
            stock += stock(SupplyShopObjs.unidentified_guam, count = 100, restockCycles = 50)
            stock += stock(SupplyShopObjs.unidentified_tarromin, count = 100, restockCycles = 50)
            stock += stock(SupplyShopObjs.unidentified_harralander, count = 100, restockCycles = 50)

            // --- Fishing ---
            stock += stock(SupplyShopObjs.net, count = 25, restockCycles = 50)
            stock += stock(SupplyShopObjs.fishing_rod, count = 25, restockCycles = 50)
            stock += stock(SupplyShopObjs.fly_fishing_rod, count = 25, restockCycles = 50)
            stock += stock(SupplyShopObjs.harpoon, count = 10, restockCycles = 100)
            stock += stock(SupplyShopObjs.lobster_pot, count = 10, restockCycles = 100)
            stock += stock(SupplyShopObjs.fishing_bait, count = 1000, restockCycles = 25)
            stock += stock(SupplyShopObjs.feather, count = 1000, restockCycles = 25)

            // --- Tools ---
            stock += stock(SupplyShopObjs.knife, count = 25, restockCycles = 50)
            stock += stock(SupplyShopObjs.needle, count = 25, restockCycles = 50)
            stock += stock(SupplyShopObjs.tinderbox, count = 25, restockCycles = 50)
            stock += stock(SupplyShopObjs.chisel, count = 25, restockCycles = 50)
            stock += stock(SupplyShopObjs.ring_mould, count = 10, restockCycles = 100)
            stock += stock(SupplyShopObjs.gold_bar, count = 500, restockCycles = 25)

            // --- Enchant-doelen ---
            stock += stock(SupplyShopObjs.sapphire_ring, count = 50, restockCycles = 50)
            stock += stock(SupplyShopObjs.strung_sapphire_amulet, count = 50, restockCycles = 50)
        }
    }
}
