package org.rsmod.content.skills.magic.utility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MagObjs : ObjReferences() {
    // Runes:
    val naturerune = find("naturerune")
    val firerune = find("firerune")
    val cosmicrune = find("cosmicrune")
    val waterrune = find("waterrune")
    // Lvl-1 Enchant doelen + resultaten:
    val sapphire_ring = find("sapphire_ring")
    val ring_of_recoil = find("ring_of_recoil")
    val strung_sapphire_amulet = find("strung_sapphire_amulet")
    val amulet_of_magic = find("amulet_of_magic")
    // High Alchemy doelen (uit de winkel-gear):
    val iron_scimitar = find("iron_scimitar")
    val steel_scimitar = find("steel_scimitar")
    val mithril_scimitar = find("mithril_scimitar")
}

/**
 * MAGIC (utility-spreuken).
 *
 * Gevechtsmagie (strikes/bolts/blasts/waves/surges) zit al in de submodule `spell-attacks`.
 * Dit voegt de utility-kant toe, gecast door runes op een item te gebruiken:
 *
 * - Lvl-1 Enchant (lvl 7): water- + cosmic-rune op saffier-juwelen
 *     saffieren ring   -> Ring of recoil
 *     saffieren amulet -> Amulet of magic
 * - High Alchemy (lvl 55): nature- + 5 fire-runes op een item -> munten
 */
class MagicUtility @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        // ---- Lvl-1 Enchant ----
        onOpHeldU(MagObjs.cosmicrune, MagObjs.sapphire_ring) {
            enchant(MagObjs.sapphire_ring, MagObjs.ring_of_recoil, "ring of recoil")
        }
        onOpHeldU(MagObjs.cosmicrune, MagObjs.strung_sapphire_amulet) {
            enchant(MagObjs.strung_sapphire_amulet, MagObjs.amulet_of_magic, "amulet of magic")
        }

        // ---- High Alchemy ----
        onOpHeldU(MagObjs.naturerune, MagObjs.iron_scimitar) { highAlch(MagObjs.iron_scimitar, 78) }
        onOpHeldU(MagObjs.naturerune, MagObjs.steel_scimitar) { highAlch(MagObjs.steel_scimitar, 195) }
        onOpHeldU(MagObjs.naturerune, MagObjs.mithril_scimitar) {
            highAlch(MagObjs.mithril_scimitar, 507)
        }
    }

    private fun ProtectedAccess.enchant(from: ObjType, to: ObjType, name: String) {
        if (player.magicLvl < 7) {
            mes("You need a Magic level of 7 to cast Lvl-1 Enchant.")
            return
        }
        if (invTotal(inv, MagObjs.waterrune) < 1 || invTotal(inv, MagObjs.cosmicrune) < 1) {
            mes("You need 1 water-rune and 1 cosmic-rune to cast this enchantment.")
            return
        }
        invDel(inv, MagObjs.waterrune, 1)
        invDel(inv, MagObjs.cosmicrune, 1)
        invDel(inv, from, 1)
        invAdd(inv, to)
        statAdvance(stats.magic, PlayerStatMap.toFineXP(17.5).toDouble())
        mes("You enchant the jewellery into a $name.")
    }

    private fun ProtectedAccess.highAlch(item: ObjType, coins: Int) {
        if (player.magicLvl < 55) {
            mes("You need a Magic level of 55 to cast High Level Alchemy.")
            return
        }
        if (invTotal(inv, MagObjs.naturerune) < 1 || invTotal(inv, MagObjs.firerune) < 5) {
            mes("You need 1 nature-rune and 5 fire-runes to cast High Level Alchemy.")
            return
        }
        invDel(inv, MagObjs.naturerune, 1)
        invDel(inv, MagObjs.firerune, 5)
        invDel(inv, item, 1)
        invAdd(inv, objs.coins, coins)
        statAdvance(stats.magic, PlayerStatMap.toFineXP(65.0).toDouble())
        mes("You convert the item into $coins coins.")
    }
}
