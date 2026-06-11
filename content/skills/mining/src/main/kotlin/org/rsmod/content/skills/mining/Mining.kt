package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.MapClock
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** De rotsen waarop je kunt mijnen (bestaande cache-locs). */
internal object MiningRocks : LocReferences() {
    val copperrock1 = find("copperrock1")
    val copperrock2 = find("copperrock2")
    val tinrock1 = find("tinrock1")
    val tinrock2 = find("tinrock2")
    val ironrock1 = find("ironrock1")
    val ironrock2 = find("ironrock2")
    val silverrock1 = find("silverrock1")
    val silverrock2 = find("silverrock2")
    val coalrock1 = find("coalrock1")
    val coalrock2 = find("coalrock2")
    val goldrock1 = find("goldrock1")
    val goldrock2 = find("goldrock2")
    val mithrilrock1 = find("mithrilrock1")
    val mithrilrock2 = find("mithrilrock2")
    val adamantiterock1 = find("adamantiterock1")
    val adamantiterock2 = find("adamantiterock2")
    val runiterock1 = find("runiterock1")
    val runiterock2 = find("runiterock2")
}

/** Het erts dat je krijgt (bestaande cache-items). */
internal object MiningObjs : ObjReferences() {
    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val iron_ore = find("iron_ore")
    val silver_ore = find("silver_ore")
    val coal = find("coal")
    val gold_ore = find("gold_ore")
    val mithril_ore = find("mithril_ore")
    val adamantite_ore = find("adamantite_ore")
    val runite_ore = find("runite_ore")

    // Edelstenen die je zelden vindt tijdens het mijnen:
    val uncut_sapphire = find("uncut_sapphire")
    val uncut_emerald = find("uncut_emerald")
    val uncut_ruby = find("uncut_ruby")
    val uncut_diamond = find("uncut_diamond")
}

/** Definitie van een erts-soort: welk item, vereist level, XP en respawn-tijd (ticks). */
private class Ore(val item: ObjType, val level: Int, val xp: Double, val respawn: Int)

/**
 * MINING-skill (met depletion + respawn + edelsteen-vondsten).
 *
 * Klik een rots aan -> erts + Mining-XP. Daarna is die rots tijdelijk **uitgeput**
 * (respawn-tijd schaalt met de erts-tier) en moet je een andere rots zoeken.
 * Tijdens het mijnen is er een kleine kans op een ongeslepen edelsteen.
 */
class Mining
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val mapClock: MapClock,
    private val random: GameRandom,
) : PluginScript() {
    private val copper = Ore(MiningObjs.copper_ore, level = 1, xp = 17.5, respawn = 3)
    private val tin = Ore(MiningObjs.tin_ore, level = 1, xp = 17.5, respawn = 3)
    private val iron = Ore(MiningObjs.iron_ore, level = 15, xp = 35.0, respawn = 5)
    private val silver = Ore(MiningObjs.silver_ore, level = 20, xp = 40.0, respawn = 6)
    private val coal = Ore(MiningObjs.coal, level = 30, xp = 50.0, respawn = 10)
    private val gold = Ore(MiningObjs.gold_ore, level = 40, xp = 65.0, respawn = 12)
    private val mithril = Ore(MiningObjs.mithril_ore, level = 55, xp = 80.0, respawn = 30)
    private val adamantite = Ore(MiningObjs.adamantite_ore, level = 70, xp = 95.0, respawn = 60)
    private val runite = Ore(MiningObjs.runite_ore, level = 85, xp = 125.0, respawn = 120)

    /** Per rots-coördinaat: de game-tick waarop de rots weer beschikbaar is. */
    private val depletedUntil = HashMap<CoordGrid, Int>()

    private val gems =
        listOf(
            MiningObjs.uncut_sapphire,
            MiningObjs.uncut_sapphire,
            MiningObjs.uncut_emerald,
            MiningObjs.uncut_ruby,
            MiningObjs.uncut_diamond,
        )

    override fun ScriptContext.startup() {
        rock(MiningRocks.copperrock1, copper)
        rock(MiningRocks.copperrock2, copper)
        rock(MiningRocks.tinrock1, tin)
        rock(MiningRocks.tinrock2, tin)
        rock(MiningRocks.ironrock1, iron)
        rock(MiningRocks.ironrock2, iron)
        rock(MiningRocks.silverrock1, silver)
        rock(MiningRocks.silverrock2, silver)
        rock(MiningRocks.coalrock1, coal)
        rock(MiningRocks.coalrock2, coal)
        rock(MiningRocks.goldrock1, gold)
        rock(MiningRocks.goldrock2, gold)
        rock(MiningRocks.mithrilrock1, mithril)
        rock(MiningRocks.mithrilrock2, mithril)
        rock(MiningRocks.adamantiterock1, adamantite)
        rock(MiningRocks.adamantiterock2, adamantite)
        rock(MiningRocks.runiterock1, runite)
        rock(MiningRocks.runiterock2, runite)
    }

    private fun ScriptContext.rock(loc: LocType, ore: Ore) {
        onOpLoc1(loc) { mine(ore, it.loc.coords) }
    }

    private fun ProtectedAccess.mine(ore: Ore, coords: CoordGrid) {
        if (mapClock < (depletedUntil[coords] ?: 0)) {
            mes("This rock has been depleted; wait for it to respawn.")
            return
        }
        if (player.miningLvl < ore.level) {
            mes("You need a Mining level of ${ore.level} to mine this rock.")
            return
        }
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more ore.")
            return
        }
        val type = objTypes[ore.item]
        invAdd(inv, ore.item)
        statAdvance(stats.mining, PlayerStatMap.toFineXP(ore.xp).toDouble())
        spam("You manage to mine some ${type.name?.lowercase() ?: "ore"}.")

        // Rots raakt uitgeput tot de respawn-tijd voorbij is.
        depletedUntil[coords] = mapClock + ore.respawn

        // Kleine kans (~1/128) op een ongeslepen edelsteen.
        if (random.of(maxExclusive = 128) == 0 && !inv.isFull()) {
            val gem = gems[random.of(maxExclusive = gems.size)]
            invAdd(inv, gem)
            mes("You find a gem while mining!")
        }
    }
}
