package org.rsmod.content.skills.smithing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SmithLocs : LocReferences() {
    val furnace = find("furnace")
    val anvil = find("anvil")
}

internal object SmithObjs : ObjReferences() {
    // Ores + coal
    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val iron_ore = find("iron_ore")
    val silver_ore = find("silver_ore")
    val gold_ore = find("gold_ore")
    val mithril_ore = find("mithril_ore")
    val adamantite_ore = find("adamantite_ore")
    val runite_ore = find("runite_ore")
    val coal = find("coal")

    // Bars
    val bronze_bar = find("bronze_bar")
    val iron_bar = find("iron_bar")
    val silver_bar = find("silver_bar")
    val steel_bar = find("steel_bar")
    val gold_bar = find("gold_bar")
    val mithril_bar = find("mithril_bar")
    val adamantite_bar = find("adamantite_bar")
    val runite_bar = find("runite_bar")

    val hammer = find("hammer")

    // Smeedbare items per tier: scimitar / full helm / platelegs / platebody
    val bronze_scimitar = find("bronze_scimitar")
    val bronze_full_helm = find("bronze_full_helm")
    val bronze_platelegs = find("bronze_platelegs")
    val bronze_platebody = find("bronze_platebody")
    val iron_scimitar = find("iron_scimitar")
    val iron_full_helm = find("iron_full_helm")
    val iron_platelegs = find("iron_platelegs")
    val iron_platebody = find("iron_platebody")
    val steel_scimitar = find("steel_scimitar")
    val steel_full_helm = find("steel_full_helm")
    val steel_platelegs = find("steel_platelegs")
    val steel_platebody = find("steel_platebody")
    val mithril_scimitar = find("mithril_scimitar")
    val mithril_full_helm = find("mithril_full_helm")
    val mithril_platelegs = find("mithril_platelegs")
    val mithril_platebody = find("mithril_platebody")
    val adamant_scimitar = find("adamant_scimitar")
    val adamant_full_helm = find("adamant_full_helm")
    val adamant_platelegs = find("adamant_platelegs")
    val adamant_platebody = find("adamant_platebody")
    val rune_scimitar = find("rune_scimitar")
    val rune_full_helm = find("rune_full_helm")
    val rune_platelegs = find("rune_platelegs")
    val rune_platebody = find("rune_platebody")
}

/** Eén metaal-tier: de staaf, smeedbare items en XP per staaf. */
private class SmithTier(
    val bar: ObjType,
    val scimitar: ObjType,
    val fullHelm: ObjType,
    val platelegs: ObjType,
    val platebody: ObjType,
    val xpPerBar: Double,
)

/**
 * SMITHING-skill (smelt + smeed tot een volledige wapen-/armour-keten).
 *
 * - OVEN ('furnace'): smelt het beste erts dat je hebt tot een staaf (+XP).
 * - AAMBEELD ('anvil', met hamer): smeedt het grootste item dat je baren toelaten van je
 *   beste metaal -> platebody (5 baren) / platelegs (3) / full helm (2) / scimitar (1).
 *   Klik herhaaldelijk om een hele harnas-set te smeden.
 */
class Smithing @Inject constructor() : PluginScript() {
    private val smithTiers: List<SmithTier> by lazy {
        listOf(
            SmithTier(
                SmithObjs.runite_bar,
                SmithObjs.rune_scimitar,
                SmithObjs.rune_full_helm,
                SmithObjs.rune_platelegs,
                SmithObjs.rune_platebody,
                75.0,
            ),
            SmithTier(
                SmithObjs.adamantite_bar,
                SmithObjs.adamant_scimitar,
                SmithObjs.adamant_full_helm,
                SmithObjs.adamant_platelegs,
                SmithObjs.adamant_platebody,
                62.5,
            ),
            SmithTier(
                SmithObjs.mithril_bar,
                SmithObjs.mithril_scimitar,
                SmithObjs.mithril_full_helm,
                SmithObjs.mithril_platelegs,
                SmithObjs.mithril_platebody,
                50.0,
            ),
            SmithTier(
                SmithObjs.steel_bar,
                SmithObjs.steel_scimitar,
                SmithObjs.steel_full_helm,
                SmithObjs.steel_platelegs,
                SmithObjs.steel_platebody,
                37.5,
            ),
            SmithTier(
                SmithObjs.iron_bar,
                SmithObjs.iron_scimitar,
                SmithObjs.iron_full_helm,
                SmithObjs.iron_platelegs,
                SmithObjs.iron_platebody,
                25.0,
            ),
            SmithTier(
                SmithObjs.bronze_bar,
                SmithObjs.bronze_scimitar,
                SmithObjs.bronze_full_helm,
                SmithObjs.bronze_platelegs,
                SmithObjs.bronze_platebody,
                12.5,
            ),
        )
    }

