package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Vis-plekken (bestaande cache-NPC's). Spawn er een met bv. `::npc fishing_spot_aerial`. */
internal object FishSpots : NpcReferences() {
    val aerial = find("fishing_spot_aerial")
    val brutA = find("0_39_54_brut_fishing_spot")
    val brutB = find("0_19_55_brut_fishing_spot")
}

internal object FishObjs : ObjReferences() {
    // Tools + aas:
    val net = find("net")
    val fishing_rod = find("fishing_rod")
    val fly_fishing_rod = find("fly_fishing_rod")
    val harpoon = find("harpoon")
    val lobster_pot = find("lobster_pot")
    val fishing_bait = find("fishing_bait")
    val feather = find("feather")
    // Rauwe vis:
    val raw_shrimp = find("raw_shrimp")
    val raw_anchovies = find("raw_anchovies")
    val raw_sardine = find("raw_sardine")
    val raw_herring = find("raw_herring")
    val raw_trout = find("raw_trout")
    val raw_salmon = find("raw_salmon")
    val raw_tuna = find("raw_tuna")
    val raw_swordfish = find("raw_swordfish")
    val raw_lobster = find("raw_lobster")
    val raw_shark = find("raw_shark")
}

/** Eén vangst-soort: welk item, vereist Fishing-level, XP en weergavenaam. */
private class Catch(val fish: ObjType, val level: Int, val xp: Double, val name: String)

/** Een vismethode: het benodigde gereedschap, optioneel verbruiksaas, en de vangsttabel. */
private class Method(val tool: ObjType, val bait: ObjType?, val table: List<Catch>)

/**
 * FISHING.
 *
 * Klik een vis-plek aan. Op basis van het gereedschap (en aas) in je inventory en je
 * Fishing-level vang je de best mogelijke vis + Fishing-XP:
 *  - klein visnet                -> garnalen (1) / ansjovis (15)
 *  - vlieghengel + veer          -> forel (20) / zalm (30)
 *  - hengel + aas                -> sardine (5) / haring (10)
 *  - harpoen                     -> tonijn (35) / zwaardvis (50) / haai (76)
 *  - kreeftenkooi                -> kreeft (40)
 */
class Fishing @Inject constructor() : PluginScript() {
    private val methods: List<Method> by lazy {
        listOf(
            Method(
                FishObjs.net,
                bait = null,
                table =
                    listOf(
                        Catch(FishObjs.raw_shrimp, 1, 10.0, "some shrimps"),
                        Catch(FishObjs.raw_anchovies, 15, 40.0, "some anchovies"),
                    ),
            ),
            Method(
                FishObjs.fly_fishing_rod,
                bait = FishObjs.feather,
                table =
                    listOf(
                        Catch(FishObjs.raw_trout, 20, 50.0, "a trout"),
                        Catch(FishObjs.raw_salmon, 30, 70.0, "a salmon"),
                    ),
            ),
            Method(
                FishObjs.fishing_rod,
                bait = FishObjs.fishing_bait,
                table =
                    listOf(
                        Catch(FishObjs.raw_sardine, 5, 20.0, "a sardine"),
                        Catch(FishObjs.raw_herring, 10, 30.0, "a herring"),
                    ),
            ),
            Method(
                FishObjs.harpoon,
                bait = null,
                table =
                    listOf(
                        Catch(FishObjs.raw_tuna, 35, 80.0, "a tuna"),
                        Catch(FishObjs.raw_swordfish, 50, 100.0, "a swordfish"),
                        Catch(FishObjs.raw_shark, 76, 110.0, "a shark"),
                    ),
            ),
            Method(
                FishObjs.lobster_pot,
                bait = null,
                table = listOf(Catch(FishObjs.raw_lobster, 40, 90.0, "a lobster")),
            ),
        )
    }

    override fun ScriptContext.startup() {
        // Visplekken kunnen zowel direct ("op", ernaast staand) als op afstand ("ap",
        // vanaf de kant) aangeklikt worden -> beide routes naar fish().
        for (spot in listOf(FishSpots.aerial, FishSpots.brutA, FishSpots.brutB)) {
            onOpNpc1(spot) { fish() }
            onOpNpc2(spot) { fish() }
            onApNpc1(spot) { fish() }
            onApNpc2(spot) { fish() }
        }
    }

    private fun ProtectedAccess.has(obj: ObjType): Boolean = invTotal(inv, obj) > 0

    private fun ProtectedAccess.fish() {
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more fish.")
            return
        }
        val method = methods.firstOrNull { has(it.tool) && (it.bait == null || has(it.bait)) }
        if (method == null) {
            mes("You need a fishing tool (and the right bait) to fish here.")
            return
        }
        val catch = method.table.filter { player.fishingLvl >= it.level }.maxByOrNull { it.level }
        if (catch == null) {
            mes("Your Fishing level isn't high enough to catch anything here yet.")
            return
        }
        method.bait?.let { invDel(inv, it, 1) }
        invAdd(inv, catch.fish)
        statAdvance(stats.fishing, PlayerStatMap.toFineXP(catch.xp).toDouble())
        spam("You catch ${catch.name}.")
    }
}
