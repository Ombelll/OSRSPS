package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.MapClock
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FarmObjs : ObjReferences() {
    val potato_seed = find("potato_seed")
    val potato = find("potato")
}

internal object FarmLocs : LocReferences() {
    val patch = find("farming_gardeningpatch_untreated_a")
    val dirt = find("farming_dirtpatch")
}

/**
 * FARMING (met echte groeitimers).
 *
 * - Gebruik aardappelzaad op een patch -> geplant (kleine Farming-XP).
 * - Het gewas groeit `growTime` ticks. Klik de patch daarna aan -> oogst 3 aardappels + XP.
 * - Te vroeg oogsten meldt dat het nog groeit. (In-memory per patch-coördinaat via MapClock.)
 */
class Farming @Inject constructor(private val mapClock: MapClock) : PluginScript() {
    private val growTime = 20 // ticks (~12s) voor het testen; echte OSRS-tijden = vervolgwerk
    private val readyAt = HashMap<CoordGrid, Int>()

    override fun ScriptContext.startup() {
        for (patch in listOf(FarmLocs.patch, FarmLocs.dirt)) {
            onOpLocU(patch, FarmObjs.potato_seed) { plant(it.loc.coords) }
            onOpLoc1(patch) { harvest(it.loc.coords) }
        }
    }

    private fun ProtectedAccess.plant(coords: CoordGrid) {
        if (readyAt.containsKey(coords)) {
            mes("Something is already growing in this patch - harvest it first.")
            return
        }
        invDel(inv, FarmObjs.potato_seed, 1)
        readyAt[coords] = mapClock + growTime
        statAdvance(stats.farming, PlayerStatMap.toFineXP(2.0).toDouble())
        mes("You plant the potato seed. Come back shortly to harvest it.")
    }

    private fun ProtectedAccess.harvest(coords: CoordGrid) {
        val ready = readyAt[coords]
        if (ready == null) {
            mes("There is nothing planted here to harvest.")
            return
        }
        if (mapClock < ready) {
            mes("The crop is still growing. Be patient!")
            return
        }
        readyAt.remove(coords)
        invAdd(inv, FarmObjs.potato, 3)
        statAdvance(stats.farming, PlayerStatMap.toFineXP(8.0).toDouble())
        mes("You harvest 3 ripe potatoes.")
    }
}