    override fun ScriptContext.startup() {
        onOpLoc1(SmithLocs.furnace) { smelt() }
        onOpLoc1(SmithLocs.anvil) { smith() }
    }

    private fun ProtectedAccess.smelt() {
        val coal = invTotal(inv, SmithObjs.coal)
        when {
            invTotal(inv, SmithObjs.runite_ore) >= 1 && coal >= 8 ->
                smeltWithCoal(SmithObjs.runite_ore, 8, SmithObjs.runite_bar, 50.0)
            invTotal(inv, SmithObjs.adamantite_ore) >= 1 && coal >= 6 ->
                smeltWithCoal(SmithObjs.adamantite_ore, 6, SmithObjs.adamantite_bar, 37.5)
            invTotal(inv, SmithObjs.mithril_ore) >= 1 && coal >= 4 ->
                smeltWithCoal(SmithObjs.mithril_ore, 4, SmithObjs.mithril_bar, 30.0)
            invTotal(inv, SmithObjs.iron_ore) >= 1 && coal >= 2 ->
                smeltWithCoal(SmithObjs.iron_ore, 2, SmithObjs.steel_bar, 17.5)
            invTotal(inv, SmithObjs.gold_ore) >= 1 ->
                smeltSingle(SmithObjs.gold_ore, SmithObjs.gold_bar, 22.5)
            invTotal(inv, SmithObjs.silver_ore) >= 1 ->
                smeltSingle(SmithObjs.silver_ore, SmithObjs.silver_bar, 13.7)
            invTotal(inv, SmithObjs.iron_ore) >= 1 ->
                smeltSingle(SmithObjs.iron_ore, SmithObjs.iron_bar, 12.5)
            invTotal(inv, SmithObjs.copper_ore) >= 1 && invTotal(inv, SmithObjs.tin_ore) >= 1 -> {
                invDel(inv, SmithObjs.copper_ore, 1, SmithObjs.tin_ore, 1)
                gainBar(SmithObjs.bronze_bar, 6.2)
            }
            else -> mes("You have no ore that you can smelt here.")
        }
    }

    private fun ProtectedAccess.smeltWithCoal(ore: ObjType, coal: Int, bar: ObjType, xp: Double) {
        invDel(inv, ore, 1, SmithObjs.coal, coal)
        gainBar(bar, xp)
    }

    private fun ProtectedAccess.smeltSingle(ore: ObjType, bar: ObjType, xp: Double) {
        invDel(inv, ore, 1)
        gainBar(bar, xp)
    }

    private fun ProtectedAccess.gainBar(bar: ObjType, xp: Double) {
        invAdd(inv, bar)
        statAdvance(stats.smithing, PlayerStatMap.toFineXP(xp).toDouble())
        mes("You smelt the ore into a bar.")
    }

    private fun ProtectedAccess.smith() {
        if (invTotal(inv, SmithObjs.hammer) < 1) {
            mes("You need a hammer to work the metal.")
            return
        }
        for (tier in smithTiers) {
            val bars = invTotal(inv, tier.bar)
            when {
                bars >= 5 -> return forge(tier.bar, 5, tier.platebody, tier.xpPerBar * 5, "platebody")
                bars >= 3 -> return forge(tier.bar, 3, tier.platelegs, tier.xpPerBar * 3, "platelegs")
                bars >= 2 -> return forge(tier.bar, 2, tier.fullHelm, tier.xpPerBar * 2, "full helm")
                bars >= 1 -> return forge(tier.bar, 1, tier.scimitar, tier.xpPerBar, "scimitar")
            }
        }
        mes("You have no metal bars to smith here.")
    }

    private fun ProtectedAccess.forge(
        bar: ObjType,
        count: Int,
        item: ObjType,
        xp: Double,
        name: String,
    ) {
        invDel(inv, bar, count)
        invAdd(inv, item)
        statAdvance(stats.smithing, PlayerStatMap.toFineXP(xp).toDouble())
        mes("You hammer the bars into a $name.")
    }
}
