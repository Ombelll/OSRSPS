package org.rsmod.content.skills.crafting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CraftObjs : ObjReferences() {
    // Leerwerk:
    val needle = find("needle")
    val cow_hide = find("cow_hide")
    val leather = find("leather")
    val leather_gloves = find("leather_gloves")

    // Gem-cutting + juwelen:
    val chisel = find("chisel")
    val gold_bar = find("gold_bar")
    val ring_mould = find("ring_mould")
    val uncut_sapphire = find("uncut_sapphire")
    val uncut_emerald = find("uncut_emerald")
    val uncut_ruby = find("uncut_ruby")
    val uncut_diamond = find("uncut_diamond")
    val sapphire = find("sapphire")
    val emerald = find("emerald")
    val ruby = find("ruby")
    val diamond = find("diamond")
    val sapphire_ring = find("sapphire_ring")
    val emerald_ring = find("emerald_ring")
    val ruby_ring = find("ruby_ring")
    val diamond_ring = find("diamond_ring")
}

/** Eén edelsteen: ongeslepen, geslepen, ring + XP voor slijpen/ring. */
private class Gem(
    val uncut: ObjType,
    val cut: ObjType,
    val ring: ObjType,
    val cutXp: Double,
    val ringXp: Double,
    val name: String,
)

/**
 * CRAFTING.
 *
 * - Leer: naald op cowhide -> leer -> handschoenen.
 * - Gem-cutting: beitel op een ongeslepen edelsteen (uit Mining) -> geslepen steen + XP.
 * - Juwelen: goudstaaf op een geslepen steen (met ring-mould) -> ring + XP.
 *   (Maakt o.a. de sapphire ring die je in Magic kunt enchanten.)
 */
class Crafting @Inject constructor() : PluginScript() {
    private val gems =
        listOf(
            Gem(CraftObjs.uncut_sapphire, CraftObjs.sapphire, CraftObjs.sapphire_ring, 50.0, 40.0, "sapphire"),
            Gem(CraftObjs.uncut_emerald, CraftObjs.emerald, CraftObjs.emerald_ring, 67.5, 55.0, "emerald"),
            Gem(CraftObjs.uncut_ruby, CraftObjs.ruby, CraftObjs.ruby_ring, 85.0, 70.0, "ruby"),
            Gem(CraftObjs.uncut_diamond, CraftObjs.diamond, CraftObjs.diamond_ring, 107.5, 85.0, "diamond"),
        )

    override fun ScriptContext.startup() {
        onOpHeldU(CraftObjs.needle, CraftObjs.cow_hide) { tan() }
        onOpHeldU(CraftObjs.needle, CraftObjs.leather) { stitch(CraftObjs.leather_gloves, 13.8) }

        for (gem in gems) {
            onOpHeldU(CraftObjs.chisel, gem.uncut) { cut(gem) }
            onOpHeldU(CraftObjs.gold_bar, gem.cut) { makeRing(gem) }
        }
    }

    private fun ProtectedAccess.tan() {
        invDel(inv, CraftObjs.cow_hide, 1)
        invAdd(inv, CraftObjs.leather)
        statAdvance(stats.crafting, PlayerStatMap.toFineXP(5.0).toDouble())
        mes("You work the cowhide into a soft piece of leather.")
    }

    private fun ProtectedAccess.stitch(item: ObjType, xp: Double) {
        invDel(inv, CraftObjs.leather, 1)
        invAdd(inv, item)
        statAdvance(stats.crafting, PlayerStatMap.toFineXP(xp).toDouble())
        mes("You stitch the leather into a piece of armour.")
    }

    private fun ProtectedAccess.cut(gem: Gem) {
        invDel(inv, gem.uncut, 1)
        invAdd(inv, gem.cut)
        statAdvance(stats.crafting, PlayerStatMap.toFineXP(gem.cutXp).toDouble())
        mes("You cut the ${gem.name}.")
    }

    private fun ProtectedAccess.makeRing(gem: Gem) {
        if (invTotal(inv, CraftObjs.ring_mould) < 1) {
            mes("You need a ring mould to make a ring.")
            return
        }
        invDel(inv, CraftObjs.gold_bar, 1, gem.cut, 1)
        invAdd(inv, gem.ring)
        statAdvance(stats.crafting, PlayerStatMap.toFineXP(gem.ringXp).toDouble())
        mes("You make a ${gem.name} ring.")
    }
}
